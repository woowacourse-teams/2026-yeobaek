package com.yeobaek.feature.onboarding.create.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.onboarding.create.CreateGroupScreen
import com.yeobaek.feature.onboarding.create.CreateGroupState
import com.yeobaek.feature.onboarding.create.CreateGroupViewModel
import com.yeobaek.feature.onboarding.selectbook.navigation.Onboarding
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingCreate(
    val bookId: Long,
    val attemptId: String,
)

fun NavController.navigateToOnboardingCreate(
    bookId: Long,
    attemptId: String,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = OnboardingCreate(
            bookId = bookId,
            attemptId = attemptId,
        ),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.onboardingCreateNavGraph(
    appContainer: AppContainer,
    navController: NavHostController,
) {
    composable<OnboardingCreate> {
        val route = it.toRoute<OnboardingCreate>()
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.ONBOARDING_CREATE,
        )

        val createGroupViewModel: CreateGroupViewModel = viewModel(
            factory = CreateGroupViewModel.createGroupViewModelFactory(
                bookId = route.bookId,
                attemptId = route.attemptId,
                groupRepository = appContainer.groupRepository,
                bookRepository = appContainer.bookRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )
        LaunchedEffect(route.bookId) {
            createGroupViewModel.initSelectBook(route.bookId)
        }

        CreateGroupScreen(
            uiState = createGroupViewModel.uiState,
            onClickBack = {
                createGroupViewModel.abandon()
                navController.popBackStack()
            },
            selectOtherBook = {
                createGroupViewModel.abandon()
                navController.popBackStack()
            },
            onValueChangeGroupName = createGroupViewModel::updateGroupNameValue,
            onClickCreateGroup = createGroupViewModel::createGroup,
            navigateToHome = {
                if (createGroupViewModel.uiState.createGroupState is CreateGroupState.Success) {
                    navController.navigate(Home) {
                        popUpTo<Onboarding> {
                            inclusive = true
                        }
                    }
                    appContainer.guideOnboardingPreferences.saveOnboardingState(true)
                }
            },
        )
    }
}
