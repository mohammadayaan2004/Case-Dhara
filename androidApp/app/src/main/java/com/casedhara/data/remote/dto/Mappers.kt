package com.casedhara.data.remote.dto

import com.casedhara.domain.model.CaseSummary
import com.casedhara.domain.model.ChatMessage
import com.casedhara.domain.model.MappingRecord
import com.casedhara.domain.model.MapperResponse
import com.casedhara.domain.model.SectionReference

fun MappingRecordDto.toDomain() = MappingRecord(
    id = id,
    ipcSection = ipcSection,
    ipcHeading = ipcHeading,
    ipcDescription = ipcDescription,
    bnsSection = bnsSection,
    bnsHeading = bnsHeading,
    bnsDescription = bnsDescription,
    status = status,
    confidence = confidence,
    retrievalTier = retrievalTier,
)

fun MapperResponseDto.toDomain() = MapperResponse(
    query = query,
    results = results.map { it.toDomain() },
    total = total,
    retrievalTier = retrievalTier,
    message = message,
)

fun SectionReferenceDto.toDomain() = SectionReference(
    rawRef = rawRef,
    ipcSection = ipcSection,
    ipcHeading = ipcHeading,
    bnsSection = bnsSection,
    bnsHeading = bnsHeading,
    status = status,
)

fun CaseSummaryDto.toDomain(): CaseSummary {
    // Resolve fields that have both new and legacy API keys
    val resolvedFacts       = factsOfCase?.takeIf { it.isNotBlank() } ?: facts.orEmpty()
    val resolvedIssues      = legalIssues?.takeIf { it.isNotEmpty() } ?: issues.orEmpty()
    val resolvedPetArgs     = petitionerArguments?.takeIf { it.isNotBlank() }
        ?: arguments?.get("prosecution").orEmpty()
    val resolvedRespArgs    = respondentArguments?.takeIf { it.isNotBlank() }
        ?: arguments?.get("defense").orEmpty()
    val resolvedJudge       = judge?.takeIf { it.isNotBlank() } ?: bench
    val resolvedPetitioner  = petitioner?.takeIf { it.isNotBlank() }
        ?: parties?.get("appellant")
    val resolvedRespondent  = respondent?.takeIf { it.isNotBlank() }
        ?: parties?.get("respondent")
    val resolvedSummary     = summary?.takeIf { it.isNotBlank() } ?: summaryShort.orEmpty()
    val resolvedFinalJudgment = finalJudgment?.takeIf { it.isNotBlank() } ?: order.orEmpty()

    return CaseSummary(
        caseId              = caseId,
        caseTitle           = caseTitle,
        court               = court,
        judge               = resolvedJudge,
        date                = date,
        citation            = citation,
        petitioner          = resolvedPetitioner,
        respondent          = resolvedRespondent,
        caseType            = caseType,
        sectionsInvoked     = sectionsInvoked.map { it.toDomain() },
        legalProvisions     = legalProvisions.orEmpty(),
        precedentsCited     = precedentsCited.orEmpty(),
        factsOfCase         = resolvedFacts,
        legalIssues         = resolvedIssues,
        petitionerArguments = resolvedPetArgs,
        respondentArguments = resolvedRespArgs,
        courtObservations   = courtObservations.orEmpty(),
        evidence            = evidence.orEmpty(),
        held                = held.orEmpty(),
        ratioDecidendi      = ratioDecidendi.orEmpty(),
        finalJudgment       = resolvedFinalJudgment,
        outcome             = outcome,
        summary             = resolvedSummary,
    )
}

fun ChatMessage.toDto() = ChatMessageDto(role = role, content = content)