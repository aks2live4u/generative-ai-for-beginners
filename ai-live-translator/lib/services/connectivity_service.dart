import 'package:connectivity_plus/connectivity_plus.dart';

/// Thin wrapper so the rest of the app only asks "are we online?" — the
/// translation pipeline is entirely cloud-based and has nothing useful to
/// do without a connection.
class ConnectivityService {
  ConnectivityService._internal();
  static final ConnectivityService instance = ConnectivityService._internal();

  Future<bool> isOnline() async {
    try {
      final results = await Connectivity()
          .checkConnectivity()
          .timeout(const Duration(seconds: 3));
      return results.any((r) => r != ConnectivityResult.none);
    } catch (_) {
      // If the platform can't answer quickly, don't block the UI on it —
      // the actual API calls will surface a clear error if we're offline.
      return true;
    }
  }

  Stream<bool> get onStatusChange => Connectivity()
      .onConnectivityChanged
      .map((results) => results.any((r) => r != ConnectivityResult.none));
}
