package com.aks.gallerysorter.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aks.gallerysorter.index.dao.AlbumDao
import com.aks.gallerysorter.index.dao.DupeDao
import com.aks.gallerysorter.index.dao.LabelDao
import com.aks.gallerysorter.index.dao.MediaDao
import com.aks.gallerysorter.index.dao.PeopleDao
import com.aks.gallerysorter.index.dao.SettingsDao
import com.aks.gallerysorter.index.dao.VaultDao
import com.aks.gallerysorter.index.entities.AlbumItemRow
import com.aks.gallerysorter.index.entities.AlbumRow
import com.aks.gallerysorter.index.entities.DupeRow
import com.aks.gallerysorter.index.entities.FaceRow
import com.aks.gallerysorter.index.entities.LabelRow
import com.aks.gallerysorter.index.entities.MediaRow
import com.aks.gallerysorter.index.entities.OverrideRow
import com.aks.gallerysorter.index.entities.PersonRow
import com.aks.gallerysorter.index.entities.SettingRow
import com.aks.gallerysorter.index.entities.VaultRow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        MediaRow::class, LabelRow::class, FaceRow::class, PersonRow::class,
        OverrideRow::class, VaultRow::class, DupeRow::class, SettingRow::class,
        AlbumRow::class, AlbumItemRow::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun labelDao(): LabelDao
    abstract fun peopleDao(): PeopleDao
    abstract fun vaultDao(): VaultDao
    abstract fun dupeDao(): DupeDao
    abstract fun settingsDao(): SettingsDao
    abstract fun albumDao(): AlbumDao

    companion object {
        const val FILE_NAME = "index.db"

        /** Opens the SQLCipher-encrypted index. Tests use Room.inMemoryDatabaseBuilder instead. */
        fun open(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(DbKeyProvider(context).passphrase())
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, FILE_NAME)
                .openHelperFactory(factory)
                .build()
        }
    }
}
