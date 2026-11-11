package com.yeobaek.feature.home.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.yeobaek.core.analytics.BookCardContext
import com.yeobaek.core.analytics.GroupDetailOpened
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
import com.yeobaek.feature.survey.HomeSurveyPrompt
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
    navigateToPublicRoomBookDetail: (Long, Long, PublicRoomEntryPoint, BookCardContext) -> Unit,
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

        val userId by appContainer.userPreferences.userId.collectAsState()
        val config by appContainer.appConfigRepository.state.collectAsState()
        val invitation by appContainer.invitations.pendingOpen.collectAsState()
        val joiningOwners by appContainer.invitations.joiningOwners.collectAsState()
        val notification by appContainer.notifications.pendingOpen.collectAsState()

        HomeScreen(
            appName = appContainer.appName,
            uiState = homeViewModel.uiState,
            updatePublicRoomSearchTextValue = homeViewModel::updatePublicRoomSearchTextValue,
            onAnalyticsEvent = { event -> appContainer.analyticsTracker.track(event) },
            navigateToJoin = {
                navigateToJoin(false)
            },
            navigateToDetail = { groupId ->
                appContainer.analyticsTracker.track(
                    GroupDetailOpened(groupId = groupId),
                    mapOf("entry_point" to "home"),
                )
                navigateToDetail(groupId)
            },
            navigateToCreate = navigateToCreate,
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
                                bookId = homeViewModel.uiState.currentlyReadingBookUiModel?.bookId,
                            ),
                        )
                        navigateToPublicRoomReader(readerTarget.id)
                    }
                }
            },
            onPublicRoomClick = { publicRoom, entryPoint, listVisitId ->
                val tab = homeViewModel.uiState.publicRoomTab
                val books = if (entryPoint == PublicRoomEntryPoint.RECENT_ROOMS) {
                    tab.visibleVisitedPublicRooms
                } else {
                    tab.visiblePublicRooms
                }
                val position = books.indexOfFirst { it.publicRoomId == publicRoom.publicRoomId } + 1
                navigateToPublicRoomBookDetail(
                    publicRoom.publicRoomId,
                    publicRoom.bookId,
                    entryPoint,
                    BookCardContext(
                        position = position.takeIf { it > 0 },
                        genreBadgeShown = publicRoom.genres.any(String::isNotBlank),
                        hashtagsShown = publicRoom.hashtags.any { it.trim().trimStart('#').isNotBlank() },
                        listVisitId = listVisitId.takeIf { entryPoint == PublicRoomEntryPoint.ROOM_LIST },
                        recentListVisitId = listVisitId.takeIf { entryPoint == PublicRoomEntryPoint.RECENT_ROOMS },
                    ),
                )
            },
            navigateToMyPage = {
                appContainer.analyticsTracker.track(MyPageOpened, mapOf("entry_point" to "home"))
                navigateToMyPage()
            },
        )
        HomeSurveyPrompt(
            userId = userId,
            config = config.surveyConfig,
            repository = appContainer.surveyRepository,
            preferences = appContainer.surveyPreferences,
            linkLauncher = appContainer.surveyLinkLauncher,
            homeReady = homeViewModel.uiState.isLoadingComplete,
            otherPromptPending = invitation != null || joiningOwners.isNotEmpty() || notification != null,
        )
    }
}
