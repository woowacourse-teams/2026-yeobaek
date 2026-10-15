package com.yeobaek.feature.guide.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.GuideCompleted
import com.yeobaek.core.analytics.GuideDirection
import com.yeobaek.core.analytics.GuideDismissed
import com.yeobaek.core.analytics.GuideEntryPoint
import com.yeobaek.core.analytics.GuidePageMoved
import com.yeobaek.core.analytics.GuideSentenceTapped
import com.yeobaek.core.analytics.GuideStarted
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.guide.GuideScreen
import com.yeobaek.feature.guide.GuideViewModel
import kotlinx.serialization.Serializable

@Serializable
data class Guide(
    val fromMyPage: Boolean = false,
)

fun NavController.navigateToGuide(
    fromMyPage: Boolean,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Guide(
            fromMyPage = fromMyPage,
        ),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.guideNavGraph(
    appContainer: AppContainer,
    onGuideFinished: (Boolean) -> Unit,
) {
    composable<Guide> { backStackEntry ->
        val route = backStackEntry.toRoute<Guide>()
        val entryPoint = if (route.fromMyPage) {
            GuideEntryPoint.MY_PAGE
        } else {
            GuideEntryPoint.ONBOARDING
        }
        val guideViewModel: GuideViewModel = viewModel(
            factory = GuideViewModel.guideViewModelFactory(
                guideOnboardingPreferences = appContainer.guideOnboardingPreferences,
            ),
        )

        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.GUIDE,
        )

        LaunchedEffect(entryPoint) {
            appContainer.analyticsTracker.track(GuideStarted(entryPoint = entryPoint))
        }

        LaunchedEffect(Unit) {
            guideViewModel.initGuidePage(route.fromMyPage)
            guideViewModel.initSentences()
        }

        GuideScreen(
            uiState = guideViewModel.uiState,
            navigateToRoute = {
                onGuideFinished(route.fromMyPage)

                appContainer.guideOnboardingPreferences.saveGuideState(true)
                appContainer.guideOnboardingPreferences.saveGuidePage(1)
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
}
