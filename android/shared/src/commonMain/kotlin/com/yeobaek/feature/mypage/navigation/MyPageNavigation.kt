package com.yeobaek.feature.mypage.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.yeobaek.core.analytics.AccountDeleteRequested
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.mypage.MyPageScreen
import com.yeobaek.feature.mypage.MyPageViewModel
import kotlinx.serialization.Serializable

@Serializable
data object MyPage

fun NavController.navigateToMyPage(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = MyPage,
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.myPageNavGraph(
    appContainer: AppContainer,
    navigateToNickname: () -> Unit,
    navigateToGuide: (Boolean) -> Unit,
    navigateToNotificationSettings: () -> Unit,
) {
    composable<MyPage> {
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.MY_PAGE,
        )
        val myPageViewModel: MyPageViewModel = viewModel(
            factory = MyPageViewModel.myPageViewModelFactory(
                guideOnboardingPreferences = appContainer.guideOnboardingPreferences,
                userRepository = appContainer.userRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )

        MyPageScreen(
            uiState = myPageViewModel.uiState,
            appVersion = appContainer.appVersion,
            onDeleteAccountClick = {
                appContainer.analyticsTracker.track(AccountDeleteRequested)
            },
            deleteAccount = myPageViewModel::deleteAccount,
            navigateToNickname = navigateToNickname,
            navigateToGuide = {
                navigateToGuide(true)
            },
            navigateToNotificationSettings = navigateToNotificationSettings,
        )
    }
}
