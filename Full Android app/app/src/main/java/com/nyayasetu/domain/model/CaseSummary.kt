package com.nyayasetu.domain.model

data class CaseSummary(
    val caseTitle: String,
    val citation: String?,
    val court: String,
    val bench: String?,
    val date: String?,
    val parties: Map<String, String>,
    val sectionsInvoked: List<SectionReference>,
    val facts: String,
    val issues: List<String>,
    val arguments: Map<String, String>,
    val evidence: String,
    val held: String,
    val ratioDecidendi: String,
    val order: String,
    val summaryShort: String,
    val outcome: String?,
)
