package com.yeobaek.feature.group.join.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.GroupJoinVisit
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.invitation.InviteAttribution
import com.yeobaek.feature.group.join.JoinScreen
import com.yeobaek.feature.group.join.JoinViewModel
import kotlinx.serialization.Serializable

@Serializable
data class Join(
    val fromOnboarding: Boolean = false,
    val initialCode: String? = null,
    val invitationRequestId: String? = null,
    val inviteShareId: String? = null,
    val inviteWebVisitId: String? = null,
)

fun NavController.navigateToJoin(
    fromOnboarding: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(route = Join(fromOnboarding = fromOnboarding), navOptions = navOptions)
}

fun NavController.navigateToJoin(
    initialCode: String,
    invitationRequestId: String,
    navOptions: NavOptions? = null,
    attribution: InviteAttribution? = null,
) {
    navigate(
        route = Join(
            initialCode = initialCode,
            invitationRequestId = invitationRequestId,
            inviteShareId = attribution?.shareId,
            inviteWebVisitId = attribution?.webVisitId,
        ),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.joinNavGraph(
    appContainer: AppContainer,
    navigateToBack: (entryId: String, beforePop: () -> Unit) -> Unit,
    navigateToHome: () -> Unit,
) {
    composable<Join> { backStackEntry ->
        val route = backStackEntry.toRoute<Join>()
        val entryPoint = when {
            route.invitationRequestId != null -> GroupJoinEntryPoint.DEEPLINK
            route.fromOnboarding -> GroupJoinEntryPoint.ONBOARDING
            else -> GroupJoinEntryPoint.HOME
        }
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.GROUP_JOIN,
        )
        val joinViewModel: JoinViewModel = viewModel(
            factory = JoinViewModel.joinViewModelFactory(
                visit = GroupJoinVisit(
                    entryPoint,
                    backStackEntry.id,
                    route.invitationRequestId,
                    route.inviteShareId?.let { InviteAttribution(it, route.inviteWebVisitId) },
                ),
                initialCode = route.initialCode,
                groupRepository = appContainer.groupRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
                onJoiningChanged = { joining -> appContainer.invitations.setJoining(backStackEntry.id, joining) },
            ),
        )

        LaunchedEffect(route.initialCode, route.invitationRequestId) {
            joinViewModel.enter()
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
                navigateToBack(backStackEntry.id) {
                    joinViewModel.abandon()
                    appContainer.invitations.cancelPending()
                }
            },
            onJoinClick = joinViewModel::joinGroup,
        )
    }
}
