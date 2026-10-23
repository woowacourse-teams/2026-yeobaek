package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.notification.TrackNotificationSpace
import com.yeobaek.data.model.CommentSpace
import com.yeobaek.feature.group.create.navigation.createNavGraph
import com.yeobaek.feature.group.create.navigation.navigateToCreate
import com.yeobaek.feature.group.detail.navigation.detailNavGraph
import com.yeobaek.feature.group.detail.navigation.navigateToDetail
import com.yeobaek.feature.group.join.navigation.joinNavGraph
import com.yeobaek.feature.group.join.navigation.navigateToJoin
import com.yeobaek.feature.guide.navigation.Guide
import com.yeobaek.feature.guide.navigation.guideNavGraph
import com.yeobaek.feature.guide.navigation.navigateToGuide
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.home.navigation.homeNavGraph
import com.yeobaek.feature.home.navigation.navigateToHome
import com.yeobaek.feature.mypage.navigation.MyPage
import com.yeobaek.feature.mypage.navigation.myPageNavGraph
import com.yeobaek.feature.mypage.navigation.navigateToMyPage
import com.yeobaek.feature.nickname.navigation.Nickname
import com.yeobaek.feature.nickname.navigation.navigateToNickname
import com.yeobaek.feature.nickname.navigation.nicknameNavGraph
import com.yeobaek.feature.onboarding.create.navigation.navigateToOnboardingCreate
import com.yeobaek.feature.onboarding.create.navigation.onboardingCreateNavGraph
import com.yeobaek.feature.onboarding.selectbook.navigation.Onboarding
import com.yeobaek.feature.onboarding.selectbook.navigation.navigateToOnboarding
import com.yeobaek.feature.onboarding.selectbook.navigation.onboardingNavGraph
import com.yeobaek.feature.reader.navigation.PublicRoomReader
import com.yeobaek.feature.reader.navigation.Reader
import com.yeobaek.feature.reader.navigation.navigateToPublicRoomReader
import com.yeobaek.feature.reader.navigation.navigateToReader
import com.yeobaek.feature.reader.navigation.publicRoomReaderNavGraph
import com.yeobaek.feature.reader.navigation.readerNavGraph
import kotlinx.coroutines.flow.combine

