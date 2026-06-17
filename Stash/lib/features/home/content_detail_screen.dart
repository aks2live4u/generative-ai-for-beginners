import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';
import '../../widgets/platform_icon.dart';
import '../notes/note_editor_screen.dart' show NoteEditorScreen, buildNoteMarkdownImage;

class ContentDetailScreen extends ConsumerStatefulWidget {
  final ContentItem item;

  const ContentDetailScreen({super.key, required this.item});

  @override
  ConsumerState<ContentDetailScreen> createState() => _ContentDetailScreenState();
}

class _ContentDetailScreenState extends ConsumerState<ContentDetailScreen> {
  late ContentItem _item;
  int _checkboxBuildIndex = 0;

  @override
  void initState() {
    super.initState();
    _item = widget.item;
  }

  bool get _isWritten =>
      _item.type == ContentType.personalNote || _item.type == ContentType.personalArticle;

  Future<void> _openLink(BuildContext context, WidgetRef ref) async {
    await ref.read(contentRepositoryProvider).markOpened(_item.id);
    if (_item.url != null) {
      await launchUrl(Uri.parse(_item.url!), mode: LaunchMode.externalApplication);
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
    await ref.read(contentRepositoryProvider).delete(_item.id);
    ref.invalidate(contentListProvider);
    ref.invalidate(searchResultsProvider);
    if (context.mounted) Navigator.of(context).pop();
  }

  Future<void> _toggleFavorite(WidgetRef ref) async {
    await ref.read(contentRepositoryProvider).toggleFavorite(_item.id, !_item.favorite);
    ref.invalidate(contentListProvider);
    setState(() => _item = _item.copyWith(favorite: !_item.favorite));
  }

  List<RegExpMatch> get _checkboxMatches =>
      RegExp(r'^(\s*)- \[([ xX])\]', multiLine: true).allMatches(_item.body ?? '').toList();

  Widget _buildCheckbox(bool checked) {
    final matches = _checkboxMatches;
    final index = _checkboxBuildIndex++;
    final theme = Theme.of(context);
    VoidCallback? onTap;
    if (index < matches.length) {
      final match = matches[index];
      onTap = () => _toggleCheckbox(match);
    }
    return GestureDetector(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.only(right: 6),
        child: Icon(
          checked ? Icons.check_box_rounded : Icons.check_box_outline_blank_rounded,
          size: 20,
          color: checked ? theme.colorScheme.primary : theme.colorScheme.onSurface.withValues(alpha: 0.6),
        ),
      ),
    );
  }

  Future<void> _toggleCheckbox(RegExpMatch match) async {
    final body = _item.body ?? '';
    final bracketStart = body.indexOf('[', match.start) + 1;
    final currentChar = body[bracketStart];
    final newChar = currentChar.trim().isEmpty ? 'x' : ' ';
    final newBody = body.replaceRange(bracketStart, bracketStart + 1, newChar);
    final updated = _item.copyWith(body: newBody, updatedAt: DateTime.now());
    setState(() => _item = updated);
    await ref.read(contentRepositoryProvider).update(updated);
    ref.invalidate(contentListProvider);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        actions: [
          IconButton(
            icon: Icon(_item.favorite ? Icons.star_rounded : Icons.star_border_rounded),
            onPressed: () => _toggleFavorite(ref),
          ),
          if (_isWritten)
            IconButton(
              icon: const Icon(Icons.edit_rounded),
              onPressed: () async {
                await Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => NoteEditorScreen(existing: _item, type: _item.type)),
                );
                ref.invalidate(contentListProvider);
              },
            ),
          IconButton(icon: const Icon(Icons.delete_outline_rounded), onPressed: () => _delete(context, ref)),
        ],
      ),
      body: SingleChildScrollView(
        // Extra bottom inset so the "Open original" button (and anything
        // else at the end of the scroll content) clears the system
        // gesture/navigation bar instead of being hidden behind it.
        padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).padding.bottom),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Video/thumbnail leads the screen, with the rest of the
            // metadata (platform, title, summary, tags) styled at
            // consistent sizes underneath it instead of a much larger
            // title floating above.
            if (_item.thumbnailUrl != null && _item.thumbnailUrl!.isNotEmpty) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: CachedNetworkImage(imageUrl: _item.thumbnailUrl!, fit: BoxFit.cover),
              ),
              const SizedBox(height: 16),
            ],
            Row(
              children: [
                PlatformIcon(platform: _item.platform, size: 16),
                const SizedBox(width: 6),
                Text(
                  PlatformIcon(platform: _item.platform).label,
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(_item.title, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
            if (_item.summary != null && _item.summary!.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text('Summary', style: theme.textTheme.labelMedium?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Text(_item.summary!, style: theme.textTheme.bodyMedium),
            ],
            if (_isWritten && _item.body != null && _item.body!.isNotEmpty) ...[
              const SizedBox(height: 16),
              Builder(builder: (context) {
                _checkboxBuildIndex = 0;
                return Markdown(
                  data: _item.body!,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  imageBuilder: buildNoteMarkdownImage,
                  checkboxBuilder: _buildCheckbox,
                );
              }),
            ],
            if (_item.tags.isNotEmpty) ...[
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _item.tags.map((t) => Chip(label: Text(t))).toList(),
              ),
            ],
            if (_item.url != null) ...[
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
