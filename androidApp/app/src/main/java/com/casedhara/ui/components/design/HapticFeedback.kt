package com.casedhara.ui.components.design

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView

enum class DharaHaptic { Light, Medium, Confirm, Search }

@Composable
fun rememberDharaHaptic(): (DharaHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { type ->
            val constant = when (type) {
                DharaHaptic.Light -> HapticFeedbackConstants.KEYBOARD_TAP
                DharaHaptic.Medium -> HapticFeedbackConstants.CONTEXT_CLICK
                DharaHaptic.Confirm -> HapticFeedbackConstants.CONFIRM
                DharaHaptic.Search -> HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(constant)
        }
    }
}

@Composable
fun performComposeHaptic(type: HapticFeedbackType = HapticFeedbackType.TextHandleMove) {
    val view = LocalView.current
    view.performHapticFeedback(
        when (type) {
            HapticFeedbackType.LongPress -> HapticFeedbackConstants.LONG_PRESS
            else -> HapticFeedbackConstants.KEYBOARD_TAP
        },
    )
}