@Composable
fun App(
    appContainer: AppContainer,
) {
    YeobaekTheme {
        val navController = rememberNavController()
        val startDestination = if (appContainer.userPreferences.getUserId() == null) {
            Nickname
        } else {
            val isFinishGuide = appContainer.guideOnboardingPreferences.getGuideState()
            val isFinishOnboarding = appContainer.guideOnboardingPreferences.getOnboardingState()

            when (isFinishGuide) {
                true if isFinishOnboarding -> {
                    Home
                }

                true if !isFinishOnboarding -> {
                    Onboarding
                }

                else -> {
                    Guide(fromMyPage = false)
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            nicknameNavGraph(
                appContainer = appContainer,
                navigateToGuide = { fromMyPage ->
                    navController.navigateToGuide(
                        fromMyPage = fromMyPage,
                        navOptions = navOptions {
                            popUpTo<Nickname> {
                                inclusive = true
                            }
                        },
                    )
                },
            )
            guideNavGraph(
                appContainer = appContainer,
                onGuideFinished = { fromMyPage ->
                    if (fromMyPage) {
                        navController.navigateToHome(
                            navOptions = navOptions {
                                popUpTo<Home> {
                                    inclusive = true
                                }
                            },
                        )
                    } else {
                        navController.navigateToOnboarding(
                            navOptions = navOptions {
                                popUpTo<Guide> {
                                    inclusive = true
                                }
                            },
                        )
                    }
                },
            )
            onboardingNavGraph(
                appContainer = appContainer,
                navigateToHome = {
                    val hasHome = navController.currentBackStack.value.any { entry ->
                        entry.destination.hasRoute<Home>()
                    }

                    navController.navigateToHome(
                        navOptions = navOptions {
                            if (hasHome) {
                                popUpTo<Home> {
                                    inclusive = true
                                }
                            } else {
                                popUpTo<Onboarding> {
                                    inclusive = true
                                }
                            }
                        },
                    )
                },
                navigateToJoin = { fromOnboarding ->
                    navController.navigateToJoin(fromOnboarding = fromOnboarding)
                },
                navigateToOnboardingCreate = { bookId, attemptId ->
                    navController.navigateToOnboardingCreate(bookId = bookId, attemptId = attemptId)
                },
            )
            onboardingCreateNavGraph(
                appContainer = appContainer,
                navigateToHome = {
                    navController.navigate(Home) {
                        popUpTo<Onboarding> {
                            inclusive = true
                        }
                    }
                },
                navigateToBack = {
                    navController.popBackStack()
                },
            )
            homeNavGraph(
                appContainer = appContainer,
                navigateToJoin = { fromOnboarding ->
                    navController.navigateToJoin(
                        fromOnboarding = fromOnboarding,
                    )
                },
                navigateToDetail = { groupId ->
                    navController.navigateToDetail(
                        groupId = groupId,
                    )
                },
                navigateToCreate = {
                    navController.navigateToCreate()
                },
                navigateToReader = { groupId ->
                    navController.navigateToReader(groupId = groupId)
                },
                navigateToPublicRoomReader = { publicRoomId ->
                    navController.navigateToPublicRoomReader(publicRoomId = publicRoomId)
                },
                navigateToMyPage = {
                    navController.navigateToMyPage()
                },
            )
            detailNavGraph(
                appContainer = appContainer,
                navigateToReader = { groupId ->
                    navController.navigate(Reader(groupId = groupId))
                },
                navigateToBack = {
                    navController.popBackStack()
                },
            )
            readerNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            publicRoomReaderNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            joinNavGraph(
                appContainer = appContainer,
                navigateToHome = {
                    val hasHome = navController.currentBackStack.value.any { entry ->
                        entry.destination.hasRoute<Home>()
                    }

                    val homeNavOptions = navOptions {
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
                    navController.navigateToHome(
                        navOptions = homeNavOptions,
                    )
                },
                navigateToBack = {
                    navController.popBackStack()
                },

            )
            createNavGraph(
                appContainer = appContainer,
                navController = navController,
                navigateToHome = {
                    navController.navigateToHome(
                        navOptions = navOptions {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        },
                    )
                },
            )
            myPageNavGraph(
                appContainer = appContainer,
                navigateToNickname = {
                    navController.navigateToNickname(
                        navOptions = navOptions {
                            popUpTo<Home> {
                                inclusive = true
                            }
                        },
                    )
                },
                navigateToGuide = { fromMyPage ->
                    navController.navigateToGuide(
                        fromMyPage = fromMyPage,
                        navOptions = navOptions {
                            popUpTo<MyPage> {
                                inclusive = false
                            }
                        },
                    )
                },
            )
        }

        TrackNotificationSpace(navController, appContainer.commentNotifications)

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(appContainer, navController, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(
                    appContainer.commentNotifications.pendingOpen,
                    appContainer.userPreferences.userId,
                    navController.currentBackStackEntryFlow,
                ) { request, memberId, _ -> request.takeIf { memberId != null } }
                    .collect { request ->
                        if (request == null || appContainer.commentNotifications.pendingOpen.value !== request) {
                            return@collect
                        }
                        val target = request.notification
                        navController.navigate(Home) {
                            popUpTo(navController.graph.id)
                            launchSingleTop = true
                        }
                        when (val space = target.space) {
                            is CommentSpace.Group -> navController.navigate(
                                Reader(space.groupId, target.passageSequence, target.sentenceId),
                            )

                            is CommentSpace.PublicRoom -> navController.navigate(
                                PublicRoomReader(space.publicRoomId, target.passageSequence, target.sentenceId),
                            )
                        }
                        appContainer.commentNotifications.consumeOpen(request)
                    }
            }
        }
    }
}
