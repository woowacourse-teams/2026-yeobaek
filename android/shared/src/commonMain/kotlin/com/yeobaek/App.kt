package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.feature.group.create.CreateScreen
import com.yeobaek.feature.group.create.CreateViewModel
import com.yeobaek.feature.group.detail.BlockState
import com.yeobaek.feature.group.detail.DetailScreen
import com.yeobaek.feature.group.detail.DetailViewModel
import com.yeobaek.feature.group.detail.UnBlockState
import com.yeobaek.feature.group.join.JoinScreen
import com.yeobaek.feature.group.join.JoinViewModel
import com.yeobaek.feature.guide.GuideScreen
import com.yeobaek.feature.guide.GuideViewModel
import com.yeobaek.feature.home.HomeScreen
import com.yeobaek.feature.home.HomeViewModel
import com.yeobaek.feature.mypage.MyPageScreen
import com.yeobaek.feature.mypage.MyPageViewModel
import com.yeobaek.feature.navigation.Create
import com.yeobaek.feature.navigation.Detail
import com.yeobaek.feature.navigation.Guide
import com.yeobaek.feature.navigation.Home
import com.yeobaek.feature.navigation.Join
import com.yeobaek.feature.navigation.MyPage
import com.yeobaek.feature.navigation.Nickname
import com.yeobaek.feature.navigation.Reader
import com.yeobaek.feature.nickname.NicknameScreen
import com.yeobaek.feature.nickname.NicknameViewModel
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderViewModel

