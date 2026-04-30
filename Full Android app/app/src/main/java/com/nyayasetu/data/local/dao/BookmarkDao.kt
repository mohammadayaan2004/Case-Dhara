package com.nyayasetu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.nyayasetu.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(entity: BookmarkEntity): Long

    @Delete
    suspend fun delete(entity: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE ipcSection = :ipc AND bnsSection = :bns LIMIT 1")
    suspend fun findBySections(ipc: String, bns: String): BookmarkEntity?
}
