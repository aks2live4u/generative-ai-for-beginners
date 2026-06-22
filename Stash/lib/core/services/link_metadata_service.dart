import 'package:html/parser.dart' as html_parser;
import 'package:http/http.dart' as http;

import '../../models/content_item.dart';

class LinkMetadata {
  final String url;
  final String title;
  final String? description;
  final String? thumbnailUrl;
  final String? author;
  final SourcePlatform platform;
  final ContentType type;

  LinkMetadata({
    required this.url,
    required this.title,
    required this.platform,
    required this.type,
    this.description,
    this.thumbnailUrl,
    this.author,
  });
}

class LinkMetadataService {
  static SourcePlatform detectPlatform(String url) {
    final host = Uri.tryParse(url)?.host.toLowerCase() ?? '';
    if (host.contains('instagram.com')) return SourcePlatform.instagram;
    if (host.contains('youtube.com') || host.contains('youtu.be')) return SourcePlatform.youtube;
    if (host.contains('facebook.com') || host.contains('fb.watch')) return SourcePlatform.facebook;
    if (host.contains('reddit.com')) return SourcePlatform.reddit;
    if (host.contains('twitter.com') || host.contains('x.com')) return SourcePlatform.twitter;
    return SourcePlatform.web;
  }

  static ContentType _typeForPlatform(SourcePlatform platform) {
    switch (platform) {
      case SourcePlatform.instagram:
        return ContentType.instagramReel;
      case SourcePlatform.youtube:
        return ContentType.youtubeVideo;
      case SourcePlatform.facebook:
        return ContentType.facebookVideo;
      default:
        return ContentType.webArticle;
    }
  }

  /// Fetches the page and extracts OpenGraph metadata. Falls back to the bare
  /// URL as the title if the network request fails (e.g. offline, blocked).
  Future<LinkMetadata> fetch(String url) async {
    final platform = detectPlatform(url);
    final type = _typeForPlatform(platform);

    try {
      final response = await http
          .get(Uri.parse(url), headers: {'User-Agent': 'Mozilla/5.0 (compatible; StashApp/1.0)'})
          .timeout(const Duration(seconds: 8));
      final document = html_parser.parse(response.body);

      String? og(String property) {
        final el = document.querySelector('meta[property="$property"]') ??
            document.querySelector('meta[name="$property"]');
        return el?.attributes['content'];
      }

      final title = og('og:title') ?? document.querySelector('title')?.text.trim() ?? url;
      final description = og('og:description') ?? og('description');
      // Instagram's og:image is a pre-cropped square thumbnail regardless of
      // the original photo's aspect ratio, which is why image posts look
      // cropped while Reels/videos (whose frame is already that shape)
      // don't. The actual, uncropped photo URL is embedded directly in the
      // page's inline JSON, so for Instagram it's tried first; other
      // platforms keep checking og:image first since their pages don't
      // reliably contain that same JSON shape. The Twitter Card fallback and
      // embedded-JSON last-resort both also help recover a thumbnail at all
      // when Instagram strips OG tags entirely, which was the other reason
      // some posts had no picture.
      final image = platform == SourcePlatform.instagram
          ? _extractEmbeddedImage(response.body) ??
              og('og:image') ??
              og('twitter:image') ??
              og('twitter:image:src')
          : og('og:image') ??
              og('twitter:image') ??
              og('twitter:image:src') ??
              _extractEmbeddedImage(response.body);
      final author = og('og:site_name') ?? og('author');

      return LinkMetadata(
        url: url,
        title: title.trim().isEmpty ? url : title.trim(),
        description: description,
        thumbnailUrl: image,
        author: author,
        platform: platform,
        type: type,
      );
    } catch (_) {
      return LinkMetadata(
        url: url,
        title: url,
        platform: platform,
        type: type,
      );
    }
  }

  /// Last-resort fallback: pulls an image URL out of the raw HTML body's
  /// inline JSON (the fields Instagram/Facebook embed for their own client
  /// rendering), for pages that strip all OpenGraph/Twitter Card meta tags.
  static String? _extractEmbeddedImage(String body) {
    final patterns = [
      RegExp(r'"display_url"\s*:\s*"([^"]+)"'),
      RegExp(r'"thumbnail_url"\s*:\s*"([^"]+)"'),
      RegExp(r'"image_url"\s*:\s*"([^"]+)"'),
    ];
    for (final pattern in patterns) {
      final match = pattern.firstMatch(body);
      if (match != null) {
        // JSON-escaped slashes/ampersands need unescaping to form a valid URL.
        return match.group(1)?.replaceAll(r'\/', '/').replaceAll('\\u0026', '&');
      }
    }
    return null;
  }
}
