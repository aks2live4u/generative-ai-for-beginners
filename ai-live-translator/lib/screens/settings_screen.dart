import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/app_settings.dart';
import '../services/secure_storage_service.dart';
import '../services/settings_controller.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _apiKeyController = TextEditingController();
  bool _obscureKey = true;
  bool _loadingKey = true;
  bool _hasSavedKey = false;
  String? _saveMessage;

  @override
  void initState() {
    super.initState();
    _loadKey();
  }

  Future<void> _loadKey() async {
    final key = await SecureStorageService.instance.getApiKey();
    if (!mounted) return;
    setState(() {
      _hasSavedKey = key != null && key.isNotEmpty;
      _loadingKey = false;
    });
  }

  Future<void> _saveKey() async {
    final value = _apiKeyController.text.trim();
    if (value.isEmpty) return;
    await SecureStorageService.instance.setApiKey(value);
    _apiKeyController.clear();
    if (!mounted) return;
    setState(() {
      _hasSavedKey = true;
      _saveMessage = 'API key saved securely on this device.';
    });
  }

  Future<void> _clearKey() async {
    await SecureStorageService.instance.clearApiKey();
    if (!mounted) return;
    setState(() {
      _hasSavedKey = false;
      _saveMessage = 'API key removed.';
    });
  }

  @override
  void dispose() {
    _apiKeyController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final settingsController = context.watch<SettingsController>();
    final settings = settingsController.settings;

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _SectionTitle('OpenAI API Key'),
          Text(
            _hasSavedKey
                ? 'A key is saved on this device (hidden for your security).'
                : 'No key saved yet. Translation will not work until you add one.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 12),
          if (!_loadingKey)
            TextField(
              controller: _apiKeyController,
              obscureText: _obscureKey,
              decoration: InputDecoration(
                labelText: 'sk-...',
                border: const OutlineInputBorder(),
                suffixIcon: IconButton(
                  icon: Icon(_obscureKey ? Icons.visibility : Icons.visibility_off),
                  onPressed: () => setState(() => _obscureKey = !_obscureKey),
                ),
              ),
            ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: _saveKey,
                  child: const Text('Save Key'),
                ),
              ),
              const SizedBox(width: 12),
              if (_hasSavedKey)
                OutlinedButton(onPressed: _clearKey, child: const Text('Remove')),
            ],
          ),
          if (_saveMessage != null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(_saveMessage!,
                  style: TextStyle(color: Theme.of(context).colorScheme.primary)),
            ),
          const SizedBox(height: 8),
          TextButton.icon(
            onPressed: () => launchUrl(
              Uri.parse('https://platform.openai.com/api-keys'),
              mode: LaunchMode.externalApplication,
            ),
            icon: const Icon(Icons.open_in_new_rounded),
            label: const Text('Get an OpenAI API key'),
          ),
          const Divider(height: 40),

          _SectionTitle('Language Pair'),
          const Text('English ↔ Telugu (more Indian languages coming soon)'),
          const Divider(height: 40),

          _SectionTitle('Appearance'),
          SegmentedButton<AppThemeMode>(
            segments: const [
              ButtonSegment(value: AppThemeMode.light, label: Text('Light')),
              ButtonSegment(value: AppThemeMode.dark, label: Text('Dark')),
              ButtonSegment(value: AppThemeMode.auto, label: Text('Auto')),
            ],
            selected: {settings.themeMode},
            onSelectionChanged: (s) => settingsController.setThemeMode(s.first),
          ),
          const Divider(height: 40),

          _SectionTitle('Voice'),
          SegmentedButton<VoiceGender>(
            segments: const [
              ButtonSegment(value: VoiceGender.female, label: Text('Female')),
              ButtonSegment(value: VoiceGender.male, label: Text('Male')),
            ],
            selected: {settings.voiceGender},
            onSelectionChanged: (s) => settingsController.setVoiceGender(s.first),
          ),
          const SizedBox(height: 16),
          Text('Speech speed: ${settings.speechSpeed.toStringAsFixed(2)}x'),
          Slider(
            value: settings.speechSpeed,
            min: 0.5,
            max: 1.5,
            divisions: 10,
            onChanged: settingsController.setSpeechSpeed,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Auto-play voice'),
            value: settings.autoPlayVoice,
            onChanged: settingsController.setAutoPlayVoice,
          ),
          const SizedBox(height: 8),
          Text('Microphone sensitivity: ${(settings.microphoneSensitivity * 100).round()}%'),
          Slider(
            value: settings.microphoneSensitivity,
            onChanged: settingsController.setMicrophoneSensitivity,
          ),
          const Divider(height: 40),

          _SectionTitle('Display'),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Show original script'),
            value: settings.showOriginalScript,
            onChanged: settingsController.setShowOriginalScript,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Show transliteration'),
            value: settings.showTransliteration,
            onChanged: settingsController.setShowTransliteration,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Show translation'),
            value: settings.showTranslation,
            onChanged: settingsController.setShowTranslation,
          ),
          const Divider(height: 40),

          _SectionTitle('Privacy'),
          const Text(
            'Conversations are never saved. Recorded audio is deleted from '
            'this device immediately after each translation. There is no '
            'account, no login, and no cloud storage.',
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(text, style: Theme.of(context).textTheme.titleLarge),
    );
  }
}
