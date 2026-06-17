import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../home/content_detail_screen.dart';

class FavoritesScreen extends ConsumerStatefulWidget {
  const FavoritesScreen({super.key});

  @override
  ConsumerState<FavoritesScreen> createState() => _FavoritesScreenState();
}

class _FavoritesScreenState extends ConsumerState<FavoritesScreen> {
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
    ref.invalidate(favoritesListProvider);
    ref.invalidate(contentListProvider);
    ref.invalidate(searchResultsProvider);
  }

  @override
  Widget build(BuildContext context) {
    final itemsAsync = ref.watch(favoritesListProvider);
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
          : AppBar(title: const Text('Favorites')),
      body: itemsAsync.when(
        data: (items) {
          if (items.isEmpty) {
            return const EmptyState(
              icon: Icons.star_border_rounded,
              title: 'No favorites yet',
              message: 'Tap the star on any saved item to find it here.',
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
                onFavoriteToggle: () async {
                  await ref.read(contentRepositoryProvider).toggleFavorite(item.id, !item.favorite);
                  ref.invalidate(favoritesListProvider);
                  ref.invalidate(contentListProvider);
                },
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
