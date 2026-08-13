package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.repositoryImpl.remote.RemoteBookRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.RemoteGroupRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.RemoteUserRepositoryImpl
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
import com.yeobaek.feature.navigation.Onboarding
import com.yeobaek.feature.navigation.Reader
import com.yeobaek.feature.onboarding.OnboardingScreen
import com.yeobaek.feature.onboarding.OnboardingStateHolder
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderViewModel
import com.yeobaek.feature.reader.ReaderViewModelFactory
import com.yeobaek.feature.onboarding.OnboardingViewModel

@Composable
fun App(
    appContainer: AppContainer,
) {
    YeobaekTheme {
        val navController = rememberNavController()

        val userRepository = RemoteUserRepositoryImpl(
            userApi = appContainer.apiProvider.userApi,
        )
        val bookRepository = RemoteBookRepositoryImpl(
            bookApi = appContainer.apiProvider.booksApi,
        )
        val groupRepository = RemoteGroupRepositoryImpl(
            clubApi = appContainer.apiProvider.clubApi,
        )

//        val detailStateHolder = remember {
//            DetailStateHolder(
//                groupRepository = groupRepository,
//                userRepository = userRepository,
//            )
//        }
//        val joinStateHolder = remember {
//            JoinStateHolder(
//                userRepository = userRepository,
//                groupRepository = groupRepository,
//            )
//        }
//        val createStateHolder = remember {
//            CreateStateHolder(
//                bookRepository = bookRepository,
//                groupRepository = groupRepository,
//                userRepository = userRepository,
//            )
//        }

        NavHost(
            navController = navController,
            startDestination = Onboarding,
        ) {
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
            composable<Onboarding> {
                val onboardingViewModel: OnboardingViewModel = viewModel(
                    factory = OnboardingViewModel.onboardingViewModelFactory(
                        userRepository = userRepository,
                        groupRepository = groupRepository,
                    ),
                )

                OnboardingScreen(
                    uiState = onboardingViewModel.uiState,
                    onCodeValueChange = onboardingViewModel::onCodeValueChange,
                    onNicknameValueChange = onboardingViewModel::onNicknameValueChange,
                    navigateToCreate = {
                        onboardingViewModel.setNickname()
                        navController.navigate(Create)
                    },
                    navigateToHome = {
                        onboardingViewModel.setNickname()
                        onboardingViewModel.joinGroup()

                        navController.navigate(Home) {
                            popUpTo<Onboarding> {
                                inclusive = true
                            }
                        }
                    },
                    navigateToAroundHome = {
                        onboardingViewModel.setNickname()

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
                        groupRepository = groupRepository,
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
                    navigateToDetail = { groupCode ->
                        navController.navigate(Detail(groupCode))
                    },
                    navigateToCreate = {
                        // navController.navigate(Create)
                    },
                )
            }
            composable<Detail> { backStackEntry ->
                val route = backStackEntry.toRoute<Detail>()

                val detailViewModel: DetailViewModel = viewModel(
                    factory = DetailViewModel.detailViewModelFactory(
                        groupRepository = groupRepository,
                    ),
                )

                detailViewModel.initGroupData(userId = 2, groupId = route.groupId)

                DetailScreen(
                    uiState = detailViewModel.uiState,
                    onBackClick = {
                        navController.popBackStack()
                    },
                )
            }
            composable<Join> {
                val joinViewModel: JoinViewModel = viewModel(
                    factory = JoinViewModel.joinViewModelFactory(
                        groupRepository = groupRepository,
                    ),
                )

                LaunchedEffect(true) {
                    joinViewModel.initInputValue()
                }

                JoinScreen(
                    uiState = joinViewModel.uiState,
                    onCodeValueChange = joinViewModel::onCodeValueChange,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        joinViewModel.joinGroup()

                        navController.navigate(Home) {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        }
                    },
                )
            }
//            composable<Create> {
//                LaunchedEffect(true) {
//                    createStateHolder.initInputValue()
//                }
//
//                CreateScreen(
//                    uiState = createStateHolder.uiState,
//                    groupNameCondition = createStateHolder.groupNameCondition,
//                    selectedBookCondition = createStateHolder.selectedBookCondition,
//                    updateGroupNameValue = createStateHolder::updateGroupNameValue,
//                    selectBook = createStateHolder::selectBook,
//                    onBackClick = {
//                        navController.popBackStack()
//                    },
//                    navigateToHome = {
//                        createStateHolder.createConditionCheck()
//                        if (!createStateHolder.createConditionCheck()) {
//                            createStateHolder.createGroup()
//
//                            val popped = navController.popBackStack<Home>(
//                                inclusive = false,
//                            )
//                            if (!popped) {
//                                navController.navigate(Home) {
//                                    popUpTo<Onboarding> {
//                                        inclusive = true
//                                    }
//                                }
//                            }
//                        }
//                    },
//                )
//            }
        }
    }
}
