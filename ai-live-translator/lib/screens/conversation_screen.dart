import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/conversation_controller.dart';
import '../widgets/language_badge.dart';
import '../widgets/message_card.dart';
import '../widgets/mic_button.dart';

class ConversationScreen extends StatelessWidget {
  const ConversationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Consumer<ConversationController>(
          builder: (context, controller, _) => LanguageBadge(
            languageA: controller.languageA,
            languageB: controller.languageB,
            onSwap: controller.swapLanguages,
          ),
        ),
      ),
      body: SafeArea(
        child: Consumer<ConversationController>(
          builder: (context, controller, _) {
            return Column(
              children: [
                const SizedBox(height: 8),
                _StatusLine(controller: controller),
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
                const SizedBox(height: 12),
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
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      'Hold to speak',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                const SizedBox(height: 16),
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
                                onReplay: index == 0 ? controller.replayLast : null,
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
    final text = switch (controller.status) {
      ListeningStatus.listening => 'Listening…',
      ListeningStatus.processing => 'Translating…',
      ListeningStatus.speaking => 'Speaking…',
      ListeningStatus.error => 'Something went wrong',
      ListeningStatus.idle => controller.mode == ConversationMode.auto
          ? 'Auto conversation active'
          : 'Ready',
    };
    return Text(text, style: Theme.of(context).textTheme.bodyMedium);
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.errorContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        message,
        textAlign: TextAlign.center,
        style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer),
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
            icon: Icons.swap_horiz_rounded,
            label: 'Swap',
            onTap: controller.swapLanguages,
          ),
          _BarButton(
            icon: controller.mode == ConversationMode.auto
                ? Icons.podcasts_rounded
                : Icons.touch_app_rounded,
            label: controller.mode == ConversationMode.auto ? 'Auto: On' : 'Auto: Off',
            onTap: () => controller.setMode(
              controller.mode == ConversationMode.auto
                  ? ConversationMode.pushToTalk
                  : ConversationMode.auto,
            ),
          ),
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
        IconButton.filledTonal(onPressed: onTap, icon: Icon(icon)),
        const SizedBox(height: 4),
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
      ],
    );
  }
}
