import 'dart:math' as math;

import 'package:flutter/material.dart';

/// Glowing gradient ring + flanking waveform bars shown behind the mic
/// button while listening — a soft, animated backdrop rather than a plain
/// pulsing circle. Bar heights react to [amplitude] (dBFS, roughly
/// -160..0) so it visibly moves as the speaker gets louder.
class WaveAnimation extends StatefulWidget {
  const WaveAnimation({
    super.key,
    required this.isActive,
    required this.amplitude,
    required this.gradient,
  });

  final bool isActive;
  final double amplitude;
  final List<Color> gradient;

  @override
  State<WaveAnimation> createState() => _WaveAnimationState();
}

class _WaveAnimationState extends State<WaveAnimation>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 2200),
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
          size: const Size(320, 220),
          painter: _MicBackdropPainter(
            progress: _controller.value,
            active: widget.isActive,
            loudness: loudness,
            gradient: widget.gradient,
          ),
        );
      },
    );
  }
}

class _MicBackdropPainter extends CustomPainter {
  _MicBackdropPainter({
    required this.progress,
    required this.active,
    required this.loudness,
    required this.gradient,
  });

  final double progress;
  final bool active;
  final double loudness;
  final List<Color> gradient;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final coreRadius = size.height / 2.9;

    _paintGlow(canvas, center, coreRadius);
    _paintRing(canvas, center, coreRadius);
    _paintBars(canvas, center, coreRadius, mirror: false, color: gradient.first);
    _paintBars(canvas, center, coreRadius, mirror: true, color: gradient.last);
    _paintCore(canvas, center, coreRadius);
  }

  void _paintGlow(Canvas canvas, Offset center, double coreRadius) {
    final glowRadius = coreRadius * (active ? 2.0 + loudness * 0.4 : 1.6);
    final paint = Paint()
      ..shader = RadialGradient(
        colors: [gradient.first.withValues(alpha: active ? 0.35 : 0.18), Colors.transparent],
      ).createShader(Rect.fromCircle(center: center, radius: glowRadius))
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 18);
    canvas.drawCircle(center, glowRadius, paint);
  }

  void _paintRing(Canvas canvas, Offset center, double coreRadius) {
    final ringRadius = coreRadius * 1.28;
    final sweep = SweepGradient(
      startAngle: 0,
      endAngle: math.pi * 2,
      transform: GradientRotation(progress * math.pi * 2),
      colors: [...gradient, gradient.first],
    );
    final paint = Paint()
      ..shader = sweep.createShader(Rect.fromCircle(center: center, radius: ringRadius))
      ..style = PaintingStyle.stroke
      ..strokeWidth = active ? 4 : 2.5
      ..strokeCap = StrokeCap.round;
    canvas.drawCircle(center, ringRadius, paint);
  }

  void _paintCore(Canvas canvas, Offset center, double coreRadius) {
    final paint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: gradient,
      ).createShader(Rect.fromCircle(center: center, radius: coreRadius));
    canvas.drawCircle(center, coreRadius * (1 + (active ? loudness * 0.05 : 0)), paint);
  }

  void _paintBars(
    Canvas canvas,
    Offset center,
    double coreRadius,
    { required bool mirror, required Color color }
  ) {
    const barCount = 5;
    const barGap = 10.0;
    const barWidth = 7.0;
    final baseX = center.dx + coreRadius * 1.55 * (mirror ? 1 : -1);
    final direction = mirror ? 1 : -1;

    for (var i = 0; i < barCount; i++) {
      final idlePhase = (progress * 2 * math.pi) + i * 0.7;
      final idleWave = active ? (math.sin(idlePhase) + 1) / 2 : 0.25;
      final distanceFactor = 1 - (i / barCount) * 0.55;
      final heightFactor = active
          ? (0.25 + loudness * 0.75) * distanceFactor * (0.5 + idleWave * 0.5)
          : 0.18 * distanceFactor;
      final barHeight = (_barMaxHeight(coreRadius) * heightFactor).clamp(6.0, _barMaxHeight(coreRadius));

      final x = baseX + direction * i * (barWidth + barGap);
      final rect = RRect.fromRectAndRadius(
        Rect.fromCenter(center: Offset(x, center.dy), width: barWidth, height: barHeight),
        const Radius.circular(barWidth / 2),
      );
      final paint = Paint()
        ..color = color.withValues(alpha: active ? (1 - i / barCount * 0.6) : 0.35);
      canvas.drawRRect(rect, paint);
    }
  }

  double _barMaxHeight(double coreRadius) => coreRadius * 1.7;

  @override
  bool shouldRepaint(covariant _MicBackdropPainter oldDelegate) =>
      oldDelegate.progress != progress ||
      oldDelegate.active != active ||
      oldDelegate.loudness != loudness;
}
