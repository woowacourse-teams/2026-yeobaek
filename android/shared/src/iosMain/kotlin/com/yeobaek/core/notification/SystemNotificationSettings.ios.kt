package com.yeobaek.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberSystemNotificationSettings(): SystemNotificationSettingsState {
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            scope.launch {
                enabled = status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral
            }
        }
    }
    return SystemNotificationSettingsState(enabled) {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { }
        }
    }
}
