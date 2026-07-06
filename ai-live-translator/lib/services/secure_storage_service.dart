import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stores the user's OpenAI API key in the platform keystore (Android
/// EncryptedSharedPreferences / Keystore-backed). The key never touches
/// disk in plaintext and is never bundled with the app or sent anywhere
/// except as an Authorization header on direct calls to api.openai.com.
class SecureStorageService {
  SecureStorageService._internal();
  static final SecureStorageService instance = SecureStorageService._internal();

  static const _apiKeyStorageKey = 'openai_api_key';

  final _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  Future<String?> getApiKey() =>
      _storage.read(key: _apiKeyStorageKey).timeout(
        const Duration(seconds: 5),
        onTimeout: () => null,
      );

  Future<void> setApiKey(String apiKey) =>
      _storage.write(key: _apiKeyStorageKey, value: apiKey.trim());

  Future<void> clearApiKey() => _storage.delete(key: _apiKeyStorageKey);

  Future<bool> hasApiKey() async {
    final key = await getApiKey();
    return key != null && key.isNotEmpty;
  }
}
