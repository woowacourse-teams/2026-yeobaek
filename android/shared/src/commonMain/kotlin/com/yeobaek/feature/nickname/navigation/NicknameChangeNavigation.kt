package com.yeobaek.feature.nickname.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.nickname.NicknameChangeScreen
import com.yeobaek.feature.nickname.NicknameUpdateState
import com.yeobaek.feature.nickname.NicknameViewModel
import kotlinx.serialization.Serializable

@Serializable
data object NicknameChange

fun NavController.navigateToNicknameChange() {
    navigate(NicknameChange) { launchSingleTop = true }
}

fun NavGraphBuilder.nicknameChangeNavGraph(
    appContainer: AppContainer,
    navigateBack: () -> Unit,
    onNicknameChanged: (String) -> Unit,
) {
    composable<NicknameChange> { entry ->
        TrackScreen(appContainer.crashReporter, appContainer.analyticsTracker, TrackedScreen.NICKNAME_CHANGE)
        val initialNickname = remember { appContainer.userPreferences.getUsername().orEmpty() }
        val nicknameViewModel: NicknameViewModel = viewModel(
            factory = NicknameViewModel.nicknameViewModelFactory(
                userRepository = appContainer.userRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
                initialNickname = initialNickname,
            ),
        )
        val state = nicknameViewModel.uiState
        LaunchedEffect(state.nicknameUpdateState) {
            if (state.nicknameUpdateState is NicknameUpdateState.Success) {
                onNicknameChanged(state.nicknameValue)
            }
        }
        NicknameChangeScreen(
            uiState = state,
            initialNickname = initialNickname,
            onNicknameValueChange = nicknameViewModel::onNicknameValueChange,
            onNicknameChange = nicknameViewModel::updateNickname,
            onBackClick = {
                val updateState = nicknameViewModel.uiState.nicknameUpdateState
                if (entry.lifecycle.currentState == Lifecycle.State.RESUMED &&
                    updateState !is NicknameUpdateState.Loading &&
                    updateState !is NicknameUpdateState.Success
                ) {
                    nicknameViewModel.abandonChange()
                    navigateBack()
                }
            },
        )
    }
}
