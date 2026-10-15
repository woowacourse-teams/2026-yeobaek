package com.yeobaek.feature.nickname.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.nickname.NicknameScreen
import com.yeobaek.feature.nickname.NicknameViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Nickname

fun NavController.navigateToNickname(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Nickname,
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.nicknameNavGraph(
    appContainer: AppContainer,
    navigateToGuide: (Boolean) -> Unit,
) {
    composable<Nickname> {
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.NICKNAME,
        )
        val nicknameViewModel: NicknameViewModel = viewModel(
            factory = NicknameViewModel.nicknameViewModelFactory(
                userRepository = appContainer.userRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )

        LaunchedEffect(nicknameViewModel.uiState.successNicknameSet) {
            if (nicknameViewModel.uiState.successNicknameSet) {
                appContainer.userPreferences.getUserId()?.let { userId ->
                    appContainer.analyticsTracker.identify(userId)
                }
                appContainer.analyticsTracker.track(AnalyticsEvent.UserCreated)
                navigateToGuide(false)
            }
        }

        NicknameScreen(
            appName = appContainer.appName,
            uiState = nicknameViewModel.uiState,
            onNicknameValueChange = nicknameViewModel::onNicknameValueChange,
            onNicknameSet = {
                nicknameViewModel.setNickname()
            },
        )
    }
}
