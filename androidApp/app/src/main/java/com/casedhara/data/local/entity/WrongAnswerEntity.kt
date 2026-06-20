package com.casedhara.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wrong_answers")
data class WrongAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val difficulty: String,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val userAnswerIndex: Int,
    val explanation: String,
    val savedAt: Long = System.currentTimeMillis(),
)
