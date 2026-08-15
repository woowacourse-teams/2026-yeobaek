package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.repositoryImpl.MockBookRepositoryImpl
import com.yeobaek.data.repositoryImpl.MockGroupRepositoryImpl
import com.yeobaek.data.repositoryImpl.MockUserRepositoryImpl
import com.yeobaek.feature.group.create.CreateScreen
import com.yeobaek.feature.group.create.CreateStateHolder
import com.yeobaek.feature.group.detail.DetailScreen
import com.yeobaek.feature.group.detail.DetailStateHolder
import com.yeobaek.feature.group.join.JoinScreen
import com.yeobaek.feature.group.join.JoinStateHolder
import com.yeobaek.feature.home.HomeScreen
import com.yeobaek.feature.home.HomeStateHolder
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

@Composable
fun App(
    appContainer: AppContainer,
) {
    YeobaekTheme {
        val navController = rememberNavController()

        val userRepository = MockUserRepositoryImpl()
        val bookRepository = MockBookRepositoryImpl()
        val groupRepository = MockGroupRepositoryImpl()

        val onboardingStateHolder = remember {
            OnboardingStateHolder(
                userRepository = userRepository,
                groupRepository = groupRepository,
            )
        }
        val homeUiState = remember {
            HomeStateHolder(
                groupRepository = groupRepository,
            )
        }
        val detailStateHolder = remember {
            DetailStateHolder(
                groupRepository = groupRepository,
                userRepository = userRepository,
            )
        }
        val joinStateHolder = remember {
            JoinStateHolder(
                userRepository = userRepository,
                groupRepository = groupRepository,
            )
        }
        val createStateHolder = remember {
            CreateStateHolder(
                bookRepository = bookRepository,
                groupRepository = groupRepository,
                userRepository = userRepository,
            )
        }

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
                OnboardingScreen(
                    codeValue = onboardingStateHolder.codeValue,
                    codeState = onboardingStateHolder.codeState,
                    onCodeValueChange = onboardingStateHolder::onCodeValueChange,
                    nicknameValue = onboardingStateHolder.nicknameValue,
                    nicknameState = onboardingStateHolder.nicknameState,
                    onNicknameValueChange = onboardingStateHolder::onNicknameValueChange,
                    navigateToCreate = {
                        onboardingStateHolder.nicknameValueCheck()
                        if (!onboardingStateHolder.nicknameState) {
                            onboardingStateHolder.setNickname()

                            navController.navigate(Create)
                        }
                    },
                    navigateToHome = {
                        onboardingStateHolder.nicknameValueCheck()
                        onboardingStateHolder.codeValueCheck()
                        if (!onboardingStateHolder.codeState && !onboardingStateHolder.nicknameState) {
                            onboardingStateHolder.setNickname()
                            onboardingStateHolder.joinGroup()

                            navController.navigate(Home) {
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    navigateToAroundHome = {
                        onboardingStateHolder.nicknameValueCheck()
                        if (!onboardingStateHolder.nicknameState) {
                            onboardingStateHolder.setNickname()

                            navController.navigate(Home) {
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                )
            }
            composable<Home> {
                LaunchedEffect(true) {
                    homeUiState.initGroups()
                }

                HomeScreen(
                    currentlyReadingBookUiModel = homeUiState.uiState.currentlyReadingBookUiModel,
                    groupUiModelList = homeUiState.uiState.groups,
                    myNickname = onboardingStateHolder.nicknameValue,
                    navigateToJoin = {
                        navController.navigate(Join)
                    },
                    navigateToDetail = { groupId, groupCode ->
                        navController.navigate(
                            Detail(
                                groupId = groupId,
                                groupCode = groupCode,
                            ),
                        )
                    },
                    navigateToCreate = {
                        navController.navigate(Create)
                    },
                )
            }
            composable<Detail> { backStackEntry ->
                val route = backStackEntry.toRoute<Detail>()

                detailStateHolder.initGroupData(route.groupCode)

                DetailScreen(
                    groupUiModel = detailStateHolder.uiState.groupUiModel,
                    bookUiModel = detailStateHolder.uiState.bookUiModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onReadClick = {
                        navController.navigate(Reader(groupId = route.groupId))
                    },
                )
            }
            composable<Join> {
                LaunchedEffect(true) {
                    joinStateHolder.initInputValue()
                }
                JoinScreen(
                    codeValue = joinStateHolder.codeValue,
                    codeState = joinStateHolder.codeState,
                    onCodeValueChange = joinStateHolder::onCodeValueChange,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        joinStateHolder.checkValue()
                        if (!joinStateHolder.codeState) {
                            joinStateHolder.joinGroup()

                            navController.navigate(Home) {
                                popUpTo<Home> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                )
            }
            composable<Create> {
                LaunchedEffect(true) {
                    createStateHolder.initInputValue()
                }

                CreateScreen(
                    uiState = createStateHolder.uiState,
                    groupNameCondition = createStateHolder.groupNameCondition,
                    selectedBookCondition = createStateHolder.selectedBookCondition,
                    updateGroupNameValue = createStateHolder::updateGroupNameValue,
                    selectBook = createStateHolder::selectBook,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        createStateHolder.createConditionCheck()
                        if (!createStateHolder.createConditionCheck()) {
                            createStateHolder.createGroup()

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
                    },
                )
            }
        }
    }
}
