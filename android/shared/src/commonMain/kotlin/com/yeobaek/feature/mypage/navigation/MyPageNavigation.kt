package com.yeobaek.feature.mypage.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

const val MY_PAGE_NICKNAME_CHANGED_KEY = "my_page_nickname_changed"

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
    navigateToNicknameChange: () -> Unit,
) {
    composable<MyPage> { entry ->
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

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { myPageViewModel.initMyPage() }
        val changedNickname by entry.savedStateHandle
            .getStateFlow<String?>(MY_PAGE_NICKNAME_CHANGED_KEY, null).collectAsStateWithLifecycle()

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
            navigateToNicknameChange = navigateToNicknameChange,
            nicknameChanged = changedNickname != null,
            onNicknameChangeMessageShown = {
                entry.savedStateHandle[MY_PAGE_NICKNAME_CHANGED_KEY] = null
            },
        )
    }
}
