import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/platform_icon.dart';
import '../notes/note_editor_screen.dart';

class ContentDetailScreen extends ConsumerWidget {
  final ContentItem item;

  const ContentDetailScreen({super.key, required this.item});

  bool get _isWritten =>
      item.type == ContentType.personalNote || item.type == ContentType.personalArticle;

  Future<void> _openLink(BuildContext context, WidgetRef ref) async {
    await ref.read(contentRepositoryProvider).markOpened(item.id);
    if (item.url != null) {
      await launchUrl(Uri.parse(item.url!), mode: LaunchMode.externalApplication);
    }
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete item?'),
        content: const Text('This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    await ref.read(contentRepositoryProvider).delete(item.id);
    ref.invalidate(contentListProvider);
    ref.invalidate(inboxListProvider);
    ref.invalidate(searchResultsProvider);
    if (context.mounted) Navigator.of(context).pop();
  }

  Future<void> _toggleFavorite(WidgetRef ref) async {
    await ref.read(contentRepositoryProvider).toggleFavorite(item.id, !item.favorite);
    ref.invalidate(contentListProvider);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        actions: [
          IconButton(
            icon: Icon(item.favorite ? Icons.star_rounded : Icons.star_border_rounded),
            onPressed: () => _toggleFavorite(ref),
          ),
          if (_isWritten)
            IconButton(
              icon: const Icon(Icons.edit_rounded),
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => NoteEditorScreen(existing: item, type: item.type)),
              ),
            ),
          IconButton(icon: const Icon(Icons.delete_outline_rounded), onPressed: () => _delete(context, ref)),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                PlatformIcon(platform: item.platform, size: 18),
                const SizedBox(width: 6),
                Text(PlatformIcon(platform: item.platform).label, style: theme.textTheme.labelMedium),
              ],
            ),
            const SizedBox(height: 12),
            Text(item.title, style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: 12),
            if (item.thumbnailUrl != null && item.thumbnailUrl!.isNotEmpty)
              ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: CachedNetworkImage(imageUrl: item.thumbnailUrl!, fit: BoxFit.cover),
              ),
            if (item.summary != null && item.summary!.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text('Summary', style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Text(item.summary!, style: theme.textTheme.bodyMedium),
            ],
            if (_isWritten && item.body != null && item.body!.isNotEmpty) ...[
              const SizedBox(height: 16),
              Markdown(
                data: item.body!,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
              ),
            ],
            if (item.tags.isNotEmpty) ...[
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: item.tags.map((t) => Chip(label: Text(t))).toList(),
              ),
            ],
            if (item.url != null) ...[
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: () => _openLink(context, ref),
                  icon: const Icon(Icons.open_in_new_rounded),
                  label: const Text('Open original'),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
