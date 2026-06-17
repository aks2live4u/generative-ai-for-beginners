import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../models/content_item.dart';
import '../../state/providers.dart';

/// Rich-ish note / personal article editor. Supports lightweight Markdown
/// formatting (headings, bold, italic, lists, checkboxes, quotes, code,
/// links) inserted via the toolbar, with a live preview toggle and autosave.
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
    final isArticle = widget.type == ContentType.personalArticle;
    return Scaffold(
      appBar: AppBar(
        title: Text(isArticle ? 'Personal Article' : 'Note'),
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
            if (!_preview) _Toolbar(onInsert: _insert),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: _preview
                    ? Markdown(data: _bodyController.text.isEmpty ? '_Nothing yet…_' : _bodyController.text)
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
  final void Function(String before, [String after]) onInsert;

  const _Toolbar({required this.onInsert});

  @override
  Widget build(BuildContext context) {
    final actions = <(IconData, String, String, String)>[
      (Icons.title_rounded, 'Heading', '## ', ''),
      (Icons.format_bold_rounded, 'Bold', '**', '**'),
      (Icons.format_italic_rounded, 'Italic', '_', '_'),
      (Icons.format_list_bulleted_rounded, 'Bullet list', '- ', ''),
      (Icons.format_list_numbered_rounded, 'Numbered list', '1. ', ''),
      (Icons.check_box_outlined, 'Checkbox', '- [ ] ', ''),
      (Icons.format_quote_rounded, 'Quote', '> ', ''),
      (Icons.code_rounded, 'Code', '`', '`'),
      (Icons.link_rounded, 'Link', '[', '](url)'),
    ];
    return SizedBox(
      height: 44,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        children: actions
            .map((a) => IconButton(
                  icon: Icon(a.$1, size: 20),
                  tooltip: a.$2,
                  onPressed: () => onInsert(a.$3, a.$4),
                ))
            .toList(),
      ),
    );
  }
}
