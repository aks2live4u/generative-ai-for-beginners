package com.aks.gallerysorter.index.entities

import androidx.room.Entity

@Entity(tableName = "dupes", primaryKeys = ["groupId", "mediaId"])
data class DupeRow(val groupId: Long, val mediaId: Long, val suggestedKeep: Boolean)
