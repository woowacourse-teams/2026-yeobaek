package com.yeobaek

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.navigation.createNavGraph
import com.yeobaek.feature.group.detail.navigation.detailNavGraph
import com.yeobaek.feature.group.join.navigation.joinNavGraph
import com.yeobaek.feature.guide.navigation.Guide
import com.yeobaek.feature.guide.navigation.guideNavGraph
import com.yeobaek.feature.home.navigation.Home
import com.yeobaek.feature.home.navigation.homeNavGraph
import com.yeobaek.feature.mypage.navigation.myPageNavGraph
import com.yeobaek.feature.nickname.navigation.Nickname
import com.yeobaek.feature.nickname.navigation.nicknameNavGraph
import com.yeobaek.feature.onboarding.create.navigation.onboardingCreateNavGraph
import com.yeobaek.feature.onboarding.selectbook.navigation.Onboarding
import com.yeobaek.feature.onboarding.selectbook.navigation.onboardingNavGraph
import com.yeobaek.feature.reader.navigation.publicRoomReaderNavGraph
import com.yeobaek.feature.reader.navigation.readerNavGraph

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
                navController = navController,
            )
            guideNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            onboardingNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            onboardingCreateNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            homeNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            detailNavGraph(
                appContainer = appContainer,
                navController = navController,
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
                navController = navController,
            )
            createNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
            myPageNavGraph(
                appContainer = appContainer,
                navController = navController,
            )
        }
    }
}
