import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../core/services/ai_service.dart';
import '../core/services/link_metadata_service.dart';
import '../core/theme/app_theme.dart';
import '../data/collection_repository.dart';
import '../data/content_repository.dart';
import '../models/content_item.dart';

final contentRepositoryProvider = Provider((ref) => ContentRepository());
final collectionRepositoryProvider = Provider((ref) => CollectionRepository());
final linkMetadataServiceProvider = Provider((ref) => LinkMetadataService());

final sharedPreferencesProvider = FutureProvider((ref) => SharedPreferences.getInstance());

final geminiApiKeyProvider = StateNotifierProvider<GeminiApiKeyNotifier, String?>(
  (ref) => GeminiApiKeyNotifier(ref),
);

class GeminiApiKeyNotifier extends StateNotifier<String?> {
  final Ref ref;
  GeminiApiKeyNotifier(this.ref) : super(null) {
    _load();
  }

  Future<void> _load() async {
    final prefs = await ref.read(sharedPreferencesProvider.future);
    state = prefs.getString('gemini_api_key');
  }

  Future<void> set(String? key) async {
    final prefs = await ref.read(sharedPreferencesProvider.future);
    if (key == null || key.isEmpty) {
      await prefs.remove('gemini_api_key');
    } else {
      await prefs.setString('gemini_api_key', key);
    }
    state = key;
  }
}

final aiServiceProvider = Provider((ref) {
  final key = ref.watch(geminiApiKeyProvider);
  return AiService(apiKey: key);
});

final themeModeProvider = StateNotifierProvider<ThemeModeNotifier, StashThemeMode>(
  (ref) => ThemeModeNotifier(ref),
);

class ThemeModeNotifier extends StateNotifier<StashThemeMode> {
  final Ref ref;
  ThemeModeNotifier(this.ref) : super(StashThemeMode.dark) {
    _load();
  }

  Future<void> _load() async {
    final prefs = await ref.read(sharedPreferencesProvider.future);
    final stored = prefs.getString('theme_mode');
    if (stored != null) {
      state = StashThemeMode.values.firstWhere((e) => e.name == stored, orElse: () => StashThemeMode.dark);
    }
  }

  Future<void> set(StashThemeMode mode) async {
    final prefs = await ref.read(sharedPreferencesProvider.future);
    await prefs.setString('theme_mode', mode.name);
    state = mode;
  }
}

/// Filter state for the Home feed and Search screen.
class ContentFilter {
  final List<ContentType>? types;
  final String? collectionId;
  final bool onlyFavorites;
  final bool includeInbox;

  const ContentFilter({
    this.types,
    this.collectionId,
    this.onlyFavorites = false,
    this.includeInbox = true,
  });
}

final contentFilterProvider = StateProvider((ref) => const ContentFilter());

final contentListProvider = FutureProvider.autoDispose((ref) async {
  final repo = ref.watch(contentRepositoryProvider);
  final filter = ref.watch(contentFilterProvider);
  return repo.all(
    types: filter.types,
    collectionId: filter.collectionId,
    onlyFavorites: filter.onlyFavorites,
    includeInbox: filter.includeInbox,
  );
});

final collectionsListProvider = FutureProvider.autoDispose((ref) async {
  final repo = ref.watch(collectionRepositoryProvider);
  return repo.all();
});

final searchQueryProvider = StateProvider((ref) => '');

final searchResultsProvider = FutureProvider.autoDispose<List<ContentItem>>((ref) async {
  final query = ref.watch(searchQueryProvider);
  if (query.trim().isEmpty) return [];
  final repo = ref.watch(contentRepositoryProvider);
  return repo.search(query);
});

