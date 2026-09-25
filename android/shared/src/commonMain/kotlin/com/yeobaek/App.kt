package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.AccountDeleteRequested
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupCreateAbandoned
import com.yeobaek.core.analytics.GroupCreateInitiated
import com.yeobaek.core.analytics.GroupCreateSubmitted
import com.yeobaek.core.analytics.GroupDetailOpened
import com.yeobaek.core.analytics.GroupExitRequested
import com.yeobaek.core.analytics.GroupJoinAbandoned
import com.yeobaek.core.analytics.GroupJoinInitiated
import com.yeobaek.core.analytics.GroupJoinSubmitted
import com.yeobaek.core.analytics.GuideCompleted
import com.yeobaek.core.analytics.GuideDirection
import com.yeobaek.core.analytics.GuideDismissed
import com.yeobaek.core.analytics.GuideEntryPoint
import com.yeobaek.core.analytics.GuidePageMoved
import com.yeobaek.core.analytics.GuideSentenceTapped
import com.yeobaek.core.analytics.GuideStarted
import com.yeobaek.core.analytics.InvalidReason
import com.yeobaek.core.analytics.InviteCodeCopied
import com.yeobaek.core.analytics.MyPageOpened
import com.yeobaek.core.analytics.ReaderOpened
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
import com.yeobaek.feature.navigation.Onboarding
import com.yeobaek.feature.navigation.Reader
import com.yeobaek.feature.nickname.NicknameScreen
import com.yeobaek.feature.nickname.NicknameViewModel
import com.yeobaek.feature.onboarding.OnboardingScreen
import com.yeobaek.feature.onboarding.OnboardingViewModel
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderViewModel

