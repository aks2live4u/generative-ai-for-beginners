import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../../core/constants/app_constants.dart';

/// A tiny animated companion rendered from emoji rather than art assets -
/// everything here is procedural so the app stays fully offline with zero
/// binary art dependencies.
class MascotWidget extends StatefulWidget {
  final Mascot mascot;
  final MascotMood mood;
  final double size;
  final bool animate;

  const MascotWidget({
    super.key,
    required this.mascot,
    this.mood = MascotMood.idle,
    this.size = 96,
    this.animate = true,
  });

  @override
  State<MascotWidget> createState() => _MascotWidgetState();
}

class _MascotWidgetState extends State<MascotWidget> with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: _durationFor(widget.mood))
      ..repeat(reverse: _reverseFor(widget.mood));
  }

  @override
  void didUpdateWidget(covariant MascotWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.mood != widget.mood) {
      _controller.duration = _durationFor(widget.mood);
      _controller
        ..reset()
        ..repeat(reverse: _reverseFor(widget.mood));
    }
  }

  Duration _durationFor(MascotMood mood) => switch (mood) {
    MascotMood.idle => const Duration(milliseconds: 2200),
    MascotMood.happy => const Duration(milliseconds: 700),
    MascotMood.sleeping => const Duration(milliseconds: 2600),
    MascotMood.thinking => const Duration(milliseconds: 1600),
    MascotMood.celebrating => const Duration(milliseconds: 500),
  };

  bool _reverseFor(MascotMood mood) => mood != MascotMood.celebrating;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  String get _emoji => switch (widget.mood) {
    MascotMood.idle => widget.mascot.idleEmoji,
    MascotMood.happy => widget.mascot.happyEmoji,
    MascotMood.sleeping => widget.mascot.idleEmoji,
    MascotMood.thinking => widget.mascot.idleEmoji,
    MascotMood.celebrating => widget.mascot.idleEmoji,
  };

  String? get _accessory => switch (widget.mood) {
    MascotMood.sleeping => '💤',
    MascotMood.thinking => '💭',
    MascotMood.celebrating => '✨',
    _ => null,
  };

  @override
  Widget build(BuildContext context) {
    if (!widget.animate) {
      return _MascotFace(emoji: _emoji, accessory: _accessory, size: widget.size);
    }
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final t = _controller.value;
        final transform = Matrix4.identity();
        switch (widget.mood) {
          case MascotMood.idle:
            final scale = 1.0 + 0.04 * t;
            transform.scaleByDouble(scale, scale, 1, 1);
          case MascotMood.happy:
            transform.translateByDouble(0.0, -8.0 * math.sin(t * math.pi), 0, 1);
          case MascotMood.sleeping:
            final scale = 1.0 + 0.015 * t;
            transform.scaleByDouble(scale, scale, 1, 1);
          case MascotMood.thinking:
            transform.rotateZ(0.06 * math.sin(t * 2 * math.pi));
          case MascotMood.celebrating:
            transform
              ..translateByDouble(0.0, -12.0 * (t < 0.5 ? t * 2 : (1 - t) * 2), 0, 1)
              ..rotateZ(0.12 * math.sin(t * 2 * math.pi));
        }
        return Opacity(
          opacity: widget.mood == MascotMood.sleeping ? 0.85 : 1.0,
          child: Transform(alignment: Alignment.center, transform: transform, child: child),
        );
      },
      child: _MascotFace(emoji: _emoji, accessory: _accessory, size: widget.size),
    );
  }
}

class _MascotFace extends StatelessWidget {
  final String emoji;
  final String? accessory;
  final double size;

  const _MascotFace({required this.emoji, required this.accessory, required this.size});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size * 1.3,
      height: size * 1.3,
      child: Stack(
        alignment: Alignment.center,
        clipBehavior: Clip.none,
        children: [
          Text(emoji, style: TextStyle(fontSize: size)),
          if (accessory != null)
            Positioned(
              top: 0,
              right: size * 0.1,
              child: Text(accessory!, style: TextStyle(fontSize: size * 0.32)),
            ),
        ],
      ),
    );
  }
}
