import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import 'package:uuid/uuid.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';

/// Builds an [Image] for both local (`file://`) and remote image
/// references, so images inserted from the device gallery render correctly
/// in the Markdown preview and on the detail screen.
Widget buildNoteMarkdownImage(Uri uri, String? title, String? alt) {
  if (uri.scheme == 'file') {
    return Image.file(File(uri.toFilePath()), fit: BoxFit.contain);
  }
  return Image.network(uri.toString(), fit: BoxFit.contain);
}

/// Rich-ish note editor (also used for personal articles). Supports
/// lightweight Markdown formatting (headings, bold, italic, lists,
/// checkboxes, quotes, code, links, images) inserted via the toolbar, with
/// a live preview toggle and autosave.
class NoteEditorScreen extends ConsumerStatefulWidget {
  final ContentItem? existing;
  final ContentType type;

  const NoteEditorScreen({super.key, this.existing, this.type = ContentType.personalNote});

  @override
  ConsumerState<NoteEditorScreen> createState() => _NoteEditorScreenState();
}

class _NoteEditorScreenState extends ConsumerState<NoteEditorScreen> {
  late final TextEditingController _titleController;
  late final TextEditingController _bodyController;
  late final TextEditingController _tagsController;
  bool _isDraft = false;
  bool _preview = false;
  bool _dirty = false;
  String _id = '';

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    _id = existing?.id ?? const Uuid().v4();
    _titleController = TextEditingController(text: existing?.title ?? '');
    _bodyController = TextEditingController(text: existing?.body ?? '');
    _tagsController = TextEditingController(text: existing?.tags.join(', ') ?? '');
    _isDraft = existing?.isDraft ?? true;
    for (final c in [_titleController, _bodyController, _tagsController]) {
      c.addListener(() => _dirty = true);
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _bodyController.dispose();
    _tagsController.dispose();
    super.dispose();
  }

  void _insert(String before, [String after = '']) {
    final text = _bodyController.text;
    final selection = _bodyController.selection;
    final start = selection.start < 0 ? text.length : selection.start;
    final end = selection.end < 0 ? text.length : selection.end;
    final selected = text.substring(start, end);
    final newText = text.replaceRange(start, end, '$before$selected$after');
    _bodyController.value = TextEditingValue(
      text: newText,
      selection: TextSelection.collapsed(offset: start + before.length + selected.length),
    );
    _dirty = true;
  }

  void _insertAt(int start, int end, String replacement) {
    final text = _bodyController.text;
    final newText = text.replaceRange(start, end, replacement);
    _bodyController.value = TextEditingValue(
      text: newText,
      selection: TextSelection.collapsed(offset: start + replacement.length),
    );
    setState(() => _dirty = true);
  }

