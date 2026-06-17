import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:receive_sharing_intent/receive_sharing_intent.dart';

import 'core/theme/app_theme.dart';
import 'features/capture/add_link_sheet.dart';
import 'features/collections/collections_screen.dart';
import 'features/home/home_screen.dart';
import 'features/search/search_screen.dart';
import 'features/settings/settings_screen.dart';
import 'state/providers.dart';

void main() {
  runApp(const ProviderScope(child: StashApp()));
}

class StashApp extends ConsumerWidget {
  const StashApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);

    ThemeData darkVariant;
    ThemeMode materialThemeMode;
    switch (themeMode) {
      case StashThemeMode.light:
        darkVariant = AppTheme.dark;
        materialThemeMode = ThemeMode.light;
        break;
      case StashThemeMode.dark:
        darkVariant = AppTheme.dark;
        materialThemeMode = ThemeMode.dark;
        break;
      case StashThemeMode.amoled:
        darkVariant = AppTheme.amoled;
        materialThemeMode = ThemeMode.dark;
        break;
      case StashThemeMode.system:
        darkVariant = AppTheme.dark;
        materialThemeMode = ThemeMode.system;
        break;
    }

    return MaterialApp(
      title: 'Stash',
      debugShowCheckedModeBanner: false,
      themeMode: materialThemeMode,
      theme: AppTheme.light,
      darkTheme: darkVariant,
      home: const RootScaffold(),
    );
  }
}

class RootScaffold extends StatefulWidget {
  const RootScaffold({super.key});

  @override
  State<RootScaffold> createState() => _RootScaffoldState();
}

class _RootScaffoldState extends State<RootScaffold> {
  int _index = 0;

  static const _screens = [
    HomeScreen(),
    CollectionsScreen(),
    SearchScreen(),
    SettingsScreen(),
  ];

  @override
  void initState() {
    super.initState();
    _wireShareIntent();
  }

  /// Listens for links shared into the app via Android's share sheet
  /// ("Share to Stash") both on cold start and while the app is running.
  void _wireShareIntent() {
    ReceiveSharingIntent.instance.getInitialMedia().then(_handleSharedFiles);
    ReceiveSharingIntent.instance.getMediaStream().listen(_handleSharedFiles);
  }

  void _handleSharedFiles(List<SharedMediaFile> files) {
    if (files.isEmpty) return;
    final shared = files.first;
    final text = shared.type == SharedMediaType.url || shared.type == SharedMediaType.text
        ? shared.path
        : null;
    if (text == null) return;
    final uri = Uri.tryParse(text);
    if (uri == null || !uri.isAbsolute) return;

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        backgroundColor: Theme.of(context).colorScheme.surface,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
        builder: (_) => AddLinkSheet(initialUrl: text),
      );
    });
    ReceiveSharingIntent.instance.reset();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _screens),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_rounded), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.collections_bookmark_rounded), label: 'Collections'),
          NavigationDestination(icon: Icon(Icons.search_rounded), label: 'Search'),
          NavigationDestination(icon: Icon(Icons.settings_rounded), label: 'Settings'),
        ],
      ),
    );
  }
}
