package com.yeobaek.feature.mypage.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import com.yeobaek.feature.mypage.MyPageUiState
import com.yeobaek.feature.mypage.MyPageViewModel
import com.yeobaek.feature.mypage.systemNotificationChecker
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
                notificationPreferences = appContainer.notificationPreferences,
                userRepository = appContainer.userRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )

        MyPageRoute(
            uiState = myPageViewModel.uiState,
            appVersion = appContainer.appVersion,
            onDeleteAccountClick = {
                appContainer.analyticsTracker.track(AccountDeleteRequested)
            },
            deleteAccount = myPageViewModel::deleteAccount,
            navigateToNickname = navigateToNickname,
            onNotificationsEnabledChange = myPageViewModel::setNotificationsEnabled,
            navigateToGuide = {
                navigateToGuide(true)
            },
        )
    }
}

@Composable
private fun MyPageRoute(
    uiState: MyPageUiState,
    appVersion: String,
    onDeleteAccountClick: () -> Unit,
    deleteAccount: () -> Unit,
    navigateToNickname: () -> Unit,
    navigateToGuide: () -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
) {
    val notificationChecker = systemNotificationChecker()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentNotificationsEnabled by rememberUpdatedState(uiState.notificationsEnabled)
    val currentOnNotificationsEnabledChange by rememberUpdatedState(onNotificationsEnabledChange)
    val currentNotificationChecker by rememberUpdatedState(notificationChecker)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!currentNotificationChecker.isSystemNotificationEnabled() && currentNotificationsEnabled) {
                currentOnNotificationsEnabledChange(false)
            }
        }
    }

    MyPageScreen(
        uiState = uiState,
        appVersion = appVersion,
        onDeleteAccountClick = onDeleteAccountClick,
        deleteAccount = deleteAccount,
        navigateToNickname = navigateToNickname,
        navigateToGuide = navigateToGuide,
        onNotificationsEnabledChange = onNotificationsEnabledChange,
        isSystemNotificationEnabled = notificationChecker::isSystemNotificationEnabled,
    )
}
