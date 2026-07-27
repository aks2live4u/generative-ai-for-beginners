import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:home_widget/home_widget.dart';

import '../../data/repositories/hive_boxes.dart';
import '../providers/pomodoro_controller.dart';
import '../providers/repositories_providers.dart';
import 'home_screen.dart';
import 'settings_screen.dart';
import 'stats_screen.dart';
import 'tasks_screen.dart';

/// Bottom navigation shell hosting the four main tabs. Also owns the
/// app-lifecycle hook that force-reloads Hive data on resume, since the
/// foreground task's isolate may have written sessions while this isolate
/// was paused (see HiveBoxes.refreshAll for why that's necessary).
class HomeShell extends ConsumerStatefulWidget {
  const HomeShell({super.key});

  @override
  ConsumerState<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends ConsumerState<HomeShell> with WidgetsBindingObserver {
  int _index = 0;
  StreamSubscription<Uri?>? _widgetClickSub;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkWidgetLaunch();
  }

  Future<void> _checkWidgetLaunch() async {
    final initialUri = await HomeWidget.initiallyLaunchedFromHomeWidget();
    _handleWidgetUri(initialUri);
    _widgetClickSub = HomeWidget.widgetClicked.listen(_handleWidgetUri);
  }

  void _handleWidgetUri(Uri? uri) {
    if (uri?.host == 'start' && mounted) {
      ref.read(pomodoroControllerProvider.notifier).startFocus();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _widgetClickSub?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      HiveBoxes.refreshAll().then((_) {
        if (mounted) ref.read(refreshTickProvider.notifier).state++;
      });
    }
  }

  static const _screens = [HomeScreen(), TasksScreen(), StatsScreen(), SettingsScreen()];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _screens),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.favorite_rounded), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.check_circle_outline_rounded), label: 'Tasks'),
          NavigationDestination(icon: Icon(Icons.bar_chart_rounded), label: 'Stats'),
          NavigationDestination(icon: Icon(Icons.settings_rounded), label: 'Settings'),
        ],
      ),
    );
  }
}
