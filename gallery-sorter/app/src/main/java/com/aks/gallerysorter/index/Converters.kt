package com.aks.gallerysorter.index

import androidx.room.TypeConverter
import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.MediaKind
import com.aks.gallerysorter.core.ScanStatus
import com.aks.gallerysorter.core.VaultTier

/** Enums are persisted by name so reordering them never corrupts existing rows. */
class Converters {
    @TypeConverter fun categoryToString(v: Category): String = v.name
    @TypeConverter fun stringToCategory(v: String): Category = Category.valueOf(v)
    @TypeConverter fun kindToString(v: MediaKind): String = v.name
    @TypeConverter fun stringToKind(v: String): MediaKind = MediaKind.valueOf(v)
    @TypeConverter fun statusToString(v: ScanStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): ScanStatus = ScanStatus.valueOf(v)
    @TypeConverter fun tierToString(v: VaultTier): String = v.name
    @TypeConverter fun stringToTier(v: String): VaultTier = VaultTier.valueOf(v)
}
