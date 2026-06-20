package com.casedhara.data.remote.dto

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
    // ── Identifiers ──────────────────────────────────────────────────────────
    @SerializedName("case_id")          val caseId: String?,
    @SerializedName("case_title")       val caseTitle: String,
    @SerializedName("court")            val court: String,
    @SerializedName("judge")            val judge: String?,
    @SerializedName("date")             val date: String?,
    @SerializedName("citation")         val citation: String?,

    // ── Parties ───────────────────────────────────────────────────────────────
    @SerializedName("petitioner")       val petitioner: String?,
    @SerializedName("respondent")       val respondent: String?,

    // ── Classification ────────────────────────────────────────────────────────
    @SerializedName("case_type")        val caseType: String?,

    // ── Legacy parties map (kept for backward compat) ─────────────────────────
    @SerializedName("parties")          val parties: Map<String, String>?,

    // ── Bench (kept for backward compat) ──────────────────────────────────────
    @SerializedName("bench")            val bench: String?,

    // ── Sections ──────────────────────────────────────────────────────────────
    @SerializedName("sections_invoked") val sectionsInvoked: List<SectionReferenceDto>,
    @SerializedName("legal_provisions") val legalProvisions: List<String>?,
    @SerializedName("precedents_cited") val precedentsCited: List<String>?,

    // ── Substance ─────────────────────────────────────────────────────────────
    @SerializedName("facts_of_case")    val factsOfCase: String?,
    @SerializedName("facts")            val facts: String?,          // legacy key
    @SerializedName("legal_issues")     val legalIssues: List<String>?,
    @SerializedName("issues")           val issues: List<String>?,   // legacy key

    // ── Arguments ─────────────────────────────────────────────────────────────
    @SerializedName("petitioner_arguments") val petitionerArguments: String?,
    @SerializedName("respondent_arguments") val respondentArguments: String?,
    @SerializedName("arguments")            val arguments: Map<String, String>?,  // legacy

    // ── Court reasoning ───────────────────────────────────────────────────────
    @SerializedName("court_observations") val courtObservations: String?,
    @SerializedName("evidence")           val evidence: String?,
    @SerializedName("held")               val held: String?,
    @SerializedName("ratio_decidendi")    val ratioDecidendi: String?,

    // ── Outcome ───────────────────────────────────────────────────────────────
    @SerializedName("final_judgment")  val finalJudgment: String?,
    @SerializedName("order")           val order: String?,
    @SerializedName("outcome")         val outcome: String?,

    // ── Summary ───────────────────────────────────────────────────────────────
    @SerializedName("summary")         val summary: String?,
    @SerializedName("summary_short")   val summaryShort: String?,    // legacy key
)

data class SummarizeTextRequestDto(
    @SerializedName("text") val text: String,
)