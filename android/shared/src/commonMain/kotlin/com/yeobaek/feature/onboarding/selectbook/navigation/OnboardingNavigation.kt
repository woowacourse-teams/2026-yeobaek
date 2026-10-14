package com.yeobaek.feature.onboarding.selectbook.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.GroupJoinInitiated
import com.yeobaek.core.analytics.ReadingOnboardingSkipped
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.group.join.navigation.navigateToJoin
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.home.navigation.navigateToHome
import com.yeobaek.feature.onboarding.create.navigation.navigateToOnboardingCreate
import com.yeobaek.feature.onboarding.selectbook.OnboardingScreen
import com.yeobaek.feature.onboarding.selectbook.OnboardingViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Onboarding

fun NavController.navigateToOnboarding(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Onboarding,
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.onboardingNavGraph(
    appContainer: AppContainer,
    navController: NavHostController,
) {
    composable<Onboarding> {
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.ONBOARDING,
        )
        val onBoardingViewModel: OnboardingViewModel = viewModel(
            factory = OnboardingViewModel.onboardingViewModelFactory(
                bookRepository = appContainer.bookRepository,
                publicRoomRepository = appContainer.publicRoomRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )

        OnboardingScreen(
            uiState = onBoardingViewModel.uiState,
            navigateToHome = {
                val hasHome = navController.currentBackStack.value.any { entry ->
                    entry.destination.hasRoute<Home>()
                }
                navController.navigateToHome(
                    navOptions = navOptions {
                        navController.currentDestination?.route?.let {
                            if (hasHome) {
                                popUpTo<Home> {
                                    inclusive = true
                                }
                            } else {
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                )
                appContainer.guideOnboardingPreferences.saveOnboardingState(true)
            },
            onClickSkip = {
                appContainer.analyticsTracker.track(ReadingOnboardingSkipped)
            },
            navigateToJoin = {
                appContainer.analyticsTracker.track(
                    GroupJoinInitiated(GroupJoinEntryPoint.ONBOARDING),
                )
                navController.navigateToJoin(fromOnboarding = true)
            },
            onSelectBook = onBoardingViewModel::onSelectBook,
            onDismissBottomSheet = onBoardingViewModel::dismissDialog,
            onClickPublicRoom = { bookId ->
                onBoardingViewModel.onClickJoinPublicRoom(bookId = bookId)
            },
            onClickCreateRoom = { bookId ->
                val attemptId = onBoardingViewModel.selectGroupCreate(bookId)
                    ?: return@OnboardingScreen
                onBoardingViewModel.dismissDialog()
                navController.navigateToOnboardingCreate(bookId = bookId, attemptId = attemptId)
            },
            closeBottomSheet = onBoardingViewModel::closeBottomSheet,
        )
    }
}
