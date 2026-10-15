package com.yeobaek.feature.home.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.yeobaek.core.analytics.GroupCreateInitiated
import com.yeobaek.core.analytics.GroupDetailOpened
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.GroupJoinInitiated
import com.yeobaek.core.analytics.MyPageOpened
import com.yeobaek.core.analytics.PublicRoomEntryPoint
import com.yeobaek.core.analytics.PublicRoomReaderOpened
import com.yeobaek.core.analytics.ReaderOpened
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.home.HomeScreen
import com.yeobaek.feature.home.HomeViewModel
import com.yeobaek.feature.reader.ReaderTarget
import kotlinx.serialization.Serializable

@Serializable
data object Home

fun NavController.navigateToHome(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Home,
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.homeNavGraph(
    appContainer: AppContainer,
    navigateToJoin: (Boolean) -> Unit,
    navigateToDetail: (Long) -> Unit,
    navigateToCreate: () -> Unit,
    navigateToReader: (Long) -> Unit,
    navigateToPublicRoomReader: (Long) -> Unit,
    navigateToMyPage: () -> Unit,
) {
    composable<Home> {
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.HOME,
        )
        val homeViewModel: HomeViewModel = viewModel(
            factory = HomeViewModel.homeViewModelFactory(
                userRepository = appContainer.userRepository,
                groupRepository = appContainer.groupRepository,
                publicRoomRepository = appContainer.publicRoomRepository,
                crashReporter = appContainer.crashReporter,
            ),
        )

        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            homeViewModel.initGroups()
            homeViewModel.initCurrentlyBook()
            homeViewModel.initPublicRooms()
        }

        HomeScreen(
            appName = appContainer.appName,
            uiState = homeViewModel.uiState,
            onAnalyticsEvent = { event -> appContainer.analyticsTracker.track(event) },
            navigateToJoin = {
                appContainer.analyticsTracker.track(
                    GroupJoinInitiated(GroupJoinEntryPoint.HOME),
                )
                navigateToJoin(false)
            },
            navigateToDetail = { groupId ->
                appContainer.analyticsTracker.track(
                    GroupDetailOpened(groupId = groupId),
                )
                navigateToDetail(groupId)
            },
            navigateToCreate = {
                appContainer.analyticsTracker.track(GroupCreateInitiated)
                navigateToCreate()
            },
            navigateToReader = { readerTarget ->
                when (readerTarget) {
                    is ReaderTarget.Group -> {
                        appContainer.analyticsTracker.track(
                            ReaderOpened(
                                groupId = readerTarget.id,
                                bookTitle = homeViewModel.uiState.currentlyReadingBookUiModel?.title,
                            ),
                        )
                        navigateToReader(readerTarget.id)
                    }

                    is ReaderTarget.PublicRoom -> {
                        appContainer.analyticsTracker.track(
                            PublicRoomReaderOpened(
                                publicRoomId = readerTarget.id,
                                entryPoint = PublicRoomEntryPoint.CONTINUE_READING,
                            ),
                        )
                        navigateToPublicRoomReader(readerTarget.id)
                    }
                }
            },
            onPublicRoomClick = { publicRoomId, entryPoint, listVisitId ->
                appContainer.analyticsTracker.track(
                    PublicRoomReaderOpened(publicRoomId, entryPoint, listVisitId),
                )
                navigateToPublicRoomReader(publicRoomId)
            },
            navigateToMyPage = {
                appContainer.analyticsTracker.track(MyPageOpened)
                navigateToMyPage()
            },
        )
    }
}