  /// Inserts a Markdown link. Unlike a raw `[](url)` placeholder, this asks
  /// for the URL and, if no link text is given, fetches the page title so
  /// the inserted link reads naturally instead of showing a bare address.
  Future<void> _insertLink() async {
    final selection = _bodyController.selection;
    final text = _bodyController.text;
    final start = selection.start < 0 ? text.length : selection.start;
    final end = selection.end < 0 ? text.length : selection.end;
    final selectedText = text.substring(start, end);

    final urlController = TextEditingController();
    final labelController = TextEditingController(text: selectedText);
    final result = await showDialog<(String, String)>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Insert link'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: urlController,
              autofocus: true,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(hintText: 'https://...'),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: labelController,
              decoration: const InputDecoration(hintText: 'Link text (optional — fetched if left blank)'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, (urlController.text.trim(), labelController.text.trim())),
            child: const Text('Insert'),
          ),
        ],
      ),
    );
    if (result == null) return;
    final (url, label) = result;
    if (url.isEmpty) return;

    var linkText = label;
    if (linkText.isEmpty) {
      try {
        final metadata = await ref.read(linkMetadataServiceProvider).fetch(url);
        linkText = metadata.title;
      } catch (_) {
        linkText = url;
      }
    }
    if (!mounted) return;
    _insertAt(start, end, '[$linkText]($url)');
  }

  Future<void> _insertImage() async {
    final result = await FilePicker.platform.pickFiles(type: FileType.image);
    final pickedPath = result?.files.single.path;
    if (pickedPath == null) return;

    final docsDir = await getApplicationDocumentsDirectory();
    final imagesDir = Directory('${docsDir.path}/note_images');
    await imagesDir.create(recursive: true);
    final ext = pickedPath.split('.').last;
    final destPath = '${imagesDir.path}/${const Uuid().v4()}.$ext';
    await File(pickedPath).copy(destPath);

    if (!mounted) return;
    _insert('![](file://$destPath)', '');
  }

  Future<void> _save({bool publish = false}) async {
    final title = _titleController.text.trim();
    if (title.isEmpty) return;
    final tags = _tagsController.text
        .split(',')
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
    final repo = ref.read(contentRepositoryProvider);
    final existing = widget.existing;
    final now = DateTime.now();

    if (existing == null) {
      final item = ContentItem(
        id: _id,
        type: widget.type,
        platform: SourcePlatform.manual,
        title: title,
        body: _bodyController.text,
        createdAt: now,
        updatedAt: now,
        isDraft: publish ? false : _isDraft,
        tags: tags,
      );
      await repo.create(item);
    } else {
      await repo.update(existing.copyWith(
        title: title,
        body: _bodyController.text,
        isDraft: publish ? false : _isDraft,
        tags: tags,
        updatedAt: now,
      ));
    }
    _dirty = false;
    ref.invalidate(contentListProvider);
    ref.invalidate(searchResultsProvider);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Note'),
        actions: [
          IconButton(
            icon: Icon(_preview ? Icons.edit_rounded : Icons.visibility_rounded),
            tooltip: _preview ? 'Edit' : 'Preview',
            onPressed: () => setState(() => _preview = !_preview),
          ),
          TextButton(
            onPressed: () async {
              await _save(publish: true);
              if (context.mounted) Navigator.of(context).pop();
            },
            child: const Text('Save'),
          ),
        ],
      ),
      body: PopScope(
        canPop: true,
        onPopInvokedWithResult: (didPop, result) {
          if (didPop && _dirty) _save();
        },
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: TextField(
                controller: _titleController,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
                decoration: const InputDecoration(
                  hintText: 'Title',
                  filled: false,
                  border: InputBorder.none,
                ),
              ),
            ),
            const Divider(height: 1),
            if (!_preview)
              _Toolbar(
                actions: [
                  (Icons.title_rounded, 'Heading', () => _insert('## ', '')),
                  (Icons.format_bold_rounded, 'Bold', () => _insert('**', '**')),
                  (Icons.format_italic_rounded, 'Italic', () => _insert('_', '_')),
                  (Icons.format_list_bulleted_rounded, 'Bullet list', () => _insert('- ', '')),
                  (Icons.format_list_numbered_rounded, 'Numbered list', () => _insert('1. ', '')),
                  (Icons.check_box_outlined, 'Checkbox', () => _insert('- [ ] ', '')),
                  (Icons.format_quote_rounded, 'Quote', () => _insert('> ', '')),
                  (Icons.code_rounded, 'Code', () => _insert('`', '`')),
                  (Icons.link_rounded, 'Link', _insertLink),
                  (Icons.image_rounded, 'Image', _insertImage),
                ],
              ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: _preview
                    ? Markdown(
                        data: _bodyController.text.isEmpty ? '_Nothing yet…_' : _bodyController.text,
                        imageBuilder: buildNoteMarkdownImage,
                      )
                    : TextField(
                        controller: _bodyController,
                        maxLines: null,
                        expands: true,
                        textAlignVertical: TextAlignVertical.top,
                        decoration: const InputDecoration(
                          hintText: 'Start writing…',
                          filled: false,
                          border: InputBorder.none,
                        ),
                      ),
              ),
            ),
            const Divider(height: 1),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _tagsController,
                      decoration: const InputDecoration(
                        hintText: 'Tags, comma separated',
                        isDense: true,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('Draft'),
                      Switch(
                        value: _isDraft,
                        onChanged: (v) => setState(() => _isDraft = v),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Toolbar extends StatelessWidget {
  final List<(IconData, String, VoidCallback)> actions;

  const _Toolbar({required this.actions});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 44,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        children: actions
            .map((a) => IconButton(
                  icon: Icon(a.$1, size: 20),
                  tooltip: a.$2,
                  onPressed: a.$3,
                ))
            .toList(),
      ),
    );
  }
}
