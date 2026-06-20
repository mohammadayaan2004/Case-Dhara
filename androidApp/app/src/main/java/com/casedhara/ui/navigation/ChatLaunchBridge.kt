package com.casedhara.ui.navigation

/**
 * UI-only bridge to pass launch intent from Home → Chat without new routes.
 */
object ChatLaunchBridge {
    var pendingQuery: String? = null
    var startVoiceOnOpen: Boolean = false

    fun consumeQuery(): String? = pendingQuery.also { pendingQuery = null }

    fun consumeVoice(): Boolean = startVoiceOnOpen.also { startVoiceOnOpen = false }
}
