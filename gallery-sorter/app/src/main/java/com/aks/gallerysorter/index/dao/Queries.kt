package com.aks.gallerysorter.index.dao

/** The `:hidden` binding for [LOCK_FILTER]. */
internal val HIDDEN_NAMES: List<String> get() = LabelDao.LOCKED_HIDDEN.map { it.name }

internal fun Boolean.bit(): Int = if (this) 1 else 0

/**
 * Shared SQL fragments over `media m`. [IN_CATEGORY] binds `:category` and honours overrides
 * (excluded = removed, not excluded = forced in). [LOCK_FILTER] binds `:includeLocked` (0/1) and
 * `:hidden` (LabelDao.LOCKED_HIDDEN names): while locked it drops items carrying any of those
 * labels (unless overridden out) or forced into one of them.
 */
internal const val IN_CATEGORY = """
    ((EXISTS (SELECT 1 FROM labels l WHERE l.mediaId = m.id AND l.category = :category)
      AND NOT EXISTS (SELECT 1 FROM overrides o
                      WHERE o.mediaId = m.id AND o.category = :category AND o.isExcluded = 1))
     OR EXISTS (SELECT 1 FROM overrides o
                WHERE o.mediaId = m.id AND o.category = :category AND o.isExcluded = 0))
"""

internal const val LOCK_FILTER = """
    (:includeLocked = 1 OR (
        NOT EXISTS (SELECT 1 FROM labels l
                    WHERE l.mediaId = m.id
                      AND l.category IN (:hidden)
                      AND NOT EXISTS (SELECT 1 FROM overrides o
                                      WHERE o.mediaId = l.mediaId AND o.category = l.category AND o.isExcluded = 1))
        AND NOT EXISTS (SELECT 1 FROM overrides o
                        WHERE o.mediaId = m.id AND o.isExcluded = 0 AND o.category IN (:hidden))
    ))
"""
