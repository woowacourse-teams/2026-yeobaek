package com.yeobaek.feature.book.detail.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.PublicRoomEntryPoint
import com.yeobaek.core.analytics.PublicRoomReaderOpened
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.book.detail.BookDetailContent
import com.yeobaek.feature.book.detail.BookDetailState
import com.yeobaek.feature.book.detail.BookDetailViewModel
import kotlinx.serialization.Serializable

@Serializable
data class PublicRoomBookDetail(
    val publicRoomId: Long,
    val bookId: Long,
    val entryPoint: String,
    val listVisitId: String? = null,
)

fun NavController.navigateToPublicRoomBookDetail(
    publicRoomId: Long,
    bookId: Long,
    entryPoint: PublicRoomEntryPoint,
    listVisitId: String? = null,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = PublicRoomBookDetail(publicRoomId, bookId, entryPoint.value, listVisitId),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.publicRoomBookDetailNavGraph(
    appContainer: AppContainer,
    navigateToPublicRoomReader: (Long) -> Unit,
    navigateToBack: () -> Unit,
) {
    composable<PublicRoomBookDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<PublicRoomBookDetail>()
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.BOOK_DETAIL,
        )
        val bookDetailViewModel: BookDetailViewModel = viewModel(
            key = "book-detail-${route.bookId}",
            factory = BookDetailViewModel.bookDetailViewModelFactory(route.bookId, appContainer.bookRepository),
        )
        BookDetailContent(
            state = bookDetailViewModel.state,
            onBackClick = {
                if (backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED) navigateToBack()
            },
            onReadClick = {
                if (bookDetailViewModel.state is BookDetailState.Success &&
                    backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED
                ) {
                    val entryPoint = PublicRoomEntryPoint.entries.firstOrNull { it.value == route.entryPoint }
                        ?: PublicRoomEntryPoint.ROOM_LIST
                    appContainer.analyticsTracker.track(
                        PublicRoomReaderOpened(route.publicRoomId, entryPoint, route.listVisitId),
                    )
                    navigateToPublicRoomReader(route.publicRoomId)
                }
            },
            onRetry = bookDetailViewModel::retry,
        )
    }
}
