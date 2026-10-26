package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.yeobaek.core.notification.CommentNotifications
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.feature.group.detail.navigation.Detail
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.reader.navigation.PublicRoomReader
import com.yeobaek.feature.reader.navigation.Reader
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

@Composable
internal fun NotificationEffects(
    navController: NavHostController,
    notifications: CommentNotifications,
    userId: StateFlow<Int?>,
) {
    TrackNotificationSpace(navController, notifications)
    HandleNotificationOpen(navController, notifications, userId)
}

@Composable
private fun TrackNotificationSpace(
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

@Composable
private fun HandleNotificationOpen(
    navController: NavHostController,
    notifications: CommentNotifications,
    userId: StateFlow<Int?>,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(notifications, userId, navController, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            combine(
                notifications.pendingOpen,
                userId,
                navController.currentBackStackEntryFlow,
            ) { request, memberId, _ -> request.takeIf { memberId != null } }
                .collect { request ->
                    if (request == null || notifications.pendingOpen.value !== request) {
                        return@collect
                    }
                    val target = request.notification
                    val hasHome = try {
                        navController.getBackStackEntry<Home>()
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                    if (!hasHome) {
                        navController.navigate(Home) {
                            popUpTo(navController.graph.id)
                            launchSingleTop = true
                        }
                    }
                    when (val space = target.space) {
                        is CommentSpace.Group -> navController.navigate(
                            Reader(space.groupId, target.passageSequence, target.sentenceId),
                        )

                        is CommentSpace.PublicRoom -> navController.navigate(
                            PublicRoomReader(space.publicRoomId, target.passageSequence, target.sentenceId),
                        )
                    }
                    notifications.consumeOpen(request)
                }
        }
    }
}

private fun NavBackStackEntry.commentSpace(): CommentSpace? = when {
    destination.hasRoute<Reader>() -> CommentSpace.Group(toRoute<Reader>().groupId)
    destination.hasRoute<PublicRoomReader>() -> CommentSpace.PublicRoom(toRoute<PublicRoomReader>().publicRoomId)
    destination.hasRoute<Detail>() -> CommentSpace.Group(toRoute<Detail>().groupId)
    else -> null
}
