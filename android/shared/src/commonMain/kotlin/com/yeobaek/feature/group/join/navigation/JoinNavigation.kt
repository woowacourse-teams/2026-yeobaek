package com.yeobaek.feature.group.join.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupJoinAbandoned
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.GroupJoinSubmitted
import com.yeobaek.core.analytics.InvalidReason
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.group.join.JoinScreen
import com.yeobaek.feature.group.join.JoinViewModel
import kotlinx.serialization.Serializable

@Serializable
data class Join(
    val fromOnboarding: Boolean = false,
)

fun NavController.navigateToJoin(
    fromOnboarding: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Join(
            fromOnboarding = fromOnboarding,
        ),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.joinNavGraph(
    appContainer: AppContainer,
    navigateToHome: () -> Unit,
    navigateToBack: () -> Unit,
) {
    composable<Join> { backStackEntry ->
        val entryPoint = if (backStackEntry.toRoute<Join>().fromOnboarding) {
            GroupJoinEntryPoint.ONBOARDING
        } else {
            GroupJoinEntryPoint.HOME
        }
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.GROUP_JOIN,
        )
        val joinViewModel: JoinViewModel = viewModel(
            factory = JoinViewModel.joinViewModelFactory(
                entryPoint = entryPoint,
                userRepository = appContainer.userRepository,
                groupRepository = appContainer.groupRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )

        LaunchedEffect(Unit) {
            joinViewModel.initInputValue()
        }

        LaunchedEffect(joinViewModel.uiState.successJoin) {
            if (joinViewModel.uiState.successJoin) {
                navigateToHome()
                if (!appContainer.guideOnboardingPreferences.getOnboardingState()) {
                    appContainer.guideOnboardingPreferences.saveOnboardingState(true)
                }
            }
        }

        JoinScreen(
            uiState = joinViewModel.uiState,
            onCodeValueChange = joinViewModel::onCodeValueChange,
            onBackClick = {
                appContainer.analyticsTracker.track(
                    GroupJoinAbandoned(
                        hasCode = joinViewModel.uiState.codeValue.isNotBlank(),
                        entryPoint = entryPoint,
                    ),
                )
                navigateToBack()
            },
            onJoinClick = {
                joinViewModel.checkCodeBlank()
                if (joinViewModel.uiState.codeState) {
                    appContainer.analyticsTracker.track(
                        GroupJoinSubmitted(
                            result = EventResult.INVALID,
                            entryPoint = entryPoint,
                            reason = InvalidReason.CODE_BLANK,
                        ),
                    )
                } else {
                    joinViewModel.joinGroup()
                }
            },
        )
    }
}
