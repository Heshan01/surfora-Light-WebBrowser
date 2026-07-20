package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BrowserDao
import com.example.data.entity.Bookmark
import com.example.data.entity.HistoryEntry

@Database(entities = [Bookmark::class, HistoryEntry::class], version = 1, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "surfora_browser_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
