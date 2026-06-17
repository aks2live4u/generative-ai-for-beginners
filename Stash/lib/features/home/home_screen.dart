import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../capture/quick_capture_sheet.dart';
import 'content_detail_screen.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

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

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contentAsync = ref.watch(contentListProvider);
    final filter = ref.watch(contentFilterProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Stash'),
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
                  child: ChoiceChip(
                    label: Text(entry.$1),
                    selected: selected,
                    onSelected: (_) {
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
                        onTap: () => Navigator.of(context).push(
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
