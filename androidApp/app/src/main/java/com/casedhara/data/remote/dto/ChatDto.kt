package com.casedhara.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
)

data class ChatRequestDto(
    @SerializedName("question") val question: String,
    @SerializedName("history") val history: List<ChatMessageDto>,
)

data class ChatResponseDto(
    @SerializedName("answer") val answer: String,
    @SerializedName("retrieved_sections") val retrievedSections: List<String>,
    @SerializedName("retrieval_tier") val retrievalTier: Int,
)
