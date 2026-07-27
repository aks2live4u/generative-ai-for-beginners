import 'dart:math';
import 'package:flutter/material.dart';
import '../../../core/constants/app_constants.dart';
import 'particle_painter.dart';

class _ThemeSpec {
  final List<Color> gradient;
  final ParticleShape shape;
  final Color particleColor;
  final int particleCount;

  const _ThemeSpec(this.gradient, this.shape, this.particleColor, this.particleCount);
}

Map<AppBackgroundTheme, _ThemeSpec> _specFor(Brightness brightness) {
  final dark = brightness == Brightness.dark;
  return {
    AppBackgroundTheme.morning: _ThemeSpec(
      dark ? [const Color(0xFF2B2420), const Color(0xFF3D2E28)] : [const Color(0xFFFFE9D6), const Color(0xFFFFF6EC)],
      ParticleShape.sparkle,
      const Color(0xFFFFC98B),
      14,
    ),
    AppBackgroundTheme.night: _ThemeSpec(
      [const Color(0xFF14152B), const Color(0xFF272A4D)],
      ParticleShape.star,
      Colors.white,
      36,
    ),
    AppBackgroundTheme.cafe: _ThemeSpec(
      dark ? [const Color(0xFF2A211B), const Color(0xFF3B2C22)] : [const Color(0xFFF3E1CC), const Color(0xFFFBF0E2)],
      ParticleShape.sparkle,
      const Color(0xFFB98352),
      10,
    ),
    AppBackgroundTheme.library: _ThemeSpec(
      dark ? [const Color(0xFF221E28), const Color(0xFF332C3B)] : [const Color(0xFFEFE6D8), const Color(0xFFF9F4EA)],
      ParticleShape.sparkle,
      const Color(0xFFC9A876),
      8,
    ),
    AppBackgroundTheme.forest: _ThemeSpec(
      dark ? [const Color(0xFF16241D), const Color(0xFF1F3327)] : [const Color(0xFFDDF0E1), const Color(0xFFF1FAF3)],
      ParticleShape.leaf,
      const Color(0xFF6FAE7F),
      12,
    ),
    AppBackgroundTheme.rain: _ThemeSpec(
      dark ? [const Color(0xFF1B222B), const Color(0xFF232E3A)] : [const Color(0xFFD8E7F2), const Color(0xFFEFF6FB)],
      ParticleShape.rainDrop,
      const Color(0xFF7FA9C9),
      40,
    ),
    AppBackgroundTheme.galaxy: _ThemeSpec(
      [const Color(0xFF120B27), const Color(0xFF2B1750)],
      ParticleShape.star,
      const Color(0xFFE3D2FF),
      44,
    ),
    AppBackgroundTheme.japaneseRoom: _ThemeSpec(
      dark ? [const Color(0xFF2A1F24), const Color(0xFF3B2830)] : [const Color(0xFFFCE4EC), const Color(0xFFFFF3F6)],
      ParticleShape.petal,
      const Color(0xFFF3A8C0),
      14,
    ),
  };
}

/// A slow, looping gradient + particle backdrop for the home/focus screen.
/// Purely procedural (no image assets) so every theme works fully offline.
class AnimatedBackground extends StatefulWidget {
  final AppBackgroundTheme theme;
  final bool animate;
  final Widget? child;

  const AnimatedBackground({super.key, required this.theme, this.animate = true, this.child});

  @override
  State<AnimatedBackground> createState() => _AnimatedBackgroundState();
}

class _AnimatedBackgroundState extends State<AnimatedBackground> with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late List<Particle> _particles;
  AppBackgroundTheme? _particlesTheme;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: const Duration(seconds: 14));
    if (widget.animate) _controller.repeat();
  }

  @override
  void didUpdateWidget(covariant AnimatedBackground oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.animate && !_controller.isAnimating) {
      _controller.repeat();
    } else if (!widget.animate && _controller.isAnimating) {
      _controller.stop();
    }
  }

  List<Particle> _particlesFor(int count) {
    final rand = Random(widget.theme.index * 97 + 13);
    return List.generate(count, (_) => Particle.random(rand, fallsDown: true));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final spec = _specFor(brightness)[widget.theme]!;
    if (_particlesTheme != widget.theme) {
      _particles = _particlesFor(spec.particleCount);
      _particlesTheme = widget.theme;
    }

    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: spec.gradient, begin: Alignment.topCenter, end: Alignment.bottomCenter),
      ),
      child: Stack(
        children: [
          Positioned.fill(
            child: AnimatedBuilder(
              animation: _controller,
              builder: (context, _) => CustomPaint(
                painter: ParticlePainter(
                  particles: _particles,
                  t: _controller.value,
                  shape: spec.shape,
                  color: spec.particleColor,
                ),
              ),
            ),
          ),
          if (widget.child != null) Positioned.fill(child: widget.child!),
        ],
      ),
    );
  }
}
