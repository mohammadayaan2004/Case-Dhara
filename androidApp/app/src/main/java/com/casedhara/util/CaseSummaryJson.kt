package com.casedhara.util

import com.casedhara.domain.model.CaseSummary
import com.google.gson.Gson

object CaseSummaryJson {
    private val gson = Gson()

    fun toJson(summary: CaseSummary): String = gson.toJson(summary)

    fun fromJson(json: String): CaseSummary? = runCatching {
        gson.fromJson(json, CaseSummary::class.java)
    }.getOrNull()
}