@Composable
fun App(
    appContainer: AppContainer,
) {
    YeobaekTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = if (appContainer.userPreferences.getUserId() == null) Nickname else Home,
        ) {
            composable<Nickname> {
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.NICKNAME,
                )
                val nicknameViewModel: NicknameViewModel = viewModel(
                    factory = NicknameViewModel.nicknameViewModelFactory(
                        userRepository = appContainer.userRepository,
                        crashReporter = appContainer.crashReporter,
                    ),
                )

                LaunchedEffect(nicknameViewModel.uiState.successNicknameSet) {
                    if (nicknameViewModel.uiState.successNicknameSet) {
                        appContainer.userPreferences.getUserId()?.let { userId ->
                            appContainer.analyticsTracker.identify(userId)
                        }
                        appContainer.analyticsTracker.track(AnalyticsEvent.UserCreated)
                        navController.navigate(Guide) {
                            popUpTo<Nickname> {
                                inclusive = true
                            }
                        }
                    }
                }

                NicknameScreen(
                    appName = appContainer.appName,
                    uiState = nicknameViewModel.uiState,
                    onNicknameValueChange = nicknameViewModel::onNicknameValueChange,
                    onNicknameSet = {
                        nicknameViewModel.setNickname()
                    },
                )
            }
            composable<Guide> {
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.GUIDE,
                )

                val guideViewModel: GuideViewModel = viewModel(factory = GuideViewModel.guideViewModelFactory())

                GuideScreen(
                    uiState = guideViewModel.uiState,
                    navigateToHome = {
                        val hasHome = navController.currentBackStack.value.any { entry ->
                            entry.destination.hasRoute<Home>()
                        }
                        navController.navigate(Home) {
                            if (hasHome) {
                                popUpTo<Home> {
                                    inclusive = true
                                }
                            } else {
                                popUpTo<Guide> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    onCurrentPage = {
                        guideViewModel.onCurrentPage(it)
                    },
                    onSuccessGuide = guideViewModel::onSuccessGuide,
                    onClickPrevious = guideViewModel::onClickPrevious,
                    onClickNext = guideViewModel::onClickNext,
                    isLast = guideViewModel.isLast(),
                    currentPageText = guideViewModel.currentPageText(),
                    onClickCommentSentence = guideViewModel::onClickCommentSentence,
                    onClickUnCommentSentence = guideViewModel::onClickUnCommentSentence,
                    onCancel = guideViewModel::onCancel,
                )
            }
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
                        crashReporter = appContainer.crashReporter,
                    ),
                )

                LaunchedEffect(true) {
                    homeViewModel.initCurrentlyBook()
                    homeViewModel.initGroups()
                }

                HomeScreen(
                    appName = appContainer.appName,
                    uiState = homeViewModel.uiState,
                    navigateToJoin = {
                        navController.navigate(Join)
                    },
                    navigateToDetail = { groupId ->
                        navController.navigate(Detail(groupId))
                    },
                    navigateToCreate = {
                        navController.navigate(Create)
                    },
                    navigateToReader = {
                        navController.navigate(Reader(groupId = it))
                    },
                    navigateToMyPage = {
                        navController.navigate(MyPage)
                    },
                )
            }
            composable<Detail> { backStackEntry ->
                val route = backStackEntry.toRoute<Detail>()
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.GROUP_DETAIL,
                )

                val detailViewModel: DetailViewModel = viewModel(
                    factory = DetailViewModel.detailViewModelFactory(
                        userRepository = appContainer.userRepository,
                        groupRepository = appContainer.groupRepository,
                        crashReporter = appContainer.crashReporter,
                    ),
                )
                LaunchedEffect(
                    route.groupId,
                ) {
                    detailViewModel.initGroupData(groupId = route.groupId)
                }

                LaunchedEffect(
                    detailViewModel.uiState.blockState,
                    detailViewModel.uiState.unBlockState,
                ) {
                    if (detailViewModel.uiState.blockState is BlockState.Success) {
                        detailViewModel.initGroupData(groupId = route.groupId)
                    }
                    if (detailViewModel.uiState.unBlockState is UnBlockState.Success) {
                        detailViewModel.initGroupData(groupId = route.groupId)
                    }
                }

                DetailScreen(
                    uiState = detailViewModel.uiState,
                    onBlockUser = { otherUserId ->
                        detailViewModel.blockUser(otherUserId)
                    },
                    onUnBlockUser = { otherUserId ->
                        detailViewModel.unBlockUser(otherUserId)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onReadClick = {
                        navController.navigate(Reader(groupId = route.groupId))
                    },
                    onExitClick = {
                        detailViewModel.exitGroup(route.groupId)
                    },
                    navigateToHome = {
                        navController.popBackStack()
                    },
                )
            }
            composable<Reader> { backStackEntry ->
                val route = backStackEntry.toRoute<Reader>()
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.READER,
                )
                val readerViewModel = viewModel<ReaderViewModel>(
                    factory = ReaderViewModel.readerViewModelFactory(
                        groupId = route.groupId,
                        bookRepository = appContainer.bookRepository,
                        groupRepository = appContainer.groupRepository,
                        readerRepository = appContainer.readerRepository,
                        commentRepository = appContainer.commentRepository,
                        crashReporter = appContainer.crashReporter,
                    ),
                )
                val commentSheet = readerViewModel.commentSheet
                val actions = remember(readerViewModel, navController) {
                    ReaderActions(
                        onBackClick = {
                            readerViewModel.saveReadingProgress(
                                onComplete = navController::popBackStack,
                            )
                        },
                        onSentenceClick = readerViewModel::openSentenceComments,
                        onTableOfContentsClick = readerViewModel::openTableOfContents,
                        onTableOfContentsDismiss = readerViewModel::dismissTableOfContents,
                        onChapterClick = readerViewModel::selectChapter,
                        onTextSettingClick = readerViewModel::toggleTextSettingMenu,
                        onTextSettingDismiss = readerViewModel::dismissTextSettingMenu,
                        onFontSizeChange = readerViewModel::updateFontSize,
                        onProgressChange = readerViewModel::selectProgress,
                        onProgressChangeFinished = readerViewModel::moveToSelectedProgress,
                        onLoadPrevious = readerViewModel::loadPreviousPassages,
                        onLoadNext = readerViewModel::loadNextPassages,
                        onVisiblePassageChange = readerViewModel::updateReadingPassage,
                        onTargetPassageReached = readerViewModel::completeMoveToPassage,
                        onTargetPassageNotFound = readerViewModel::cancelMoveToPassage,
                        onCommentCollectionsClick = readerViewModel::openCommentCollections,
                        onCommentCollectionsDismiss = readerViewModel::dismissCommentCollections,
                        onCommentCollectionsRetry = readerViewModel::getCommentCollections,
                        onCommentCardClick = readerViewModel::openSentenceCommentsByCollection,
                        onMoveToComment = readerViewModel::moveToSelectedComment,
                        onReturnToPreviousReadingPosition = readerViewModel::returnToReadingAnchor,
                    )
                }
                val commentSheetActions = remember(commentSheet) {
                    CommentSheetActions(
                        onDismiss = commentSheet::dismiss,
                        onRetry = commentSheet::retryLoad,
                        onInputChange = commentSheet::updateInput,
                        onSubmit = commentSheet::submit,
                        onEdit = commentSheet::startEditing,
                        onEditCancel = commentSheet::cancelEditing,
                        onDelete = commentSheet::requestDelete,
                        onDeleteCancel = commentSheet::cancelDelete,
                        onDeleteConfirm = commentSheet::confirmDelete,
                        onReport = commentSheet::report,
                        onReportResultConsumed = commentSheet::consumeReportResult,
                    )
                }

                ReaderScreen(
                    uiState = readerViewModel.uiState,
                    commentSheet = commentSheet.uiState,
                    actions = actions,
                    commentSheetActions = commentSheetActions,
                )
            }
            composable<Join> {
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.GROUP_JOIN,
                )
                val joinViewModel: JoinViewModel = viewModel(
                    factory = JoinViewModel.joinViewModelFactory(
                        userRepository = appContainer.userRepository,
                        groupRepository = appContainer.groupRepository,
                        crashReporter = appContainer.crashReporter,
                    ),
                )

                LaunchedEffect(Unit) {
                    joinViewModel.initInputValue()
                }

                LaunchedEffect(joinViewModel.uiState.successJoin) {
                    if (joinViewModel.uiState.successJoin) {
                        navController.navigate(Home) {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        }
                    }
                }

                JoinScreen(
                    uiState = joinViewModel.uiState,
                    onCodeValueChange = joinViewModel::onCodeValueChange,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        joinViewModel.checkCodeBlank()
                        if (!joinViewModel.uiState.codeState) {
                            joinViewModel.joinGroup()
                        }
                    },
                )
            }
            composable<Create> {
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.GROUP_CREATE,
                )
                val createViewModel: CreateViewModel = viewModel(
                    factory = CreateViewModel.createViewModelFactory(
                        groupRepository = appContainer.groupRepository,
                        bookRepository = appContainer.bookRepository,
                        crashReporter = appContainer.crashReporter,
                    ),
                )

                CreateScreen(
                    uiState = createViewModel.uiState,
                    updateGroupNameValue = createViewModel::updateGroupNameValue,
                    selectBook = createViewModel::selectBook,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onCreateGroup = {
                        if (!createViewModel.createConditionCheck()) {
                            createViewModel.createGroup()
                        }
                    },
                    navigateToHome = {
                        navController.navigate(Home) {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable<MyPage> {
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.MY_PAGE,
                )
                val myPageViewModel: MyPageViewModel = viewModel(
                    factory = MyPageViewModel.myPageViewModelFactory(
                        userRepository = appContainer.userRepository,
                    ),
                )

                MyPageScreen(
                    uiState = myPageViewModel.uiState,
                    appVersion = appContainer.appVersion,
                    deleteAccount = myPageViewModel::deleteAccount,
                    navigateToNickname = {
                        navController.navigate(Nickname) {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        }
                    },
                    navigateToGuide = {
                        navController.navigate(Guide) {
                            popUpTo<MyPage> {
                                inclusive = false
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TrackScreen(
    crashReporter: CrashReporter,
    analyticsTracker: AnalyticsTracker,
    screen: TrackedScreen,
) {
    LaunchedEffect(screen) {
        analyticsTracker.trackScreen(screen.value)
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = CrashContext(
                screen = screen,
                operation = CrashOperation.SCREEN_VIEWED,
            ),
        )
    }
}
