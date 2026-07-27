import 'dart:math';
import 'package:confetti/confetti.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/settings_provider.dart';
import '../widgets/mascot/mascot_widget.dart';
import '../../core/constants/app_constants.dart';

class CelebrationScreen extends ConsumerStatefulWidget {
  final int rewardAmount;
  const CelebrationScreen({super.key, required this.rewardAmount});

  @override
  ConsumerState<CelebrationScreen> createState() => _CelebrationScreenState();
}

class _CelebrationScreenState extends ConsumerState<CelebrationScreen> {
  late final ConfettiController _confetti;

  @override
  void initState() {
    super.initState();
    _confetti = ConfettiController(duration: const Duration(seconds: 2));
    _confetti.play();
    Future.delayed(const Duration(seconds: 4), () {
      if (mounted) Navigator.of(context).maybePop();
    });
  }

  @override
  void dispose() {
    _confetti.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final settings = ref.watch(settingsControllerProvider);

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Stack(
        alignment: Alignment.topCenter,
        children: [
          Align(
            alignment: Alignment.topCenter,
            child: ConfettiWidget(
              confettiController: _confetti,
              blastDirection: pi / 2,
              blastDirectionality: BlastDirectionality.explosive,
              numberOfParticles: 24,
              gravity: 0.25,
              shouldLoop: false,
              colors: [theme.colorScheme.primary, theme.colorScheme.secondary, Colors.white],
            ),
          ),
          Center(
            child: Card(
              margin: const EdgeInsets.symmetric(horizontal: 32),
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    MascotWidget(mascot: settings.mascot, mood: MascotMood.celebrating, size: 90),
                    const SizedBox(height: 20),
                    Text('✨ Great Job!', style: theme.textTheme.headlineMedium, textAlign: TextAlign.center),
                    const SizedBox(height: 10),
                    Text(
                      'You completed one focus session!',
                      style: theme.textTheme.bodyLarge,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 16),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.secondary,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        '+${widget.rewardAmount} ${settings.rewardCurrency.emoji}',
                        style: theme.textTheme.titleMedium,
                      ),
                    ),
                    const SizedBox(height: 24),
                    ElevatedButton(onPressed: () => Navigator.of(context).maybePop(), child: const Text('Continue')),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
