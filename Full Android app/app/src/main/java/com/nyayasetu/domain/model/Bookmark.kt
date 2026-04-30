package com.nyayasetu.domain.model

data class Bookmark(
    val id: Long = 0,
    val ipcSection: String,
    val bnsSection: String,
    val ipcHeading: String,
    val bnsHeading: String,
    val status: String,
    val savedAt: Long,
)
