package com.yeobaek.feature.group.detail.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.GroupExitRequested
import com.yeobaek.core.analytics.InviteCodeCopied
import com.yeobaek.core.analytics.ReaderOpened
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.group.detail.BlockState
import com.yeobaek.feature.group.detail.DetailScreen
import com.yeobaek.feature.group.detail.DetailViewModel
import com.yeobaek.feature.group.detail.UnBlockState
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable

@Serializable
data class Detail(
    val groupId: Long,
)

fun NavController.navigateToDetail(
    groupId: Long,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Detail(groupId = groupId),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.detailNavGraph(
    appContainer: AppContainer,
    navigateToReader: (Long) -> Unit,
    navigateToBack: () -> Unit,
) {
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

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(route.groupId, lifecycleOwner, appContainer.notifications) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appContainer.notifications.clubJoinEvents
                    .filter { it.groupId == route.groupId }
                    .collect { detailViewModel.refreshMembers(route.groupId) }
            }
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
            onBackClick = navigateToBack,
            onReadClick = {
                appContainer.analyticsTracker.track(
                    ReaderOpened(
                        groupId = route.groupId,
                        bookTitle = detailViewModel.uiState.bookUiModel.title,
                    ),
                )
                navigateToReader(route.groupId)
            },
            onExitClick = {
                detailViewModel.exitGroup(route.groupId)
            },
            navigateToHome = navigateToBack,
        )
    }
}
