import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/stash_card.dart';
import '../home/content_detail_screen.dart';

class FavoritesScreen extends ConsumerWidget {
  const FavoritesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final itemsAsync = ref.watch(favoritesListProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Favorites')),
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
                onTap: () => Navigator.of(context).push(
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
