package com.yeobaek.feature.mypage

import android.app.NotificationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun systemNotificationChecker(): SystemNotificationChecker {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidSystemNotificationChecker(context.getSystemService(NotificationManager::class.java))
    }
}

private class AndroidSystemNotificationChecker(
    private val notificationManager: NotificationManager,
) : SystemNotificationChecker {
    override suspend fun isSystemNotificationEnabled(): Boolean = notificationManager.areNotificationsEnabled()
}