@Composable
fun App(
    appContainer: AppContainer,
) {
    appContainer.userPreferences.clearUser()
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
                        analyticsTracker = appContainer.analyticsTracker,
                    ),
                )

                LaunchedEffect(nicknameViewModel.uiState.successNicknameSet) {
                    if (nicknameViewModel.uiState.successNicknameSet) {
                        appContainer.userPreferences.getUserId()?.let { userId ->
                            appContainer.analyticsTracker.identify(userId)
                        }
                        appContainer.analyticsTracker.track(AnalyticsEvent.UserCreated)
                        navController.navigate(Guide(fromMyPage = false)) {
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
            composable<Guide> { backStackEntry ->
                val route = backStackEntry.toRoute<Guide>()
                val entryPoint = if (route.fromMyPage) {
                    GuideEntryPoint.MY_PAGE
                } else {
                    GuideEntryPoint.ONBOARDING
                }
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = TrackedScreen.GUIDE,
                )

                LaunchedEffect(entryPoint) {
                    appContainer.analyticsTracker.track(GuideStarted(entryPoint = entryPoint))
                }

                val guideViewModel: GuideViewModel = viewModel(factory = GuideViewModel.guideViewModelFactory())

                GuideScreen(
                    uiState = guideViewModel.uiState,
                    navigateToOnboarding = {
                        navController.navigate(Onboarding) {
                            popUpTo<Guide> {
                                inclusive = true
                            }
                        }
                    },
                    onCurrentPage = {
                        guideViewModel.onCurrentPage(it)
                    },
                    onSuccessGuide = {
                        appContainer.analyticsTracker.track(GuideCompleted(entryPoint = entryPoint))
                        guideViewModel.onSuccessGuide()
                    },
                    onClickPrevious = {
                        guideViewModel.onClickPrevious()
                        appContainer.analyticsTracker.track(
                            GuidePageMoved(
                                page = guideViewModel.uiState.currentPage,
                                direction = GuideDirection.PREVIOUS,
                            ),
                        )
                    },
                    onClickNext = {
                        guideViewModel.onClickNext()
                        appContainer.analyticsTracker.track(
                            GuidePageMoved(
                                page = guideViewModel.uiState.currentPage,
                                direction = GuideDirection.NEXT,
                            ),
                        )
                    },
                    isLast = guideViewModel.isLast(),
                    currentPageText = guideViewModel.currentPageText(),
                    onClickCommentSentence = {
                        appContainer.analyticsTracker.track(GuideSentenceTapped(hasComment = true))
                        guideViewModel.onClickCommentSentence()
                    },
                    onClickUnCommentSentence = {
                        appContainer.analyticsTracker.track(GuideSentenceTapped(hasComment = false))
                        guideViewModel.onClickUnCommentSentence()
                    },
                    onCancel = {
                        appContainer.analyticsTracker.track(
                            GuideDismissed(
                                entryPoint = entryPoint,
                                page = guideViewModel.uiState.currentPage,
                            ),
                        )
                        guideViewModel.onCancel()
                    },
                )
            }
            composable<Onboarding> {
                val onBoardingViewModel: OnboardingViewModel = viewModel()

                OnboardingScreen(
                    uiState = onBoardingViewModel.uiState,
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
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    navigateToJoin = {
                        navController.navigate(Join)
                    },
                    onSelectBook = onBoardingViewModel::onSelectBook,
                    onDismissBottomSheet = onBoardingViewModel::dismissDialog,
                    onClickPublicRoom = {
                        TODO("공개방 API가 나오면 구현할 계획")
                    },
                    onClickCreateRoom = {
                        onBoardingViewModel.dismissDialog()
                        navController.navigate(Create)
                    },
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
                        appContainer.analyticsTracker.track(GroupJoinInitiated)
                        navController.navigate(Join)
                    },
                    navigateToDetail = { groupId ->
                        appContainer.analyticsTracker.track(
                            GroupDetailOpened(groupId = groupId),
                        )
                        navController.navigate(Detail(groupId))
                    },
                    navigateToCreate = {
                        appContainer.analyticsTracker.track(GroupCreateInitiated)
                        navController.navigate(Create)
                    },
                    navigateToReader = {
                        appContainer.analyticsTracker.track(
                            ReaderOpened(
                                groupId = it,
                                bookTitle = homeViewModel.uiState.currentlyReadingBookUiModel?.title,
                            ),
                        )
                        navController.navigate(Reader(groupId = it))
                    },
                    navigateToMyPage = {
                        appContainer.analyticsTracker.track(MyPageOpened)
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
                        analyticsTracker = appContainer.analyticsTracker,
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
                    onInviteCodeCopy = {
                        appContainer.analyticsTracker.track(InviteCodeCopied(groupId = route.groupId))
                    },
                    onBlockUser = { otherUserId ->
                        detailViewModel.blockUser(otherUserId)
                    },
                    onUnBlockUser = { otherUserId ->
                        detailViewModel.unBlockUser(otherUserId)
                    },
                    onExitRequest = {
                        appContainer.analyticsTracker.track(GroupExitRequested(groupId = route.groupId))
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onReadClick = {
                        appContainer.analyticsTracker.track(
                            ReaderOpened(
                                groupId = route.groupId,
                                bookTitle = detailViewModel.uiState.bookUiModel.title,
                            ),
                        )
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
                val readerViewModel = viewModel<ReaderViewModel>(
                    factory = ReaderViewModel.readerViewModelFactory(
                        groupId = route.groupId,
                        bookRepository = appContainer.bookRepository,
                        groupRepository = appContainer.groupRepository,
                        readerRepository = appContainer.readerRepository,
                        readerPreferences = appContainer.readerPreferences,
                        commentRepository = appContainer.commentRepository,
                        crashReporter = appContainer.crashReporter,
                        analyticsTracker = appContainer.analyticsTracker,
                    ),
                )
                TrackScreen(
                    crashReporter = appContainer.crashReporter,
                    analyticsTracker = appContainer.analyticsTracker,
                    screen = if (readerViewModel.uiState.isCommentCollectionsVisible) {
                        TrackedScreen.COMMENT_COLLECTION
                    } else {
                        TrackedScreen.READER
                    },
                )
                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    readerViewModel.onScreenStarted()
                }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    readerViewModel.onScreenStopped()
                }
                val commentSheet = readerViewModel.commentSheet
                val actions = remember(readerViewModel, navController) {
                    ReaderActions(
                        onBackClick = {
                            readerViewModel.finishReadingSession()
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
                        onCommentSentenceReveal = readerViewModel::onCommentSentenceRevealed,
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
                        analyticsTracker = appContainer.analyticsTracker,
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
                        appContainer.analyticsTracker.track(
                            GroupJoinAbandoned(hasCode = joinViewModel.uiState.codeValue.isNotBlank()),
                        )
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        joinViewModel.checkCodeBlank()
                        if (joinViewModel.uiState.codeState) {
                            appContainer.analyticsTracker.track(
                                GroupJoinSubmitted(
                                    result = EventResult.INVALID,
                                    reason = InvalidReason.CODE_BLANK,
                                ),
                            )
                        } else {
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
                        analyticsTracker = appContainer.analyticsTracker,
                    ),
                )

                CreateScreen(
                    uiState = createViewModel.uiState,
                    updateGroupNameValue = createViewModel::updateGroupNameValue,
                    selectBook = createViewModel::selectBook,
                    onBookListScrolled = createViewModel::onBookListScrolled,
                    onBackClick = {
                        appContainer.analyticsTracker.track(
                            GroupCreateAbandoned(
                                hasName = createViewModel.uiState.groupNameValue.isNotBlank(),
                                hasBook = createViewModel.uiState.bookList.any { it.selected },
                                bookList = createViewModel.bookListExposure(),
                            ),
                        )
                        navController.popBackStack()
                    },
                    onCreateGroup = {
                        if (createViewModel.createConditionCheck()) {
                            val reason = if (createViewModel.uiState.groupNameCondition) {
                                InvalidReason.NAME_BLANK
                            } else {
                                InvalidReason.BOOK_NOT_SELECTED
                            }
                            appContainer.analyticsTracker.track(
                                GroupCreateSubmitted(
                                    result = EventResult.INVALID,
                                    reason = reason,
                                    bookList = createViewModel.bookListExposure(),
                                ),
                            )
                        } else {
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
                    navigateToNickname = {
                        navController.navigate(Nickname) {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        }
                    },
                    navigateToGuide = {
                        navController.navigate(Guide(fromMyPage = true)) {
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
