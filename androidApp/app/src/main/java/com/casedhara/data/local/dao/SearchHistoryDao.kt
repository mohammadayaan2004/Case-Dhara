package com.casedhara.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.casedhara.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: SearchHistoryEntity): Long

    @Update
    suspend fun update(entry: SearchHistoryEntity)

    @Query("SELECT * FROM search_history WHERE query = :query LIMIT 1")
    suspend fun findByQuery(query: String): SearchHistoryEntity?

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun observeRecent(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
