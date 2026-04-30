package com.nyayasetu.domain.model

data class MapperResponse(
    val query: String,
    val results: List<MappingRecord>,
    val total: Int,
    val retrievalTier: Int,
    val message: String,
)
