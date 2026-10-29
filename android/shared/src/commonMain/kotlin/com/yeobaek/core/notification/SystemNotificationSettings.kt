package com.yeobaek.core.notification

import androidx.compose.runtime.Composable

const val ACTIVITY_NOTIFICATION_CHANNEL_ID = "yeobaek_activity"

data class SystemNotificationSettingsState(
    val enabled: Boolean?,
    val openSettings: () -> Unit,
)

@Composable
expect fun rememberSystemNotificationSettings(): SystemNotificationSettingsState
