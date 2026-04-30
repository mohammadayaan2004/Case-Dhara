package com.nyayasetu.data.remote.dto

import com.nyayasetu.domain.model.CaseSummary
import com.nyayasetu.domain.model.ChatMessage
import com.nyayasetu.domain.model.MappingRecord
import com.nyayasetu.domain.model.MapperResponse
import com.nyayasetu.domain.model.SectionReference

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

fun CaseSummaryDto.toDomain() = CaseSummary(
    caseTitle = caseTitle,
    citation = citation,
    court = court,
    bench = bench,
    date = date,
    parties = parties,
    sectionsInvoked = sectionsInvoked.map { it.toDomain() },
    facts = facts,
    issues = issues,
    arguments = arguments,
    evidence = evidence,
    held = held,
    ratioDecidendi = ratioDecidendi,
    order = order,
    summaryShort = summaryShort,
    outcome = outcome,
)

fun ChatMessage.toDto() = ChatMessageDto(role = role, content = content)
