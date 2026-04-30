package com.nyayasetu.domain.model

data class SectionReference(
    val rawRef: String,
    val ipcSection: String,
    val ipcHeading: String,
    val bnsSection: String,
    val bnsHeading: String,
    val status: String,
)
