package com.nyayasetu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nyayasetu.data.local.dao.BookmarkDao
import com.nyayasetu.data.local.dao.SearchHistoryDao
import com.nyayasetu.data.local.entity.BookmarkEntity
import com.nyayasetu.data.local.entity.SearchHistoryEntity

@Database(
    entities = [SearchHistoryEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
