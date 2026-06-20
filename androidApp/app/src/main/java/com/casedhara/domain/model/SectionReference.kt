package com.casedhara.domain.model

data class SectionReference(
    val rawRef: String,
    val ipcSection: String,
    val ipcHeading: String,
    val bnsSection: String,
    val bnsHeading: String,
    val status: String,
)

/** Chip / PDF label for IPC → BNS mapping. */
fun SectionReference.mappingLabel(): String = when {
    ipcSection.isNotEmpty() && bnsSection.isNotEmpty() ->
        "IPC $ipcSection → BNS $bnsSection"
    ipcSection.isNotEmpty() -> "IPC $ipcSection"
    bnsSection.isNotEmpty() -> "BNS $bnsSection"
    rawRef.isNotBlank() -> rawRef
    else -> "Section"
}

/** Query string for Section Mapper search. */
fun SectionReference.mapperQuery(): String = when {
    ipcSection.isNotEmpty() -> "IPC $ipcSection"
    bnsSection.isNotEmpty() -> "BNS $bnsSection"
    else -> rawRef
}
