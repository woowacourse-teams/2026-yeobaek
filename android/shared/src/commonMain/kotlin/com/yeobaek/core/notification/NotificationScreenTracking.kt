package com.yeobaek.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.feature.group.detail.navigation.Detail
import com.yeobaek.feature.reader.navigation.PublicRoomReader
import com.yeobaek.feature.reader.navigation.Reader

@Composable
internal fun TrackNotificationSpace(
    navController: NavController,
    notifications: CommentNotifications,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(navController, notifications, lifecycle) {
        fun updateActiveSpace() {
            notifications.updateActiveSpace(
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    navController.currentBackStackEntry?.commentSpace()
                } else {
                    null
                },
            )
        }

        val destinationListener = NavController.OnDestinationChangedListener { _, _, _ ->
            updateActiveSpace()
        }
        val lifecycleObserver = LifecycleEventObserver { _, _ -> updateActiveSpace() }
        navController.addOnDestinationChangedListener(destinationListener)
        lifecycle.addObserver(lifecycleObserver)
        updateActiveSpace()

        onDispose {
            navController.removeOnDestinationChangedListener(destinationListener)
            lifecycle.removeObserver(lifecycleObserver)
            notifications.updateActiveSpace(null)
        }
    }
}

private fun NavBackStackEntry.commentSpace(): CommentSpace? = when {
    destination.hasRoute<Reader>() -> CommentSpace.Group(toRoute<Reader>().groupId)
    destination.hasRoute<PublicRoomReader>() -> CommentSpace.PublicRoom(toRoute<PublicRoomReader>().publicRoomId)
    destination.hasRoute<Detail>() -> CommentSpace.Group(toRoute<Detail>().groupId)
    else -> null
}
