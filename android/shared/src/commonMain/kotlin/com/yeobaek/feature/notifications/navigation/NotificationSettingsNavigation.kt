package com.yeobaek.feature.notifications.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.notification.rememberSystemNotificationSettings
import com.yeobaek.feature.notifications.NotificationSettingsScreen
import com.yeobaek.feature.notifications.NotificationSettingsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object NotificationSettings

fun NavController.navigateToNotificationSettings() {
    navigate(NotificationSettings) { launchSingleTop = true }
}

fun NavGraphBuilder.notificationSettingsNavGraph(appContainer: AppContainer, navigateBack: () -> Unit) {
    composable<NotificationSettings> {
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.NOTIFICATION_SETTINGS,
        )
        val viewModel: NotificationSettingsViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    NotificationSettingsViewModel(
                        appContainer.notificationRepository,
                        appContainer.userPreferences.userId,
                        appContainer.notificationInstallationId,
                        trackEvent = { appContainer.analyticsTracker.track(it) },
                    )
                }
            },
        )
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val systemSettings = rememberSystemNotificationSettings()
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenResumed() }
        NotificationSettingsScreen(
            uiState = state,
            onBackClick = navigateBack,
            onRetry = viewModel::refresh,
            onToggle = viewModel::update,
            onSaveErrorShown = viewModel::dismissSaveError,
            systemNotificationsEnabled = systemSettings.enabled,
            onOpenSystemSettings = systemSettings.openSettings,
        )
    }
}
