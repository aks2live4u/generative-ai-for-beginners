import 'package:flutter/material.dart';

/// Simple pulsing-ring animation shown behind the mic button while
/// listening. Ring size reacts to [amplitude] (dBFS, roughly -160..0) so it
/// visibly grows louder as the speaker gets louder.
class WaveAnimation extends StatefulWidget {
  const WaveAnimation({
    super.key,
    required this.isActive,
    required this.amplitude,
    required this.color,
  });

  final bool isActive;
  final double amplitude;
  final Color color;

  @override
  State<WaveAnimation> createState() => _WaveAnimationState();
}

class _WaveAnimationState extends State<WaveAnimation>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final loudness = ((widget.amplitude + 60) / 60).clamp(0.0, 1.0);
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return CustomPaint(
          size: const Size(260, 260),
          painter: _RingPainter(
            progress: _controller.value,
            active: widget.isActive,
            loudness: loudness,
            color: widget.color,
          ),
        );
      },
    );
  }
}

class _RingPainter extends CustomPainter {
  _RingPainter({
    required this.progress,
    required this.active,
    required this.loudness,
    required this.color,
  });

  final double progress;
  final bool active;
  final double loudness;
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final baseRadius = size.shortestSide / 3.4;

    if (active) {
      for (final phase in [0.0, 0.33, 0.66]) {
        final t = (progress + phase) % 1.0;
        final radius = baseRadius + (t * baseRadius * (0.6 + loudness));
        final opacity = (1 - t).clamp(0.0, 1.0) * 0.35;
        final paint = Paint()
          ..color = color.withValues(alpha: opacity)
          ..style = PaintingStyle.fill;
        canvas.drawCircle(center, radius, paint);
      }
    }

    final corePaint = Paint()..color = color.withValues(alpha: active ? 1 : 0.7);
    canvas.drawCircle(center, baseRadius * (0.62 + (active ? loudness * 0.1 : 0)), corePaint);
  }

  @override
  bool shouldRepaint(covariant _RingPainter oldDelegate) =>
      oldDelegate.progress != progress ||
      oldDelegate.active != active ||
      oldDelegate.loudness != loudness;
}
