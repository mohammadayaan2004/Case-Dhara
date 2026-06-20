package com.casedhara.data.local.dao

import androidx.room.*
import com.casedhara.data.local.entity.QuizProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizProgressDao {
    @Query("SELECT * FROM quiz_progress ORDER BY completedAt DESC")
    fun getAllFlow(): Flow<List<QuizProgressEntity>>

    @Query("SELECT * FROM quiz_progress WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QuizProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QuizProgressEntity)

    @Delete
    suspend fun delete(entity: QuizProgressEntity)

    @Query("DELETE FROM quiz_progress")
    suspend fun deleteAll()
}
