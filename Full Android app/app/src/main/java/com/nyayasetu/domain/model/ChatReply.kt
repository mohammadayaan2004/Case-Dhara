package com.nyayasetu.domain.model

data class ChatReply(
    val answer: String,
    val retrievedSections: List<String>,
    val retrievalTier: Int,
)
