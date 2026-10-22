package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.TileEntity

@Database(entities = [TileEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LumiaDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao

    companion object {
        @Volatile
        private var INSTANCE: LumiaDatabase? = null

        fun getDatabase(context: Context): LumiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumiaDatabase::class.java,
                    "lumia_launcher_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
