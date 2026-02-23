package com.arman.rasald.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arman.rasald.data.dao.DownloadDao
import com.arman.rasald.data.entity.DownloadItem
import com.arman.rasald.utils.Converters

/**
 * Room Database for RAS ALD Application
 * Version: 2
 */
@Database(
    entities = [DownloadItem::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ras_ald_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns for version 2.0 features
                database.execSQL(
                    """
                    ALTER TABLE downloads 
                    ADD COLUMN batchId TEXT DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    ALTER TABLE downloads 
                    ADD COLUMN batchPosition INTEGER DEFAULT 0 NOT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    ALTER TABLE downloads 
                    ADD COLUMN startTime INTEGER DEFAULT NULL
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    ALTER TABLE downloads 
                    ADD COLUMN endTime INTEGER DEFAULT NULL
                    """.trimIndent()
                )
            }
        }
    }
}
