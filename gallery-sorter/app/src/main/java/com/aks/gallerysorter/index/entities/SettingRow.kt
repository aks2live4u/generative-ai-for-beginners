package com.aks.gallerysorter.index.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingRow(@PrimaryKey val key: String, val value: String)
