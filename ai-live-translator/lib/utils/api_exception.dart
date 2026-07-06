/// Thrown by any service that calls OpenAI, so the UI can show one of the
/// friendly PRD error messages instead of a raw stack trace.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

/// Raised when no API key has been entered in Settings yet.
class MissingApiKeyException implements Exception {
  @override
  String toString() =>
      'No OpenAI API key found. Add one in Settings to start translating.';
}
