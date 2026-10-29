package com.yeobaek.feature.onboarding.selectbook.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.GroupJoinInitiated
import com.yeobaek.core.analytics.ReadingOnboardingSkipped
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
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
    navigateToHome: () -> Unit,
    navigateToJoin: (Boolean) -> Unit,
    navigateToOnboardingCreate: (Long, String) -> Unit,
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

        val appConfig by appContainer.appConfigRepository.state.collectAsStateWithLifecycle()

        OnboardingScreen(
            uiState = onBoardingViewModel.uiState,
            title = appConfig.sessionMessages.onboardingTitle,
            updateSearchTextValue = onBoardingViewModel::updateSearchTextValue,
            navigateToHome = {
                navigateToHome()
                appContainer.guideOnboardingPreferences.saveOnboardingState(true)
            },
            onClickSkip = {
                appContainer.analyticsTracker.track(ReadingOnboardingSkipped)
            },
            navigateToJoin = {
                appContainer.analyticsTracker.track(
                    GroupJoinInitiated(GroupJoinEntryPoint.ONBOARDING),
                )
                navigateToJoin(true)
            },
            onSelectBook = onBoardingViewModel::onSelectBook,
            onReadBook = onBoardingViewModel::showReadingOptions,
            onBackBookDetail = onBoardingViewModel::closeBookDetail,
            onRetryBookDetail = onBoardingViewModel::retryBookDetail,
            onDismissBottomSheet = onBoardingViewModel::dismissDialog,
            onClickPublicRoom = { bookId ->
                onBoardingViewModel.onClickJoinPublicRoom(bookId = bookId)
            },
            onClickCreateRoom = { bookId ->
                val attemptId = onBoardingViewModel.selectGroupCreate(bookId)
                    ?: return@OnboardingScreen
                onBoardingViewModel.dismissDialog()
                navigateToOnboardingCreate(bookId, attemptId)
            },
            closeBottomSheet = onBoardingViewModel::closeBottomSheet,
        )
    }
}
