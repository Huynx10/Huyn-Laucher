package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.TileEntity

@Database(entities = [TileEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LumiaDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao

    companion object {
        @Volatile
        private var INSTANCE: LumiaDatabase? = null

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateTilesTableColumnsSafely(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateTilesTableColumnsSafely(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateTilesTableColumnsSafely(db)
            }
        }

        fun migrateTilesTableColumnsSafely(db: SupportSQLiteDatabase) {
            val existingColumns = mutableSetOf<String>()
            db.query("PRAGMA table_info(`tiles`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        existingColumns.add(cursor.getString(nameIndex))
                    }
                }
            }

            fun addColumnIfMissing(columnName: String, sqlDefinition: String) {
                if (columnName !in existingColumns) {
                    db.execSQL("ALTER TABLE `tiles` ADD COLUMN `$columnName` $sqlDefinition")
                    existingColumns.add(columnName)
                }
            }

            addColumnIfMissing("pageId", "TEXT NOT NULL DEFAULT 'page_home_1'")
            addColumnIfMissing("gridX", "INTEGER NOT NULL DEFAULT -1")
            addColumnIfMissing("gridY", "INTEGER NOT NULL DEFAULT -1")
            addColumnIfMissing("colorOverrideHex", "TEXT")
            addColumnIfMissing("customIconName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("notificationCount", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("isLiveEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("appWidgetId", "INTEGER NOT NULL DEFAULT -1")
            addColumnIfMissing("widgetProvider", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("widgetMinSpanX", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("widgetMinSpanY", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("widgetResizeMode", "INTEGER NOT NULL DEFAULT 3")
        }

        fun getDatabase(context: Context): LumiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumiaDatabase::class.java,
                    "lumia_launcher_db"
                )
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
