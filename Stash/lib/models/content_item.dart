enum ContentType {
  instagramReel,
  youtubeVideo,
  facebookVideo,
  webArticle,
  personalNote,
  personalArticle,
}

enum SourcePlatform {
  instagram,
  youtube,
  facebook,
  reddit,
  twitter,
  web,
  manual,
}

ContentType contentTypeFromString(String value) =>
    ContentType.values.firstWhere((e) => e.name == value, orElse: () => ContentType.webArticle);

SourcePlatform sourcePlatformFromString(String value) =>
    SourcePlatform.values.firstWhere((e) => e.name == value, orElse: () => SourcePlatform.web);

class ContentItem {
  final String id;
  final ContentType type;
  final SourcePlatform platform;
  final String? url;
  final String title;
  final String? description;
  final String? body; // rich/markdown body for notes & personal articles
  final String? summary;
  final String? thumbnailUrl;
  final String? author;
  final DateTime? publishedDate;
  final DateTime createdAt;
  final DateTime updatedAt;
  final bool favorite;
  final bool archived;
  final bool isDraft;
  final bool inInbox;
  final int openCount;
  final DateTime? lastOpenedAt;
  final List<String> tags;
  final List<String> collectionIds;

  const ContentItem({
    required this.id,
    required this.type,
    required this.platform,
    required this.title,
    required this.createdAt,
    required this.updatedAt,
    this.url,
    this.description,
    this.body,
    this.summary,
    this.thumbnailUrl,
    this.author,
    this.publishedDate,
    this.favorite = false,
    this.archived = false,
    this.isDraft = false,
    this.inInbox = false,
    this.openCount = 0,
    this.lastOpenedAt,
    this.tags = const [],
    this.collectionIds = const [],
  });

  int get wordCount => body == null || body!.trim().isEmpty
      ? 0
      : body!.trim().split(RegExp(r'\s+')).length;

  ContentItem copyWith({
    ContentType? type,
    SourcePlatform? platform,
    String? url,
    String? title,
    String? description,
    String? body,
    String? summary,
    String? thumbnailUrl,
    String? author,
    DateTime? publishedDate,
    DateTime? updatedAt,
    bool? favorite,
    bool? archived,
    bool? isDraft,
    bool? inInbox,
    int? openCount,
    DateTime? lastOpenedAt,
    List<String>? tags,
    List<String>? collectionIds,
  }) {
    return ContentItem(
      id: id,
      type: type ?? this.type,
      platform: platform ?? this.platform,
      url: url ?? this.url,
      title: title ?? this.title,
      description: description ?? this.description,
      body: body ?? this.body,
      summary: summary ?? this.summary,
      thumbnailUrl: thumbnailUrl ?? this.thumbnailUrl,
      author: author ?? this.author,
      publishedDate: publishedDate ?? this.publishedDate,
      createdAt: createdAt,
      updatedAt: updatedAt ?? DateTime.now(),
      favorite: favorite ?? this.favorite,
      archived: archived ?? this.archived,
      isDraft: isDraft ?? this.isDraft,
      inInbox: inInbox ?? this.inInbox,
      openCount: openCount ?? this.openCount,
      lastOpenedAt: lastOpenedAt ?? this.lastOpenedAt,
      tags: tags ?? this.tags,
      collectionIds: collectionIds ?? this.collectionIds,
    );
  }

  Map<String, dynamic> toMap() => {
        'id': id,
        'type': type.name,
        'platform': platform.name,
        'url': url,
        'title': title,
        'description': description,
        'body': body,
        'summary': summary,
        'thumbnailUrl': thumbnailUrl,
        'author': author,
        'publishedDate': publishedDate?.toIso8601String(),
        'createdAt': createdAt.toIso8601String(),
        'updatedAt': updatedAt.toIso8601String(),
        'favorite': favorite ? 1 : 0,
        'archived': archived ? 1 : 0,
        'isDraft': isDraft ? 1 : 0,
        'inInbox': inInbox ? 1 : 0,
        'openCount': openCount,
        'lastOpenedAt': lastOpenedAt?.toIso8601String(),
      };

  factory ContentItem.fromMap(Map<String, dynamic> map,
      {List<String> tags = const [], List<String> collectionIds = const []}) {
    return ContentItem(
      id: map['id'] as String,
      type: contentTypeFromString(map['type'] as String),
      platform: sourcePlatformFromString(map['platform'] as String),
      url: map['url'] as String?,
      title: map['title'] as String,
      description: map['description'] as String?,
      body: map['body'] as String?,
      summary: map['summary'] as String?,
      thumbnailUrl: map['thumbnailUrl'] as String?,
      author: map['author'] as String?,
      publishedDate:
          map['publishedDate'] == null ? null : DateTime.parse(map['publishedDate'] as String),
      createdAt: DateTime.parse(map['createdAt'] as String),
      updatedAt: DateTime.parse(map['updatedAt'] as String),
      favorite: (map['favorite'] as int) == 1,
      archived: (map['archived'] as int) == 1,
      isDraft: (map['isDraft'] as int? ?? 0) == 1,
      inInbox: (map['inInbox'] as int? ?? 0) == 1,
      openCount: map['openCount'] as int? ?? 0,
      lastOpenedAt:
          map['lastOpenedAt'] == null ? null : DateTime.parse(map['lastOpenedAt'] as String),
      tags: tags,
      collectionIds: collectionIds,
    );
  }
}
