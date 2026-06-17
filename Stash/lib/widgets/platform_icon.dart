import 'package:flutter/material.dart';

import '../models/content_item.dart';

class PlatformIcon extends StatelessWidget {
  final SourcePlatform platform;
  final double size;

  const PlatformIcon({super.key, required this.platform, this.size = 16});

  IconData get _icon {
    switch (platform) {
      case SourcePlatform.instagram:
        return Icons.camera_alt_rounded;
      case SourcePlatform.youtube:
        return Icons.play_circle_fill_rounded;
      case SourcePlatform.facebook:
        return Icons.facebook_rounded;
      case SourcePlatform.reddit:
        return Icons.forum_rounded;
      case SourcePlatform.twitter:
        return Icons.tag_rounded;
      case SourcePlatform.web:
        return Icons.public_rounded;
      case SourcePlatform.manual:
        return Icons.edit_note_rounded;
    }
  }

  Color _color(BuildContext context) {
    switch (platform) {
      case SourcePlatform.instagram:
        return const Color(0xFFE1306C);
      case SourcePlatform.youtube:
        return const Color(0xFFFF0000);
      case SourcePlatform.facebook:
        return const Color(0xFF1877F2);
      case SourcePlatform.reddit:
        return const Color(0xFFFF4500);
      case SourcePlatform.twitter:
        return const Color(0xFF1DA1F2);
      case SourcePlatform.web:
        return Theme.of(context).colorScheme.secondary;
      case SourcePlatform.manual:
        return Theme.of(context).colorScheme.primary;
    }
  }

  String get label {
    switch (platform) {
      case SourcePlatform.instagram:
        return 'Instagram';
      case SourcePlatform.youtube:
        return 'YouTube';
      case SourcePlatform.facebook:
        return 'Facebook';
      case SourcePlatform.reddit:
        return 'Reddit';
      case SourcePlatform.twitter:
        return 'Twitter/X';
      case SourcePlatform.web:
        return 'Web';
      case SourcePlatform.manual:
        return 'Note';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Icon(_icon, size: size, color: _color(context));
  }
}
