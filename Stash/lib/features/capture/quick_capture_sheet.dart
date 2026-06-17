import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../notes/note_editor_screen.dart';
import 'add_link_sheet.dart';

class QuickCaptureSheet extends StatelessWidget {
  const QuickCaptureSheet({super.key});

  static Future<void> show(BuildContext context) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => const QuickCaptureSheet(),
    );
  }

  Future<void> _addLink(BuildContext context) async {
    Navigator.of(context).pop();
    final clipboard = await Clipboard.getData('text/plain');
    final clipText = clipboard?.text?.trim();
    final initialUrl = (clipText != null && Uri.tryParse(clipText)?.isAbsolute == true) ? clipText : null;
    if (!context.mounted) return;
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => AddLinkSheet(initialUrl: initialUrl),
    );
  }

  void _addNote(BuildContext context) {
    Navigator.of(context).pop();
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const NoteEditorScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final items = <(IconData, String, VoidCallback)>[
      (Icons.link_rounded, 'Add Link', () => _addLink(context)),
      (Icons.edit_note_rounded, 'Add Note', () => _addNote(context)),
    ];

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(8, 20, 8, 8),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: items
              .map((i) => ListTile(
                    leading: Icon(i.$1, color: Theme.of(context).colorScheme.primary),
                    title: Text(i.$2),
                    onTap: i.$3,
                  ))
              .toList(),
        ),
      ),
    );
  }
}
