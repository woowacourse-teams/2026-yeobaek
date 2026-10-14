package com.yeobaek.feature.nickname.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.guide.navigation.navigateToGuide
import com.yeobaek.feature.nickname.NicknameScreen
import com.yeobaek.feature.nickname.NicknameViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Nickname

fun NavGraphBuilder.nicknameNavGraph(
    appContainer: AppContainer,
    navController: NavHostController,
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
                navController.navigateToGuide(
                    fromMyPage = false,
                    navOptions = navOptions {
                        navController.currentDestination?.route?.let {
                            popUpTo<Nickname> {
                                inclusive = true
                            }
                        }
                    },
                )
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
