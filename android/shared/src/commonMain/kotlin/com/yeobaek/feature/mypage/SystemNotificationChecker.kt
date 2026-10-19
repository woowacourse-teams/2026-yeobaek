package com.yeobaek.feature.mypage

import androidx.compose.runtime.Composable

internal interface SystemNotificationChecker {
    suspend fun isSystemNotificationEnabled(): Boolean
}

@Composable
internal expect fun systemNotificationChecker(): SystemNotificationChecker
