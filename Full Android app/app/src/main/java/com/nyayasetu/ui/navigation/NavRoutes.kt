package com.nyayasetu.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val MAPPER = "mapper"
    const val SECTION_DETAIL = "mapper/detail/{recordId}"
    const val COMPARE = "mapper/compare/{recordId}"
    const val SUMMARIZER = "summarizer"
    const val CHAT = "chat"
    const val BOOKMARKS = "bookmarks"

    fun sectionDetail(recordId: Int) = "mapper/detail/$recordId"
    fun compare(recordId: Int) = "mapper/compare/$recordId"
}
