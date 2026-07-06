import 'package:flutter/material.dart';

import '../services/conversation_controller.dart';
import 'wave_animation.dart';

/// The big central microphone. In push-to-talk mode, hold it down to
/// record and release to translate. In auto-conversation mode it just
/// reflects current status (listening/processing/speaking) — the pipeline
/// starts and stops itself via voice activity detection.
class MicButton extends StatelessWidget {
  const MicButton({
    super.key,
    required this.status,
    required this.amplitude,
    required this.mode,
    required this.onHoldStart,
    required this.onHoldEnd,
  });

  final ListeningStatus status;
  final double amplitude;
  final ConversationMode mode;
  final VoidCallback onHoldStart;
  final VoidCallback onHoldEnd;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = switch (status) {
      ListeningStatus.error => scheme.error,
      ListeningStatus.listening => scheme.primary,
      ListeningStatus.processing => scheme.tertiary,
      ListeningStatus.speaking => scheme.secondary,
      ListeningStatus.idle => scheme.primary,
    };

    final icon = switch (status) {
      ListeningStatus.processing => Icons.hourglass_top_rounded,
      ListeningStatus.speaking => Icons.volume_up_rounded,
      ListeningStatus.error => Icons.error_outline_rounded,
      _ => Icons.mic_rounded,
    };

    return GestureDetector(
      onLongPressStart: mode == ConversationMode.pushToTalk
          ? (_) => onHoldStart()
          : null,
      onLongPressEnd:
          mode == ConversationMode.pushToTalk ? (_) => onHoldEnd() : null,
      child: SizedBox(
        width: 260,
        height: 260,
        child: Stack(
          alignment: Alignment.center,
          children: [
            WaveAnimation(
              isActive: status == ListeningStatus.listening,
              amplitude: amplitude,
              color: color,
            ),
            Icon(icon, size: 56, color: Theme.of(context).colorScheme.onPrimary),
          ],
        ),
      ),
    );
  }
}
