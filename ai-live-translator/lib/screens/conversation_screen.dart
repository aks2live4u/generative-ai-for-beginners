import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/languages_config.dart';
import '../services/conversation_controller.dart';
import '../widgets/detected_language_chip.dart';
import '../widgets/glass.dart';
import '../widgets/language_badge.dart';
import '../widgets/message_card.dart';
import '../widgets/mic_button.dart';

class ConversationScreen extends StatelessWidget {
  const ConversationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: GlassAppBar(
        title: Consumer<ConversationController>(
          builder: (context, controller, _) => LanguageBadge(
            english: controller.english,
            nativeLanguage: controller.nativeLanguage,
          ),
        ),
        actions: [
          Consumer<ConversationController>(
            builder: (context, controller, _) => IconButton(
              onPressed: controller.messages.isEmpty ? null : controller.clearConversation,
              icon: const Icon(Icons.delete_outline_rounded),
              tooltip: 'Clear conversation',
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Consumer<ConversationController>(
          builder: (context, controller, _) {
            return Column(
              children: [
                const SizedBox(height: 4),
                _StatusLine(controller: controller),
                if (controller.lastMessage != null) ...[
                  const SizedBox(height: 8),
                  DetectedLanguageChip(
                    language: LanguagesConfig.byCode(controller.lastMessage!.spokenLanguageCode),
                  ),
                ],
                if (!controller.isOnline)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
                    child: _ErrorBanner(
                      message:
                          'No internet connection.\nTranslation requires an online AI service.',
                    ),
                  )
                else if (controller.errorMessage != null)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
                    child: _ErrorBanner(message: controller.errorMessage!),
                  ),
                const SizedBox(height: 8),
                MicButton(
                  status: controller.status,
                  amplitude: controller.amplitude,
                  mode: controller.mode,
                  onHoldStart: controller.isOnline ? controller.startPushToTalk : () {},
                  onHoldEnd: controller.isOnline
                      ? controller.stopPushToTalkAndProcess
                      : () {},
                ),
                if (controller.mode == ConversationMode.pushToTalk)
                  Text('Hold to speak', style: Theme.of(context).textTheme.bodyMedium),
                const SizedBox(height: 12),
                _ModeSelector(controller: controller),
                const SizedBox(height: 12),
                Expanded(
                  child: controller.messages.isEmpty
                      ? Center(
                          child: Text(
                            'Say something to begin translating.',
                            style: Theme.of(context)
                                .textTheme
                                .bodyLarge
                                ?.copyWith(color: Theme.of(context).colorScheme.outline),
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          itemCount: controller.messages.length,
                          itemBuilder: (context, index) {
                            final message = controller.messages[index];
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 12),
                              child: MessageCard(
                                message: message,
                                settings: controller.settings,
                                onReplayOriginal: () => controller.replayOriginal(message),
                                onReplayTranslation: () => controller.replayTranslation(message),
                              ),
                            );
                          },
                        ),
                ),
                _BottomBar(controller: controller),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _StatusLine extends StatelessWidget {
  const _StatusLine({required this.controller});
  final ConversationController controller;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = switch (controller.status) {
      ListeningStatus.listening => 'Listening…',
      ListeningStatus.processing => 'Translating…',
      ListeningStatus.speaking => 'Speaking…',
      ListeningStatus.error => 'Something went wrong',
      ListeningStatus.idle => controller.mode == ConversationMode.auto
          ? 'Auto conversation active'
          : 'Ready',
    };
    final dotColor = switch (controller.status) {
      ListeningStatus.listening => scheme.tertiary,
      ListeningStatus.processing => scheme.secondary,
      ListeningStatus.speaking => scheme.primary,
      ListeningStatus.error => scheme.error,
      ListeningStatus.idle => scheme.outline,
    };
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(color: dotColor, shape: BoxShape.circle),
        ),
        const SizedBox(width: 8),
        Text(text, style: Theme.of(context).textTheme.bodyMedium),
      ],
    );
  }
}

class _ModeSelector extends StatelessWidget {
  const _ModeSelector({required this.controller});
  final ConversationController controller;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: SegmentedButton<ConversationMode>(
        segments: const [
          ButtonSegment(
            value: ConversationMode.pushToTalk,
            label: Text('Push to Talk'),
            icon: Icon(Icons.touch_app_rounded, size: 18),
          ),
          ButtonSegment(
            value: ConversationMode.auto,
            label: Text('Auto Conversation'),
            icon: Icon(Icons.podcasts_rounded, size: 18),
          ),
        ],
        selected: {controller.mode},
        onSelectionChanged: (s) => controller.setMode(s.first),
      ),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return GlassContainer(
      borderRadius: 12,
      tint: scheme.errorContainer,
      opacity: 0.55,
      padding: const EdgeInsets.all(12),
      child: Text(
        message,
        textAlign: TextAlign.center,
        style: TextStyle(color: scheme.onErrorContainer),
      ),
    );
  }
}

class _BottomBar extends StatelessWidget {
  const _BottomBar({required this.controller});
  final ConversationController controller;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _BarButton(
            icon: Icons.replay_rounded,
            label: 'Replay',
            onTap: controller.messages.isEmpty ? null : controller.replayLast,
          ),
          _BarButton(
            icon: controller.muted ? Icons.volume_off_rounded : Icons.volume_up_rounded,
            label: controller.muted ? 'Muted' : 'Mute',
            onTap: controller.toggleMute,
          ),
        ],
      ),
    );
  }
}

class _BarButton extends StatelessWidget {
  const _BarButton({required this.icon, required this.label, required this.onTap});
  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        GlassIconButton(icon: icon, onPressed: onTap, tooltip: label, size: 56),
        const SizedBox(height: 4),
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
      ],
    );
  }
}
