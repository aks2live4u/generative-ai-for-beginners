import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../capture/quick_capture_sheet.dart';
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

  @override
  Widget build(BuildContext context) {
    final contentAsync = ref.watch(contentListProvider);
    final filter = ref.watch(contentFilterProvider);

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
              title: _brandTitle(context),
            ),
      body: Column(
        children: [
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              children: _filters.map((entry) {
                final types = entry.$2;
                final selected = types == null
                    ? (filter.types == null || filter.types!.isEmpty)
                    : (filter.types != null &&
                        filter.types!.length == types.length &&
                        types.every(filter.types!.contains));
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
              }).toList(),
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
    return Material(
      color: selected ? theme.colorScheme.primary : _orange,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Text(
            label,
            style: theme.textTheme.labelLarge?.copyWith(
              color: selected ? theme.colorScheme.onPrimary : Colors.white,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}
