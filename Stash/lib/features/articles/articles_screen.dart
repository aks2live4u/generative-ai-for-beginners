import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/empty_state.dart';
import '../home/content_detail_screen.dart';

final _articlesProvider = FutureProvider.autoDispose((ref) async {
  final repo = ref.watch(contentRepositoryProvider);
  return repo.all(types: [ContentType.webArticle, ContentType.personalArticle]);
});

class ArticlesScreen extends ConsumerWidget {
  const ArticlesScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final articlesAsync = ref.watch(_articlesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Articles')),
      body: articlesAsync.when(
        data: (articles) {
          if (articles.isEmpty) {
            return const EmptyState(
              icon: Icons.article_outlined,
              title: 'No articles saved',
              message: 'Share a web article to Stash, or write your own from the + button.',
            );
          }
          return ListView.separated(
            padding: const EdgeInsets.all(12),
            itemCount: articles.length,
            separatorBuilder: (context, index) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              final article = articles[index];
              return _ArticleTile(article: article);
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Something went wrong: $e')),
      ),
    );
  }
}

class _ArticleTile extends StatelessWidget {
  final ContentItem article;

  const _ArticleTile({required this.article});

  int get _readingMinutes {
    final words = (article.body ?? article.description ?? '').split(RegExp(r'\s+')).length;
    return (words / 200).ceil().clamp(1, 99);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hasThumb = article.thumbnailUrl != null && article.thumbnailUrl!.isNotEmpty;
    return Card(
      margin: EdgeInsets.zero,
      child: InkWell(
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => ContentDetailScreen(item: article)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (hasThumb)
                ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: CachedNetworkImage(
                    imageUrl: article.thumbnailUrl!,
                    width: 72,
                    height: 72,
                    fit: BoxFit.cover,
                  ),
                )
              else
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(Icons.article_rounded),
                ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      article.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      article.author ?? (Uri.tryParse(article.url ?? '')?.host ?? 'Personal'),
                      style: theme.textTheme.bodySmall,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '$_readingMinutes min read',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurface.withValues(alpha: 0.5),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
