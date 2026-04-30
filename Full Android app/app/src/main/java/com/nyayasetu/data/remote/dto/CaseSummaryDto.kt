package com.nyayasetu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SectionReferenceDto(
    @SerializedName("raw_ref") val rawRef: String,
    @SerializedName("ipc_section") val ipcSection: String,
    @SerializedName("ipc_heading") val ipcHeading: String,
    @SerializedName("bns_section") val bnsSection: String,
    @SerializedName("bns_heading") val bnsHeading: String,
    @SerializedName("status") val status: String,
)

data class CaseSummaryDto(
    @SerializedName("case_title") val caseTitle: String,
    @SerializedName("citation") val citation: String?,
    @SerializedName("court") val court: String,
    @SerializedName("bench") val bench: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("parties") val parties: Map<String, String>,
    @SerializedName("sections_invoked") val sectionsInvoked: List<SectionReferenceDto>,
    @SerializedName("facts") val facts: String,
    @SerializedName("issues") val issues: List<String>,
    @SerializedName("arguments") val arguments: Map<String, String>,
    @SerializedName("evidence") val evidence: String,
    @SerializedName("held") val held: String,
    @SerializedName("ratio_decidendi") val ratioDecidendi: String,
    @SerializedName("order") val order: String,
    @SerializedName("summary_short") val summaryShort: String,
    @SerializedName("outcome") val outcome: String?,
)

data class SummarizeTextRequestDto(
    @SerializedName("text") val text: String,
)
