import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../../state/providers.dart';
import '../home/content_detail_screen.dart';

enum SearchSort { newest, oldest, mostOpened }

final _sortProvider = StateProvider((ref) => SearchSort.newest);

class SearchScreen extends ConsumerWidget {
  const SearchScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final query = ref.watch(searchQueryProvider);
    final resultsAsync = ref.watch(searchResultsProvider);
    final sort = ref.watch(_sortProvider);

    return Scaffold(
      appBar: AppBar(
        title: TextField(
          autofocus: true,
          decoration: const InputDecoration(
            hintText: 'Search everything…',
            border: InputBorder.none,
          ),
          onChanged: (value) => ref.read(searchQueryProvider.notifier).state = value,
        ),
      ),
      body: Column(
        children: [
          if (query.trim().isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              child: Row(
                children: [
                  const Text('Sort: '),
                  const SizedBox(width: 8),
                  DropdownButton<SearchSort>(
                    value: sort,
                    underline: const SizedBox.shrink(),
                    items: const [
                      DropdownMenuItem(value: SearchSort.newest, child: Text('Newest')),
                      DropdownMenuItem(value: SearchSort.oldest, child: Text('Oldest')),
                      DropdownMenuItem(value: SearchSort.mostOpened, child: Text('Most opened')),
                    ],
                    onChanged: (v) => ref.read(_sortProvider.notifier).state = v ?? SearchSort.newest,
                  ),
                ],
              ),
            ),
          Expanded(
            child: query.trim().isEmpty
                ? const EmptyState(
                    icon: Icons.search_rounded,
                    title: 'Search your vault',
                    message: 'Find anything by title, tag, summary, collection, or note content.',
                  )
                : resultsAsync.when(
                    data: (results) {
                      if (results.isEmpty) {
                        return const EmptyState(
                          icon: Icons.search_off_rounded,
                          title: 'No matches',
                          message: 'Try a different keyword or check your spelling.',
                        );
                      }
                      final sorted = [...results];
                      switch (sort) {
                        case SearchSort.newest:
                          sorted.sort((a, b) => b.createdAt.compareTo(a.createdAt));
                          break;
                        case SearchSort.oldest:
                          sorted.sort((a, b) => a.createdAt.compareTo(b.createdAt));
                          break;
                        case SearchSort.mostOpened:
                          sorted.sort((a, b) => b.openCount.compareTo(a.openCount));
                          break;
                      }
                      return ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: sorted.length,
                        itemBuilder: (context, index) {
                          final item = sorted[index];
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: StashCard(
                              item: item,
                              onTap: () => Navigator.of(context).push(
                                MaterialPageRoute(builder: (_) => ContentDetailScreen(item: item)),
                              ),
                            ),
                          );
                        },
                      );
                    },
                    loading: () => const Center(child: CircularProgressIndicator()),
                    error: (e, _) => Center(child: Text('Something went wrong: $e')),
                  ),
          ),
        ],
      ),
    );
  }
}
