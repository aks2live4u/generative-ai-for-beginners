import 'dart:math';
import 'package:flutter/material.dart';

enum ParticleShape { leaf, star, rainDrop, sparkle, petal }

class Particle {
  final double x; // 0..1 horizontal seed position
  final double y0; // 0..1 initial vertical position
  final double speed; // loops per animation cycle
  final double size;
  final double driftAmount;
  final double driftFrequency;
  final double phase;
  final double opacity;

  Particle({
    required this.x,
    required this.y0,
    required this.speed,
    required this.size,
    required this.driftAmount,
    required this.driftFrequency,
    required this.phase,
    required this.opacity,
  });

  factory Particle.random(Random rand, {required bool fallsDown}) {
    return Particle(
      x: rand.nextDouble(),
      y0: rand.nextDouble(),
      speed: 0.4 + rand.nextDouble() * 0.8,
      size: 6 + rand.nextDouble() * 10,
      driftAmount: 0.02 + rand.nextDouble() * 0.04,
      driftFrequency: 1 + rand.nextDouble() * 2,
      phase: rand.nextDouble() * 2 * pi,
      opacity: 0.25 + rand.nextDouble() * 0.4,
    );
  }
}

class ParticlePainter extends CustomPainter {
  final List<Particle> particles;
  final double t; // 0..1 animation progress, loops
  final ParticleShape shape;
  final Color color;

  ParticlePainter({required this.particles, required this.t, required this.shape, required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    for (final p in particles) {
      final loopedY = (p.y0 + t * p.speed) % 1.0;
      final dx = sin(t * 2 * pi * p.driftFrequency + p.phase) * p.driftAmount;
      final dy = shape == ParticleShape.rainDrop ? loopedY : loopedY;
      final center = Offset((p.x + dx) * size.width, dy * size.height);
      final paint = Paint()
        ..color = color.withValues(alpha: p.opacity)
        ..style = PaintingStyle.fill;

      switch (shape) {
        case ParticleShape.sparkle:
          _drawSparkle(canvas, center, p.size * 0.5, paint);
        case ParticleShape.star:
          _drawSparkle(canvas, center, p.size * 0.4, paint);
        case ParticleShape.leaf:
          _drawLeaf(canvas, center, p.size, paint, t * 2 * pi + p.phase);
        case ParticleShape.petal:
          _drawLeaf(canvas, center, p.size * 0.8, paint, t * 2 * pi + p.phase);
        case ParticleShape.rainDrop:
          canvas.drawLine(center, center.translate(-2, p.size * 1.6), paint..strokeWidth = 2);
      }
    }
  }

  void _drawSparkle(Canvas canvas, Offset center, double radius, Paint paint) {
    canvas.drawCircle(center, radius, paint);
  }

  void _drawLeaf(Canvas canvas, Offset center, double size, Paint paint, double angle) {
    canvas.save();
    canvas.translate(center.dx, center.dy);
    canvas.rotate(angle);
    final path = Path()
      ..moveTo(0, -size / 2)
      ..quadraticBezierTo(size / 2, 0, 0, size / 2)
      ..quadraticBezierTo(-size / 2, 0, 0, -size / 2);
    canvas.drawPath(path, paint);
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant ParticlePainter oldDelegate) => oldDelegate.t != t;
}
