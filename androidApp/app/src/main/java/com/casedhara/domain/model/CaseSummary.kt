package com.casedhara.domain.model

data class CaseSummary(
    // ── Identifiers ──────────────────────────────────────────────────────────
    val caseId: String?,
    val caseTitle: String,
    val court: String,
    val judge: String?,
    val date: String?,
    val citation: String?,

    // ── Parties ───────────────────────────────────────────────────────────────
    val petitioner: String?,
    val respondent: String?,

    // ── Classification ────────────────────────────────────────────────────────
    val caseType: String?,

    // ── Sections ──────────────────────────────────────────────────────────────
    val sectionsInvoked: List<SectionReference>,
    val legalProvisions: List<String>,
    val precedentsCited: List<String>,

    // ── Substance ─────────────────────────────────────────────────────────────
    val factsOfCase: String,
    val legalIssues: List<String>,

    // ── Arguments ─────────────────────────────────────────────────────────────
    val petitionerArguments: String,
    val respondentArguments: String,

    // ── Court reasoning ───────────────────────────────────────────────────────
    val courtObservations: String,
    val evidence: String,
    val held: String,
    val ratioDecidendi: String,

    // ── Outcome ───────────────────────────────────────────────────────────────
    val finalJudgment: String,
    val outcome: String?,

    // ── Summary ───────────────────────────────────────────────────────────────
    val summary: String,
)

enum class SummaryFieldType { TEXT, LIST }

data class SummaryDisplayField(
    val label: String,
    val value: String,
    val type: SummaryFieldType = SummaryFieldType.TEXT,
)

/**
 * Converts [CaseSummary] into a flat list of display-ready fields following
 * the canonical Case Dhara schema. Null, blank, and empty-list fields are
 * filtered out — zero empty rows guaranteed.
 */
fun CaseSummary.toDisplayFields(): List<SummaryDisplayField> {
    val fields = mutableListOf<SummaryDisplayField>()

    fun String?.addText(label: String) {
        if (!isNullOrBlank()) fields += SummaryDisplayField(label, this!!)
    }
    fun List<String>?.addList(label: String) {
        if (!isNullOrEmpty()) fields += SummaryDisplayField(label, joinToString("\n"), SummaryFieldType.LIST)
    }

    caseId.addText("Case ID")
    caseTitle.addText("Case Title")
    court.addText("Court")
    judge.addText("Judge")
    date.addText("Date")
    citation.addText("Citation")
    petitioner.addText("Petitioner")
    respondent.addText("Respondent")
    caseType.addText("Case Type")

    factsOfCase.addText("Facts of Case")
    legalIssues.addList("Legal Issues")
    petitionerArguments.addText("Petitioner Arguments")
    respondentArguments.addText("Respondent Arguments")
    courtObservations.addText("Court Observations")

    if (legalProvisions.isNotEmpty()) legalProvisions.addList("Legal Provisions")
    if (precedentsCited.isNotEmpty()) precedentsCited.addList("Precedents Cited")

    evidence.addText("Evidence")
    held.addText("Held")
    ratioDecidendi.addText("Ratio Decidendi")
    finalJudgment.addText("Final Judgment")
    outcome.addText("Outcome")
    summary.addText("Summary")

    return fields
}