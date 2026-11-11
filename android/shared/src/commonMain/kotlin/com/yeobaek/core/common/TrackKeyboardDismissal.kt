package com.yeobaek.core.common

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.InputKeyboardDismissed
import com.yeobaek.core.analytics.KeyboardDismissTrigger
import com.yeobaek.core.analytics.KeyboardField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberKeyboardDismissal(
    field: KeyboardField?,
    trackEvent: (AnalyticsEvent) -> Unit,
): (KeyboardDismissTrigger) -> Unit {
    val visible by rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current) > 0)
    val track by rememberUpdatedState(trackEvent)
    var pending by remember(field) { mutableStateOf<KeyboardDismissTrigger?>(null) }
    LaunchedEffect(field, visible, pending) {
        val trigger = pending ?: return@LaunchedEffect
        if (!visible && field != null) {
            pending = null
            track(InputKeyboardDismissed(field, trigger))
        }
    }
    return remember(field) {
        { trigger ->
            if (field != null && visible && pending == null) pending = trigger
        }
    }
}
