package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.repositoryImpl.MockBookRepositoryImpl
import com.yeobaek.data.repositoryImpl.MockGroupRepositoryImpl
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
import com.yeobaek.feature.onboarding.OnboardingScreen
import com.yeobaek.feature.onboarding.OnboardingStateHolder

@Composable
@Preview
fun App() {
    YeobaekTheme {
        val navController = rememberNavController()

        val bookRepository = MockBookRepositoryImpl()
        val groupRepository = MockGroupRepositoryImpl()

        val onboardingStateHolder = remember {
            OnboardingStateHolder(
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
            )
        }
        val joinStateHolder = remember {
            JoinStateHolder(
                groupRepository = groupRepository,
            )
        }
        val createStateHolder = remember {
            CreateStateHolder(
                bookRepository = bookRepository,
                groupRepository = groupRepository,
            )
        }

        NavHost(
            navController = navController,
            startDestination = Onboarding,
        ) {
            composable<Onboarding> {
                OnboardingScreen(
                    codeValue = onboardingStateHolder.codeValue,
                    codeState = onboardingStateHolder.codeState,
                    onCodeValueChange = onboardingStateHolder::onCodeValueChange,
                    navigateToCreate = {
                        navController.navigate(Create)
                    },
                    navigateToHome = {
                        onboardingStateHolder.checkValue()
                        if (!onboardingStateHolder.codeState) {
                            onboardingStateHolder.joinGroup()

                            navController.navigate(Home) {
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    navigateToAroundHome = {
                        navController.navigate(Home) {
                            popUpTo<Onboarding> {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable<Home> {
                LaunchedEffect(true) {
                    homeUiState.initGroups()
                }

                HomeScreen(
                    currentlyReadingBookUiModel = homeUiState.uiState.currentlyReadingBookUiModel,
                    groupUiModelList = homeUiState.uiState.groups,
                    navigateToJoin = {
                        navController.navigate(Join)
                    },
                    navigateToDetail = { groupCode ->
                        navController.navigate(Detail(groupCode))
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
                )
            }
            composable<Join> {
                JoinScreen(
                    codeValue = joinStateHolder.codeValue,
                    onCodeValueChange = joinStateHolder::onCodeValueChange,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        joinStateHolder.joinGroup()

                        navController.navigate(Home) {
                            popUpTo<Join> {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable<Create> {
                CreateScreen(
                    uiState = createStateHolder.uiState,
                    updateGroupNameValue = createStateHolder::updateGroupNameValue,
                    selectBook = createStateHolder::selectBook,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToHome = {
                        createStateHolder.createGroup()
                        navController.navigate(Home) {
                            popUpTo<Create> {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
    }
}
