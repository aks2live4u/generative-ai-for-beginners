import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

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

  // Each saved item's createdAt is recorded precisely (to the millisecond)
  // at save time and is what the list is actually sorted by — this label
  // just needs enough granularity (minutes, not only hours/days) to make
  // that ordering visible instead of collapsing every recent save into the
  // same "just now" bucket.
  String get _relativeDate {
    final diff = DateTime.now().difference(item.createdAt);
    if (diff.inDays >= 7) return 'Saved ${DateFormat.yMMMd().format(item.createdAt)}';
    if (diff.inDays >= 1) return 'Saved ${diff.inDays}d ago';
    if (diff.inHours >= 1) return 'Saved ${diff.inHours}h ago';
    if (diff.inMinutes >= 1) return 'Saved ${diff.inMinutes}m ago';
    return 'Saved just now';
  }

  bool get _hasThumb => item.thumbnailUrl != null && item.thumbnailUrl!.isNotEmpty;

  String get _oneLineSummary {
    if (item.summary != null && item.summary!.isNotEmpty) return item.summary!;
    if (item.description != null && item.description!.isNotEmpty) return item.description!;
    return item.title;
  }

  // Tapping the photo shows a quick-glance info card (saved time, summary)
  // without leaving the feed; tapping the text strip below opens the full
  // detail screen / original link, matching how Pinterest separates "peek"
  // from "open" gestures.
  void _showQuickInfo(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Theme.of(context).colorScheme.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) {
        final theme = Theme.of(ctx);
        return Padding(
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  PlatformIcon(platform: item.platform, size: 16),
                  const SizedBox(width: 6),
                  Text(
                    PlatformIcon(platform: item.platform).label,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(item.title, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Text(_oneLineSummary, maxLines: 3, overflow: TextOverflow.ellipsis, style: theme.textTheme.bodyMedium),
              const SizedBox(height: 12),
              Text(
                _relativeDate,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurface.withValues(alpha: 0.5),
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    Navigator.pop(ctx);
                    onTap();
                  },
                  child: const Text('Open'),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          InkWell(
            onTap: () => _showQuickInfo(context),
            child: Stack(
              children: [
                if (_hasThumb)
                  CachedNetworkImage(
                    imageUrl: item.thumbnailUrl!,
                    fit: BoxFit.cover,
                    width: double.infinity,
                    // No fixed height/aspect ratio: the image keeps its own
                    // proportions so the grid produces Pinterest-style
                    // varied-height cards instead of uniformly cropped tiles.
                    placeholder: (context, url) =>
                        Container(height: 160, color: theme.colorScheme.primary.withValues(alpha: 0.08)),
                    errorWidget: (context, url, error) =>
                        Container(height: 160, color: theme.colorScheme.primary.withValues(alpha: 0.08)),
                  )
                else
                  Container(
                    height: item.type == ContentType.personalNote ? 70 : 120,
                    width: double.infinity,
                    color: theme.colorScheme.primary.withValues(alpha: 0.08),
                  ),
                if (onFavoriteToggle != null)
                  Positioned(
                    right: 6,
                    top: 6,
                    child: _FavoriteButton(active: item.favorite, onTap: onFavoriteToggle!),
                  ),
              ],
            ),
          ),
          InkWell(
            onTap: onTap,
            child: Padding(
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
          ),
        ],
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
