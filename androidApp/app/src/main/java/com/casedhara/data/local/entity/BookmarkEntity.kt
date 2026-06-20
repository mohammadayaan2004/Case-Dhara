package com.casedhara.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ipcSection: String,
    val bnsSection: String,
    val ipcHeading: String,
    val bnsHeading: String,
    val status: String,
    val savedAt: Long,
)
