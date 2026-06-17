import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../../models/collection.dart';
import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../home/content_detail_screen.dart';

final _collectionItemsProvider =
    FutureProvider.autoDispose.family<List<ContentItem>, String>((ref, collectionId) async {
  final repo = ref.watch(contentRepositoryProvider);
  return repo.all(collectionId: collectionId);
});

class CollectionDetailScreen extends ConsumerStatefulWidget {
  final Collection collection;

  const CollectionDetailScreen({super.key, required this.collection});

  @override
  ConsumerState<CollectionDetailScreen> createState() => _CollectionDetailScreenState();
}

class _CollectionDetailScreenState extends ConsumerState<CollectionDetailScreen> {
  final Set<String> _selectedIds = {};

  bool get _selectionMode => _selectedIds.isNotEmpty;

  void _toggleSelected(String id) {
    setState(() {
      if (_selectedIds.contains(id)) {
        _selectedIds.remove(id);
      } else {
        _selectedIds.add(id);
      }
    });
  }

  Future<void> _deleteSelected() async {
    final count = _selectedIds.length;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(count == 1 ? 'Delete item?' : 'Delete $count items?'),
        content: const Text('This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    final repo = ref.read(contentRepositoryProvider);
    for (final id in _selectedIds) {
      await repo.delete(id);
    }
    setState(() => _selectedIds.clear());
    ref.invalidate(_collectionItemsProvider(widget.collection.id));
    ref.invalidate(contentListProvider);
    ref.invalidate(searchResultsProvider);
  }

  Future<void> _rename(BuildContext context, WidgetRef ref) async {
    final controller = TextEditingController(text: widget.collection.name);
    final newName = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Rename collection'),
        content: TextField(controller: controller, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, controller.text.trim()), child: const Text('Save')),
        ],
      ),
    );
    if (newName == null || newName.isEmpty) return;
    await ref.read(collectionRepositoryProvider).rename(widget.collection.id, newName);
    ref.invalidate(collectionsListProvider);
    if (context.mounted) Navigator.of(context).pop();
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete collection?'),
        content: const Text('Items inside will remain in your vault but leave this collection.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    await ref.read(collectionRepositoryProvider).delete(widget.collection.id);
    ref.invalidate(collectionsListProvider);
    if (context.mounted) Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final collection = widget.collection;
    final itemsAsync = ref.watch(_collectionItemsProvider(collection.id));
    return Scaffold(
      appBar: _selectionMode
          ? AppBar(
              leading: IconButton(
                icon: const Icon(Icons.close_rounded),
                onPressed: () => setState(() => _selectedIds.clear()),
              ),
              title: Text('${_selectedIds.length} selected'),
              actions: [
                IconButton(
                  icon: const Icon(Icons.delete_outline_rounded),
                  onPressed: _deleteSelected,
                ),
              ],
            )
          : AppBar(
              title: Text(collection.name),
              actions: [
                IconButton(
                  icon: Icon(collection.pinned ? Icons.push_pin : Icons.push_pin_outlined),
                  onPressed: () async {
                    await ref.read(collectionRepositoryProvider).setPinned(collection.id, !collection.pinned);
                    ref.invalidate(collectionsListProvider);
                  },
                ),
                IconButton(
                  icon: Icon(collection.favorite ? Icons.star_rounded : Icons.star_border_rounded),
                  onPressed: () async {
                    await ref.read(collectionRepositoryProvider).setFavorite(collection.id, !collection.favorite);
                    ref.invalidate(collectionsListProvider);
                  },
                ),
                PopupMenuButton<String>(
                  onSelected: (value) {
                    if (value == 'rename') _rename(context, ref);
                    if (value == 'delete') _delete(context, ref);
                  },
                  itemBuilder: (ctx) => const [
                    PopupMenuItem(value: 'rename', child: Text('Rename')),
                    PopupMenuItem(value: 'delete', child: Text('Delete')),
                  ],
                ),
              ],
            ),
      body: itemsAsync.when(
        data: (items) {
          if (items.isEmpty) {
            return const EmptyState(
              icon: Icons.collections_bookmark_outlined,
              title: 'Empty collection',
              message: 'Save items and assign them to this collection to see them here.',
            );
          }
          return MasonryGridView.count(
            padding: const EdgeInsets.all(12),
            crossAxisCount: 2,
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            itemCount: items.length,
            itemBuilder: (context, index) {
              final item = items[index];
              return StashCard(
                item: item,
                selectionMode: _selectionMode,
                selected: _selectedIds.contains(item.id),
                onLongPress: () => _toggleSelected(item.id),
                onTap: _selectionMode
                    ? () => _toggleSelected(item.id)
                    : () => Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => ContentDetailScreen(item: item)),
                        ),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Something went wrong: $e')),
      ),
    );
  }
}
