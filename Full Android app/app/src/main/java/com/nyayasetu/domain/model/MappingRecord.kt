package com.nyayasetu.domain.model

data class MappingRecord(
    val id: Int,
    val ipcSection: String,
    val ipcHeading: String,
    val ipcDescription: String,
    val bnsSection: String,
    val bnsHeading: String,
    val bnsDescription: String,
    val status: String,
    val confidence: Float,
    val retrievalTier: Int,
)
