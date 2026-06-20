package com.casedhara.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MappingRecordDto(
    @SerializedName("id") val id: Int,
    @SerializedName("ipc_section") val ipcSection: String,
    @SerializedName("ipc_heading") val ipcHeading: String,
    @SerializedName("ipc_description") val ipcDescription: String,
    @SerializedName("bns_section") val bnsSection: String,
    @SerializedName("bns_heading") val bnsHeading: String,
    @SerializedName("bns_description") val bnsDescription: String,
    @SerializedName("status") val status: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("retrieval_tier") val retrievalTier: Int,
)
