import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../models/content_item.dart';
import 'platform_icon.dart';

class StashCard extends StatelessWidget {
  final ContentItem item;
  final VoidCallback onTap;
  final VoidCallback? onFavoriteToggle;

  const StashCard({
    super.key,
    required this.item,
    required this.onTap,
    this.onFavoriteToggle,
  });

  String get _relativeDate {
    final diff = DateTime.now().difference(item.createdAt);
    if (diff.inDays >= 1) return 'Saved ${diff.inDays}d ago';
    if (diff.inHours >= 1) return 'Saved ${diff.inHours}h ago';
    return 'Saved just now';
  }

  bool get _hasThumb => item.thumbnailUrl != null && item.thumbnailUrl!.isNotEmpty;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: EdgeInsets.zero,
      child: InkWell(
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_hasThumb)
              AspectRatio(
                aspectRatio: 4 / 3,
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    CachedNetworkImage(
                      imageUrl: item.thumbnailUrl!,
                      fit: BoxFit.cover,
                      errorWidget: (context, url, error) => Container(color: theme.colorScheme.surface),
                    ),
                    if (onFavoriteToggle != null)
                      Positioned(
                        right: 6,
                        top: 6,
                        child: _FavoriteButton(active: item.favorite, onTap: onFavoriteToggle!),
                      ),
                  ],
                ),
              )
            else
              Container(
                height: item.type == ContentType.personalNote ? 70 : 120,
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                color: theme.colorScheme.primary.withValues(alpha: 0.08),
                child: Align(
                  alignment: Alignment.topRight,
                  child: onFavoriteToggle != null
                      ? _FavoriteButton(active: item.favorite, onTap: onFavoriteToggle!)
                      : const SizedBox.shrink(),
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      PlatformIcon(platform: item.platform, size: 14),
                      const SizedBox(width: 6),
                      Text(
                        PlatformIcon(platform: item.platform).label,
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    item.title,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                  ),
                  if (item.tags.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    Text(
                      item.tags.take(3).join(' • '),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.primary,
                      ),
                    ),
                  ],
                  const SizedBox(height: 6),
                  Text(
                    _relativeDate,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurface.withValues(alpha: 0.5),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FavoriteButton extends StatelessWidget {
  final bool active;
  final VoidCallback onTap;

  const _FavoriteButton({required this.active, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      customBorder: const CircleBorder(),
      child: Container(
        padding: const EdgeInsets.all(6),
        decoration: BoxDecoration(
          color: Colors.black.withValues(alpha: 0.45),
          shape: BoxShape.circle,
        ),
        child: Icon(
          active ? Icons.star_rounded : Icons.star_border_rounded,
          size: 18,
          color: active ? const Color(0xFFF97316) : Colors.white,
        ),
      ),
    );
  }
}
