package com.yeobaek.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalAppPromptBlocked = compositionLocalOf { false }

@Composable
fun AppPromptBlocker(
    blocked: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppPromptBlocked provides (LocalAppPromptBlocked.current || blocked),
        content = content,
    )
}
