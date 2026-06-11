package com.prettycountdown.data.model

import kotlinx.serialization.Serializable

/**
 * Everything needed to restore a user's data on another device. Pretty Countdown
 * is local-first with no account, so this JSON export is the only way to move
 * or protect data.
 */
@Serializable
data class BackupData(
    val events: List<Event>,
    val checklistItems: List<ChecklistItem>,
    val collections: List<EventCollection>,
    val collectionMemberships: List<EventCollectionCrossRef>,
    val version: Int = 1,
)
