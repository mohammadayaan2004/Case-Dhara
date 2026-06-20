package com.casedhara.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cases")
data class SavedCaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val source: String = "case_summarizer",
    val savedAt: Long = System.currentTimeMillis(),
    val summaryJson: String,
)
