import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../../models/collection.dart';
import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../capture/add_collection_sheet.dart';
import '../capture/quick_capture_sheet.dart';
import '../favorites/favorites_screen.dart';
import 'content_detail_screen.dart';

// The "tash" lettering in the wordmark is white, which disappears against
// a light app bar, so light theme uses a recolored variant (dark lettering,
// same colorful gradient "S") at the same size/weight instead of falling
// back to plain text.
Widget _brandTitle(BuildContext context) {
  final asset = Theme.of(context).brightness == Brightness.dark
      ? 'assets/icon/wordmark.png'
      : 'assets/icon/wordmark_light.png';
  return Image.asset(asset, height: 28, fit: BoxFit.contain);
}

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  // Notes and personal articles share one editor and one filter chip; they
  // were previously split into separate "Notes"/"Writing" sections with
  // identical UI, which was confusing.
  static const _filters = <(String, List<ContentType>?)>[
    ('All', null),
    ('Reels', [ContentType.instagramReel]),
    ('YouTube', [ContentType.youtubeVideo]),
    ('Facebook', [ContentType.facebookVideo]),
    ('Articles', [ContentType.webArticle]),
    ('Notes', [ContentType.personalNote, ContentType.personalArticle]),
  ];

  // Long-pressing a card enters selection mode; tapping other cards then
  // toggles them in/out instead of opening the quick-info sheet, so several
  // items can be deleted in one action instead of one-by-one.
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
    ref.invalidate(contentListProvider);
    ref.invalidate(searchResultsProvider);
  }

  // Lets the user manually group selected items (e.g. movie reviews, recipe
  // videos, research clips) into a folder of their own, separate from the
  // automatic platform/type filters above. Moving doesn't remove the items
  // from "All" or their type filter — it only adds them to the folder so
  // they're also reachable from that folder's own pill.
  Future<void> _moveSelectedToFolder() async {
    final collections = await ref.read(collectionsListProvider.future);
    if (!mounted) return;
    final choice = await showModalBottomSheet<String>(
      context: context,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 20, 20, 8),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Move to folder', style: Theme.of(ctx).textTheme.titleLarge),
                ),
              ),
              for (final c in collections)
                ListTile(
                  leading: const Icon(Icons.folder_rounded),
                  title: Text(c.name),
                  onTap: () => Navigator.pop(ctx, c.id),
                ),
              ListTile(
                leading: Icon(Icons.add_rounded, color: Theme.of(ctx).colorScheme.primary),
                title: Text('New folder', style: TextStyle(color: Theme.of(ctx).colorScheme.primary)),
                onTap: () => Navigator.pop(ctx, '__new__'),
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
    if (choice == null) return;

    String? targetId = choice;
    if (choice == '__new__') {
      targetId = await _createFolder(returnId: true);
      if (targetId == null) return;
    }

    final repo = ref.read(contentRepositoryProvider);
    for (final id in _selectedIds) {
      await repo.addToCollection(id, targetId);
    }
    setState(() => _selectedIds.clear());
    ref.invalidate(collectionsListProvider);
  }

  // Opens the same "create collection" sheet used elsewhere in the app.
  // When [returnId] is true (called from the move-to-folder flow), it waits
  // for the newly created folder's id instead of just closing.
  Future<String?> _createFolder({bool returnId = false}) async {
    if (!returnId) {
      await showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        backgroundColor: Theme.of(context).colorScheme.surface,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
        builder: (_) => const AddCollectionSheet(),
      );
      return null;
    }

    final before = await ref.read(collectionsListProvider.future);
    if (!mounted) return null;
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => const AddCollectionSheet(),
    );
    ref.invalidate(collectionsListProvider);
    final after = await ref.read(collectionsListProvider.future);
    final created = after.where((c) => !before.any((b) => b.id == c.id));
    return created.isEmpty ? null : created.first.id;
  }

  void _openFolder(Collection collection) {
    ref.read(contentFilterProvider.notifier).state = ContentFilter(collectionId: collection.id);
  }

  @override
  Widget build(BuildContext context) {
    final contentAsync = ref.watch(contentListProvider);
    final filter = ref.watch(contentFilterProvider);
    final collectionsAsync = ref.watch(collectionsListProvider);

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
                  icon: const Icon(Icons.folder_outlined),
                  tooltip: 'Move to folder',
                  onPressed: _moveSelectedToFolder,
                ),
                IconButton(
                  icon: const Icon(Icons.delete_outline_rounded),
                  onPressed: _deleteSelected,
                ),
              ],
            )
          : AppBar(
              title: _brandTitle(context),
              actions: [
                IconButton(
                  icon: const Icon(Icons.star_rounded),
                  tooltip: 'Favorites',
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const FavoritesScreen()),
                  ),
                ),
              ],
            ),
      body: Column(
        children: [
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              children: [
                ..._filters.map((entry) {
                  final types = entry.$2;
                  final selected = filter.collectionId == null &&
                      (types == null
                          ? (filter.types == null || filter.types!.isEmpty)
                          : (filter.types != null &&
                              filter.types!.length == types.length &&
                              types.every(filter.types!.contains)));
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: _FilterPill(
                      label: entry.$1,
                      selected: selected,
                      onTap: () {
                        ref.read(contentFilterProvider.notifier).state = ContentFilter(
                          types: types,
                          onlyFavorites: filter.onlyFavorites,
                        );
                      },
                    ),
                  );
                }),
                // Custom, user-created folders (e.g. "Movies", "Recipes",
                // "Research") for manually grouping items that don't fit
                // any single platform/type filter above.
                ...collectionsAsync.maybeWhen(
                  data: (collections) => collections.map((c) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: _FilterPill(
                        label: c.name,
                        selected: filter.collectionId == c.id,
                        onTap: () => _openFolder(c),
                      ),
                    );
                  }),
                  orElse: () => const <Widget>[],
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: _AddFolderPill(onTap: () => _createFolder()),
                ),
              ],
            ),
          ),
          Expanded(
            child: contentAsync.when(
              data: (items) {
                if (items.isEmpty) {
                  return const EmptyState(
                    icon: Icons.bookmark_add_outlined,
                    title: 'Nothing saved yet',
                    message: 'Tap the + button to save a link, write a note, or start a collection.',
                  );
                }
                return RefreshIndicator(
                  onRefresh: () async => ref.invalidate(contentListProvider),
                  child: MasonryGridView.count(
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
                          await ref
                              .read(contentRepositoryProvider)
                              .toggleFavorite(item.id, !item.favorite);
                          ref.invalidate(contentListProvider);
                        },
                      );
                    },
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('Something went wrong: $e')),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => QuickCaptureSheet.show(context),
        child: const Icon(Icons.add_rounded),
      ),
    );
  }
}

