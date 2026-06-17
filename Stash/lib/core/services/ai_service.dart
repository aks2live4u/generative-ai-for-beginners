import 'dart:convert';

import 'package:http/http.dart' as http;

class AiOrganizationResult {
  final List<String> tags;
  final List<String> suggestedCollections;
  final String summary;

  AiOrganizationResult({
    required this.tags,
    required this.suggestedCollections,
    required this.summary,
  });
}

/// Generates tags, collection suggestions and a short summary for saved
/// content. Uses the Gemini API when an API key is configured in Settings;
/// otherwise falls back to a lightweight on-device keyword heuristic so the
/// app remains fully functional without any network/AI dependency.
class AiService {
  final String? apiKey;

  AiService({this.apiKey});

  static const _stopWords = {
    'the', 'a', 'an', 'of', 'to', 'for', 'and', 'or', 'in', 'on', 'with',
    'is', 'are', 'this', 'that', 'how', 'why', 'what', 'your', 'you', 'i',
    'my', 'top', 'best', 'new',
  };

  Future<AiOrganizationResult> organize({
    required String title,
    String? description,
  }) async {
    if (apiKey != null && apiKey!.trim().isNotEmpty) {
      try {
        return await _organizeWithGemini(title: title, description: description);
      } catch (_) {
        // Network/API failure: fall back to the local heuristic below.
      }
    }
    return _organizeHeuristically(title: title, description: description);
  }

  Future<AiOrganizationResult> _organizeWithGemini({
    required String title,
    String? description,
  }) async {
    final prompt = '''
You are organizing a saved item for a personal knowledge vault app called Stash.
Title: $title
Description: ${description ?? ''}

Respond ONLY with compact JSON in this exact shape:
{"tags": ["..."], "collections": ["..."], "summary": "one or two sentence summary"}
Provide 3-6 tags and 1-4 collection names. Keep them short (1-2 words).
''';

    final uri = Uri.parse(
      'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey',
    );
    final response = await http
        .post(
          uri,
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({
            'contents': [
              {'parts': [{'text': prompt}]}
            ]
          }),
        )
        .timeout(const Duration(seconds: 15));

    final data = jsonDecode(response.body) as Map<String, dynamic>;
    final text = data['candidates'][0]['content']['parts'][0]['text'] as String;
    final jsonStart = text.indexOf('{');
    final jsonEnd = text.lastIndexOf('}');
    final parsed = jsonDecode(text.substring(jsonStart, jsonEnd + 1)) as Map<String, dynamic>;

    return AiOrganizationResult(
      tags: (parsed['tags'] as List).map((e) => e.toString()).toList(),
      suggestedCollections: (parsed['collections'] as List).map((e) => e.toString()).toList(),
      summary: parsed['summary']?.toString() ?? '',
    );
  }

  AiOrganizationResult _organizeHeuristically({
    required String title,
    String? description,
  }) {
    final text = '$title ${description ?? ''}'.toLowerCase();
    final words = text
        .split(RegExp(r'[^a-z0-9]+'))
        .where((w) => w.length > 3 && !_stopWords.contains(w))
        .toSet()
        .take(6)
        .map((w) => w[0].toUpperCase() + w.substring(1))
        .toList();

    final collections = words.take(3).toList();
    final summary = description != null && description.isNotEmpty
        ? (description.length > 160 ? '${description.substring(0, 160)}...' : description)
        : title;

    return AiOrganizationResult(
      tags: words,
      suggestedCollections: collections,
      summary: summary,
    );
  }
}
