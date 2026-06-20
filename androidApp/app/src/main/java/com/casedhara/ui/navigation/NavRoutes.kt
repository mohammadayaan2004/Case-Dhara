package com.casedhara.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val MAPPER = "mapper"
    const val SECTION_DETAIL = "mapper/detail/{recordId}"
    const val COMPARE = "mapper/compare/{recordId}"
    const val SUMMARIZER = "summarizer"
    const val CHAT = "chat"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"
    const val PROFILE = "settings/profile"
    const val WRONG_ANSWERS = "settings/wrong-answers"
    const val QUIZ_PROGRESS = "settings/quiz-progress"
    const val SAVED_CASE_SUMMARIES = "settings/saved-case-summaries"
    const val CASE_SUMMARY_DETAIL = "case_summary_detail"
    const val WRONG_ANSWER_DETAIL = "wrong_answer_detail"
    const val QUIZ_PROGRESS_DETAIL = "quiz_progress_detail"
    const val ABOUT = "about"
    const val LOGIN = "auth/login"
    const val SIGNUP = "auth/signup"
    const val QUIZ = "quiz"

    fun mapperWithQuery(query: String, openDetail: Boolean = false): String {
        val encoded = android.net.Uri.encode(query)
        return "$MAPPER?query=$encoded&openDetail=$openDetail"
    }

    fun sectionDetail(recordId: Int) = "mapper/detail/$recordId"
    fun compare(recordId: Int) = "mapper/compare/$recordId"
    fun caseSummaryDetail(id: String) = "$CASE_SUMMARY_DETAIL/$id"
    fun wrongAnswerDetail(id: String) = "$WRONG_ANSWER_DETAIL/$id"
    fun quizProgressDetail(id: String) = "$QUIZ_PROGRESS_DETAIL/$id"
}