// Plain Material pill instead of ChoiceChip: ChoiceChip clips its label to
// an internally computed width that doesn't always grow with larger system
// font sizes, which was cutting off "Reels"/"YouTube"/"Facebook". A Text
// with no maxLines/overflow constraint always sizes to fit its content.
class _FilterPill extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterPill({required this.label, required this.selected, required this.onTap});

  // The whole filter row used to be black-on-black with only the selected
  // pill picking up the purple primary color. Giving every unselected pill
  // a solid orange fill (white text, same as the selected purple pill)
  // makes the row read as colorful chips rather than plain text.
  static const _orange = Color(0xFFF97316);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // The selected pill pops slightly larger with a real shadow under it
    // (a "pressed up" look); the rest sit flat and a touch smaller so the
    // active filter is unmistakable at a glance.
    return AnimatedScale(
      scale: selected ? 1.08 : 0.92,
      duration: const Duration(milliseconds: 150),
      curve: Curves.easeOut,
      child: Material(
        color: selected ? theme.colorScheme.primary : _orange,
        elevation: selected ? 5 : 0,
        shadowColor: theme.colorScheme.primary.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(20),
        child: InkWell(
          borderRadius: BorderRadius.circular(20),
          onTap: onTap,
          child: Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
              child: Text(
                label,
                textHeightBehavior: const TextHeightBehavior(
                  applyHeightToFirstAscent: false,
                  applyHeightToLastDescent: false,
                ),
                style: theme.textTheme.labelLarge?.copyWith(
                  color: selected ? theme.colorScheme.onPrimary : Colors.white,
                  fontWeight: FontWeight.w600,
                  height: 1.0,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// A small circular "+" pill at the end of the filter row for creating a new
// folder on the spot, matching the row's existing chip styling rather than
// looking like an unrelated button bolted on.
class _AddFolderPill extends StatelessWidget {
  final VoidCallback onTap;

  const _AddFolderPill({required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: theme.colorScheme.surface,
      shape: CircleBorder(
        side: BorderSide(color: theme.colorScheme.primary.withValues(alpha: 0.4)),
      ),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Icon(Icons.add_rounded, size: 22, color: theme.colorScheme.primary),
        ),
      ),
    );
  }
}
