class Collection {
  final String id;
  final String name;
  final String? description;
  final String? coverImage;
  final DateTime createdAt;
  final bool pinned;
  final bool favorite;
  final int itemCount;

  const Collection({
    required this.id,
    required this.name,
    required this.createdAt,
    this.description,
    this.coverImage,
    this.pinned = false,
    this.favorite = false,
    this.itemCount = 0,
  });

  Collection copyWith({
    String? name,
    String? description,
    String? coverImage,
    bool? pinned,
    bool? favorite,
    int? itemCount,
  }) {
    return Collection(
      id: id,
      name: name ?? this.name,
      description: description ?? this.description,
      coverImage: coverImage ?? this.coverImage,
      createdAt: createdAt,
      pinned: pinned ?? this.pinned,
      favorite: favorite ?? this.favorite,
      itemCount: itemCount ?? this.itemCount,
    );
  }

  Map<String, dynamic> toMap() => {
        'id': id,
        'name': name,
        'description': description,
        'coverImage': coverImage,
        'createdAt': createdAt.toIso8601String(),
        'pinned': pinned ? 1 : 0,
        'favorite': favorite ? 1 : 0,
      };

  factory Collection.fromMap(Map<String, dynamic> map, {int itemCount = 0}) {
    return Collection(
      id: map['id'] as String,
      name: map['name'] as String,
      description: map['description'] as String?,
      coverImage: map['coverImage'] as String?,
      createdAt: DateTime.parse(map['createdAt'] as String),
      pinned: (map['pinned'] as int? ?? 0) == 1,
      favorite: (map['favorite'] as int? ?? 0) == 1,
      itemCount: itemCount,
    );
  }
}
