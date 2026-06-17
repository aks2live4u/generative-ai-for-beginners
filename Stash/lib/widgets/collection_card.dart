import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

import '../models/collection.dart';

class CollectionCard extends StatelessWidget {
  final Collection collection;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  const CollectionCard({super.key, required this.collection, required this.onTap, this.onLongPress});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hasCover = collection.coverImage != null && collection.coverImage!.isNotEmpty;
    return Card(
      margin: EdgeInsets.zero,
      child: InkWell(
        onTap: onTap,
        onLongPress: onLongPress,
        child: AspectRatio(
          aspectRatio: 1,
          child: Stack(
            fit: StackFit.expand,
            children: [
              if (hasCover)
                CachedNetworkImage(imageUrl: collection.coverImage!, fit: BoxFit.cover)
              else
                Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        theme.colorScheme.primary.withValues(alpha: 0.25),
                        theme.colorScheme.secondary.withValues(alpha: 0.25),
                      ],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                  ),
                ),
              Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Colors.transparent, Colors.black.withValues(alpha: 0.55)],
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                  ),
                ),
              ),
              Positioned(
                left: 12,
                right: 12,
                bottom: 10,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      collection.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700),
                    ),
                    Text(
                      '${collection.itemCount} items',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 12),
                    ),
                  ],
                ),
              ),
              if (collection.pinned)
                const Positioned(top: 8, right: 8, child: Icon(Icons.push_pin_rounded, color: Colors.white, size: 18)),
              if (collection.favorite)
                Positioned(
                  top: 8,
                  left: 8,
                  child: Icon(Icons.star_rounded, color: theme.colorScheme.secondary, size: 18),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
