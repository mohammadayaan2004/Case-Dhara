package com.casedhara.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_progress")
data class QuizProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val difficulty: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val timeTakenSeconds: Int,
    /** JSON string: list of {question, options, correctIndex, userAnswer, explanation} */
    val questionsJson: String,
    val completedAt: Long = System.currentTimeMillis(),
)
