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
      final image = og('og:image');
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
}
