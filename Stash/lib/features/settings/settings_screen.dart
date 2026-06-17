import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';
import 'package:file_picker/file_picker.dart';

import '../../core/services/backup_service.dart';
import '../../core/theme/app_theme.dart';
import '../../state/providers.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  Future<void> _exportBackup(BuildContext context) async {
    final file = await BackupService().exportZip();
    await Share.shareXFiles([XFile(file.path)], text: 'Stash backup');
  }

  Future<void> _importBackup(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Restore backup?'),
        content: const Text('This replaces everything currently in your vault with the backup contents.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Restore')),
        ],
      ),
    );
    if (confirmed != true) return;

    final result = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: ['zip']);
    if (result == null || result.files.single.path == null) return;
    await BackupService().importZip(File(result.files.single.path!));
    ref.invalidate(contentListProvider);
    ref.invalidate(collectionsListProvider);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Backup restored')));
    }
  }

  Future<void> _editApiKey(BuildContext context, WidgetRef ref) async {
    final controller = TextEditingController(text: ref.read(geminiApiKeyProvider) ?? '');
    final newKey = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Gemini API Key'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: 'Paste your Gemini API key'),
          obscureText: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, controller.text.trim()), child: const Text('Save')),
        ],
      ),
    );
    if (newKey == null) return;
    await ref.read(geminiApiKeyProvider.notifier).set(newKey);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);
    final apiKey = ref.watch(geminiApiKeyProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          const _SectionHeader('Theme'),
          RadioListTile<StashThemeMode>(
            title: const Text('System default'),
            value: StashThemeMode.system,
            groupValue: themeMode,
            onChanged: (v) => ref.read(themeModeProvider.notifier).set(v!),
          ),
          RadioListTile<StashThemeMode>(
            title: const Text('Light'),
            value: StashThemeMode.light,
            groupValue: themeMode,
            onChanged: (v) => ref.read(themeModeProvider.notifier).set(v!),
          ),
          RadioListTile<StashThemeMode>(
            title: const Text('Dark'),
            value: StashThemeMode.dark,
            groupValue: themeMode,
            onChanged: (v) => ref.read(themeModeProvider.notifier).set(v!),
          ),
          RadioListTile<StashThemeMode>(
            title: const Text('AMOLED Dark'),
            value: StashThemeMode.amoled,
            groupValue: themeMode,
            onChanged: (v) => ref.read(themeModeProvider.notifier).set(v!),
          ),
          const Divider(),
          const _SectionHeader('AI Settings'),
          ListTile(
            title: const Text('Gemini API Key'),
            subtitle: Text(apiKey == null || apiKey.isEmpty
                ? 'Not set — using on-device tagging (works offline, fewer/simpler tags)'
                : '•••• configured — using Gemini for richer tags & summaries'),
            trailing: const Icon(Icons.chevron_right_rounded),
            onTap: () => _editApiKey(context, ref),
          ),
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Text(
              'Saving and organizing works fully without a key. Adding a free Gemini '
              'API key from Google AI Studio improves the tags, collection suggestions, '
              'and summaries generated for new saves — instead of the built-in offline '
              'heuristic, which just pulls a few keywords out of the title and description.',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ),
          const Divider(),
          const _SectionHeader('Backup'),
          ListTile(
            title: const Text('Export backup'),
            subtitle: const Text('Save a ZIP with your full vault — no cloud required'),
            leading: const Icon(Icons.upload_rounded),
            onTap: () => _exportBackup(context),
          ),
          ListTile(
            title: const Text('Import backup'),
            subtitle: const Text('Restore from a previously exported ZIP'),
            leading: const Icon(Icons.download_rounded),
            onTap: () => _importBackup(context, ref),
          ),
          const Divider(),
          const _SectionHeader('About'),
          ListTile(
            leading: ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: Image.asset('assets/icon/icon.png', width: 40, height: 40),
            ),
            title: const Text('Stash'),
            subtitle: const Text('Personal AI-powered content vault. Local-first, no login, no cloud.'),
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader(this.title);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
      child: Text(
        title.toUpperCase(),
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.w700,
              letterSpacing: 1.1,
            ),
      ),
    );
  }
}
