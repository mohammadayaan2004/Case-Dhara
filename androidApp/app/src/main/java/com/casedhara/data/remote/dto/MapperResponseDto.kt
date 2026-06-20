package com.casedhara.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MapperResponseDto(
    @SerializedName("query") val query: String,
    @SerializedName("results") val results: List<MappingRecordDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("retrieval_tier") val retrievalTier: Int,
    @SerializedName("message") val message: String,
)

data class SearchRequestDto(
    @SerializedName("query") val query: String,
    @SerializedName("top_k") val topK: Int = 5,
    @SerializedName("law_filter") val lawFilter: String? = null,
)

data class BatchSearchRequestDto(
    @SerializedName("queries") val queries: List<String>,
    @SerializedName("top_k") val topK: Int = 1,
    @SerializedName("law_filter") val lawFilter: String? = null,
)
