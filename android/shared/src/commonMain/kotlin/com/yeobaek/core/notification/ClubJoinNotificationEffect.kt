package com.yeobaek.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.filter

@Composable
fun ClubJoinNotificationEffect(notifications: AppNotifications, groupId: Long, onJoined: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnJoined by rememberUpdatedState(onJoined)
    LaunchedEffect(groupId, lifecycleOwner, notifications) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            notifications.clubJoinEvents.filter { it.groupId == groupId }.collect { currentOnJoined() }
        }
    }
}
