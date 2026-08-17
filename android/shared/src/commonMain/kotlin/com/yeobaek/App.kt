package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.CreateScreen
import com.yeobaek.feature.group.create.CreateViewModel
import com.yeobaek.feature.group.detail.DetailScreen
import com.yeobaek.feature.group.detail.DetailViewModel
import com.yeobaek.feature.group.join.JoinScreen
import com.yeobaek.feature.group.join.JoinViewModel
import com.yeobaek.feature.home.HomeScreen
import com.yeobaek.feature.home.HomeViewModel
import com.yeobaek.feature.navigation.Create
import com.yeobaek.feature.navigation.Detail
import com.yeobaek.feature.navigation.Home
import com.yeobaek.feature.navigation.Join
import com.yeobaek.feature.navigation.Nickname
import com.yeobaek.feature.navigation.Onboarding
import com.yeobaek.feature.navigation.Reader
import com.yeobaek.feature.nickname.NicknameScreen
import com.yeobaek.feature.nickname.NicknameViewModel
import com.yeobaek.feature.onboarding.OnboardingScreen
import com.yeobaek.feature.onboarding.OnboardingViewModel
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderViewModel
import com.yeobaek.feature.reader.ReaderViewModelFactory

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
                val nicknameViewModel: NicknameViewModel = viewModel(
                    factory = NicknameViewModel.nicknameViewModelFactory(
                        userRepository = appContainer.userRepository,
                    ),
                )

                LaunchedEffect(nicknameViewModel.uiState.successNicknameSet) {
                    if (nicknameViewModel.uiState.successNicknameSet) {
                        navController.navigate(Onboarding) {
                            popUpTo<Nickname> {
                                inclusive = true
                            }
                        }
                    }
                }

                NicknameScreen(
                    uiState = nicknameViewModel.uiState,
                    onNicknameValueChange = nicknameViewModel::onNicknameValueChange,
                    onNicknameSet = {
                        nicknameViewModel.setNickname()
                    },
                )
            }
            composable<Onboarding> {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.onboardingViewModelFactory(
                        groupRepository = appContainer.groupRepository,
                    ),
                )

                LaunchedEffect(onboardingViewModel.uiState.successJoin) {
                    if (onboardingViewModel.uiState.successJoin && !onboardingViewModel.uiState.codeState) {
                        navController.navigate(Home) {
                            popUpTo<Onboarding> {
                                inclusive = true
                            }
                        }
                    }
                }

                OnboardingScreen(
                    uiState = onboardingViewModel.uiState,
                    onCodeValueChange = onboardingViewModel::onCodeValueChange,
                    navigateToCreate = {
                        navController.navigate(Create)
                    },
                    navigateToHome = {
                        onboardingViewModel.checkCodeBlank()
                        if (!onboardingViewModel.uiState.codeState) {
                            onboardingViewModel.joinGroup()
                        }
                    },
                    navigateToAroundHome = {
                        navController.navigate(Home) {
                            popUpTo<Onboarding> {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable<Home> {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.homeViewModelFactory(
                        userRepository = appContainer.userRepository,
                        groupRepository = appContainer.groupRepository,
                    ),
                )

                LaunchedEffect(true) {
                    homeViewModel.initGroups()
                }

                HomeScreen(
                    currentlyReadingBookUiModel = homeViewModel.uiState.currentlyReadingBookUiModel,
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
                )
            }
            composable<Detail> { backStackEntry ->
                val route = backStackEntry.toRoute<Detail>()

                val detailViewModel: DetailViewModel = viewModel(
                    factory = DetailViewModel.detailViewModelFactory(
                        groupRepository = appContainer.groupRepository,
                    ),
                )
                LaunchedEffect(route.groupId) {
                    detailViewModel.initGroupData(groupId = route.groupId)
                }

                DetailScreen(
                    uiState = detailViewModel.uiState,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onReadClick = {
                        navController.navigate(Reader(groupId = route.groupId))
                    },
                )
            }
            composable<Reader> { backStackEntry ->
                val route = backStackEntry.toRoute<Reader>()
                val readerViewModel = viewModel<ReaderViewModel>(
                    factory = ReaderViewModelFactory(
                        groupId = route.groupId,
                        groupRepository = appContainer.groupRepository,
                        readerRepository = appContainer.readerRepository,
                    ),
                )

                ReaderScreen(
                    uiState = readerViewModel.uiState,
                    onPassageClick = readerViewModel::openPassageComments,
                    onBackClick = {
                        readerViewModel.saveCurrentPassage(
                            onComplete = navController::popBackStack,
                        )
                    },
                    onTextSettingClick = readerViewModel::toggleTextSettingMenu,
                    onTextSettingDismiss = readerViewModel::dismissTextSettingMenu,
                    onFontSizeChange = readerViewModel::updateFontSize,
                    onCommentSheetDismiss = readerViewModel::dismissPassageComments,
                    onCommentInputChange = readerViewModel::updateCommentInput,
                    onCommentSubmit = readerViewModel::submitComment,
                    onCommentEdit = readerViewModel::startEditingComment,
                    onCommentEditCancel = readerViewModel::cancelEditingComment,
                    onCommentDelete = readerViewModel::requestDeleteComment,
                    onCommentDeleteCancel = readerViewModel::cancelDeleteComment,
                    onCommentDeleteConfirm = readerViewModel::confirmDeleteComment,
                    onLoadPrevious = readerViewModel::loadPreviousPassages,
                    onLoadNext = readerViewModel::loadNextPassages,
                    onVisiblePassageChange = readerViewModel::updateCurrentPassage,
                    onProgressChange = readerViewModel::updateSeekProgress,
                    onProgressChangeFinished = readerViewModel::seekToProgress,
                    onProgressSeekCompleted = readerViewModel::completeProgressSeek,
                )
            }
            composable<Join> {
                val joinViewModel: JoinViewModel = viewModel(
                    factory = JoinViewModel.joinViewModelFactory(
                        userRepository = appContainer.userRepository,
                        groupRepository = appContainer.groupRepository,
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
                val createViewModel: CreateViewModel = viewModel(
                    factory = CreateViewModel.createViewModelFactory(
                        groupRepository = appContainer.groupRepository,
                        bookRepository = appContainer.bookRepository,
                    ),
                )

                LaunchedEffect(true) {
                    createViewModel.initInputValue()
                }

                LaunchedEffect(createViewModel.uiState.successBookLoading) {
                    createViewModel.initBookList()
                }

                LaunchedEffect(createViewModel.uiState.successCreate) {
                    if (createViewModel.uiState.successCreate) {
                        val popped = navController.popBackStack<Home>(
                            inclusive = false,
                        )
                        if (!popped) {
                            navController.navigate(Home) {
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }

                CreateScreen(
                    uiState = createViewModel.uiState,
                    updateGroupNameValue = createViewModel::updateGroupNameValue,
                    selectBook = createViewModel::selectBook,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        if (!createViewModel.createConditionCheck()) {
                            createViewModel.createGroup()
                        }
                    },
                )
            }
        }
    }
}
