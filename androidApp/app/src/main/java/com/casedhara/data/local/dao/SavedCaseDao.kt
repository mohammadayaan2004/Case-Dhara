package com.casedhara.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.casedhara.data.local.entity.SavedCaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCaseDao {
    @Query("SELECT * FROM saved_cases ORDER BY savedAt DESC")
    fun getAllFlow(): Flow<List<SavedCaseEntity>>

    @Query("SELECT * FROM saved_cases WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavedCaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedCaseEntity)

    @Delete
    suspend fun delete(entity: SavedCaseEntity)

    @Query("DELETE FROM saved_cases")
    suspend fun deleteAll()
}
