package com.casedhara.data.local.dao

import androidx.room.*
import com.casedhara.data.local.entity.WrongAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrongAnswerDao {
    @Query("SELECT * FROM wrong_answers ORDER BY savedAt DESC")
    fun getAllFlow(): Flow<List<WrongAnswerEntity>>

    @Query("SELECT * FROM wrong_answers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WrongAnswerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WrongAnswerEntity)

    @Delete
    suspend fun delete(entity: WrongAnswerEntity)

    @Query("DELETE FROM wrong_answers")
    suspend fun deleteAll()
}
