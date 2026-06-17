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

  static const _typeLabels = {
    null: 'All',
    ContentType.instagramReel: 'Reels',
    ContentType.youtubeVideo: 'YouTube',
    ContentType.facebookVideo: 'Facebook',
    ContentType.webArticle: 'Articles',
    ContentType.personalNote: 'Notes',
    ContentType.personalArticle: 'Writing',
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final contentAsync = ref.watch(contentListProvider);
    final filter = ref.watch(contentFilterProvider);
    final inboxAsync = ref.watch(inboxListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Stash'),
        actions: [
          inboxAsync.maybeWhen(
            data: (items) => items.isEmpty
                ? const SizedBox.shrink()
                : Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Badge(
                      label: Text('${items.length}'),
                      child: IconButton(
                        icon: const Icon(Icons.inbox_rounded),
                        tooltip: 'Inbox',
                        onPressed: () => _showInbox(context, items),
                      ),
                    ),
                  ),
            orElse: () => const SizedBox.shrink(),
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
              children: _typeLabels.entries.map((entry) {
                final selected = entry.key == null
                    ? (filter.types == null || filter.types!.isEmpty)
                    : (filter.types?.contains(entry.key) ?? false);
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: ChoiceChip(
                    label: Text(entry.value),
                    selected: selected,
                    onSelected: (_) {
                      ref.read(contentFilterProvider.notifier).state = ContentFilter(
                        types: entry.key == null ? null : [entry.key!],
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

  void _showInbox(BuildContext context, List<ContentItem> items) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        expand: false,
        builder: (ctx, scrollController) => Column(
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Inbox', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            ),
            Expanded(
              child: ListView.builder(
                controller: scrollController,
                itemCount: items.length,
                itemBuilder: (ctx, i) {
                  final item = items[i];
                  return ListTile(
                    title: Text(item.title, maxLines: 1, overflow: TextOverflow.ellipsis),
                    subtitle: Text(item.tags.join(', ')),
                    onTap: () {
                      Navigator.of(ctx).pop();
                      Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => ContentDetailScreen(item: item)),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
