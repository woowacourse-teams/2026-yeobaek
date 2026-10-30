package com.yeobaek.core.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
actual fun rememberSystemNotificationSettings(): SystemNotificationSettingsState {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    fun notificationsEnabled(): Boolean = manager.areNotificationsEnabled() && !manager.isActivityChannelBlocked()

    var enabled by remember(context) { mutableStateOf(notificationsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = notificationsEnabled() }
    return SystemNotificationSettingsState(enabled) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelBlocked = manager.areNotificationsEnabled() && manager.isActivityChannelBlocked()
            if (channelBlocked) {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, ACTIVITY_NOTIFICATION_CHANNEL_ID)
            } else {
                Intent(
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun NotificationManager.isActivityChannelBlocked(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    return getNotificationChannel(ACTIVITY_NOTIFICATION_CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
}
