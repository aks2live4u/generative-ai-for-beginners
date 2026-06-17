import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';

/// "Share to Stash" manual entry point: paste a link, the app fetches
/// OpenGraph metadata, runs AI tagging, and drops the item in the Inbox.
class AddLinkSheet extends ConsumerStatefulWidget {
  final String? initialUrl;

  const AddLinkSheet({super.key, this.initialUrl});

  @override
  ConsumerState<AddLinkSheet> createState() => _AddLinkSheetState();
}

class _AddLinkSheetState extends ConsumerState<AddLinkSheet> {
  late final _urlController = TextEditingController(text: widget.initialUrl ?? '');
  bool _saving = false;
  String? _error;

  Future<void> _save() async {
    final url = _urlController.text.trim();
    final uri = Uri.tryParse(url);
    if (uri == null || !uri.isAbsolute) {
      setState(() => _error = 'Enter a valid URL');
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });

    try {
      final metadataService = ref.read(linkMetadataServiceProvider);
      final metadata = await metadataService.fetch(url);
      final ai = ref.read(aiServiceProvider);
      final organized = await ai.organize(title: metadata.title, description: metadata.description);

      final collectionRepo = ref.read(collectionRepositoryProvider);
      final existingCollections = await collectionRepo.all();
      final collectionIds = <String>[];
      for (final name in organized.suggestedCollections) {
        final match = existingCollections.where((c) => c.name.toLowerCase() == name.toLowerCase());
        if (match.isNotEmpty) {
          collectionIds.add(match.first.id);
        } else {
          final created = await collectionRepo.create(name);
          collectionIds.add(created.id);
        }
      }

      final now = DateTime.now();
      final item = ContentItem(
        id: const Uuid().v4(),
        type: metadata.type,
        platform: metadata.platform,
        url: metadata.url,
        title: metadata.title,
        description: metadata.description,
        summary: organized.summary,
        thumbnailUrl: metadata.thumbnailUrl,
        author: metadata.author,
        createdAt: now,
        updatedAt: now,
        inInbox: true,
        tags: organized.tags,
        collectionIds: collectionIds,
      );
      await ref.read(contentRepositoryProvider).create(item);
      ref.invalidate(contentListProvider);
      ref.invalidate(inboxListProvider);
      ref.invalidate(collectionsListProvider);
      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      setState(() => _error = 'Could not save link: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 20,
        bottom: 20 + MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Add Link', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          TextField(
            controller: _urlController,
            autofocus: true,
            keyboardType: TextInputType.url,
            decoration: InputDecoration(
              hintText: 'Paste a link from Instagram, YouTube, Reddit…',
              errorText: _error,
            ),
            onSubmitted: (_) => _save(),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(
                      height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Save to Stash'),
            ),
          ),
        ],
      ),
    );
  }
}
