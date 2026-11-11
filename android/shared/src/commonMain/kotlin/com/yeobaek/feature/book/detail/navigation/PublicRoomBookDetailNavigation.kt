package com.yeobaek.feature.book.detail.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.analytics.BookCardContext
import com.yeobaek.core.analytics.PublicRoomEntryPoint
import com.yeobaek.core.analytics.PublicRoomReaderOpened
import com.yeobaek.core.analytics.bookDetailScreenProperties
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
    val bookPosition: Int = 0,
    val genreBadgeShown: Boolean = false,
    val hashtagsShown: Boolean = false,
    val recentListVisitId: String? = null,
)

fun NavController.navigateToPublicRoomBookDetail(
    publicRoomId: Long,
    bookId: Long,
    entryPoint: PublicRoomEntryPoint,
    listVisitId: String? = null,
    navOptions: NavOptions? = null,
    card: BookCardContext? = null,
) {
    navigate(
        route = PublicRoomBookDetail(
            publicRoomId = publicRoomId,
            bookId = bookId,
            entryPoint = entryPoint.value,
            listVisitId = listVisitId,
            bookPosition = card?.position ?: 0,
            genreBadgeShown = card?.genreBadgeShown == true,
            hashtagsShown = card?.hashtagsShown == true,
            recentListVisitId = card?.recentListVisitId,
        ),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.publicRoomBookDetailNavGraph(
    appContainer: AppContainer,
    navigateToBack: (entryId: String) -> Unit,
    navigateToPublicRoomReader: (Long) -> Unit,
) {
    composable<PublicRoomBookDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<PublicRoomBookDetail>()
        val entryPoint = PublicRoomEntryPoint.entries.firstOrNull { it.value == route.entryPoint }
            ?: PublicRoomEntryPoint.ROOM_LIST
        val bookDetailViewModel: BookDetailViewModel = viewModel(
            key = "book-detail-${route.bookId}",
            factory = BookDetailViewModel.bookDetailViewModelFactory(route.bookId, appContainer.bookRepository),
        )
        val detailVisit = bookDetailViewModel.detailVisit
        val card = if (route.bookPosition > 0 || route.recentListVisitId != null) {
            BookCardContext(
                position = route.bookPosition.takeIf { it > 0 },
                genreBadgeShown = route.genreBadgeShown,
                hashtagsShown = route.hashtagsShown,
                listVisitId = route.listVisitId,
                recentListVisitId = route.recentListVisitId,
            )
        } else {
            null
        }
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = TrackedScreen.BOOK_DETAIL,
            properties = bookDetailScreenProperties(
                bookId = route.bookId,
                publicRoomId = route.publicRoomId,
                entryPoint = entryPoint.value,
                listVisitId = route.listVisitId,
                detailVisitId = detailVisit.id,
                cardContext = card,
            ),
        )
        BookDetailContent(
            state = bookDetailViewModel.state,
            onBackClick = {
                if (backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    navigateToBack(backStackEntry.id)
                }
            },
            onReadClick = {
                if (bookDetailViewModel.state is BookDetailState.Success &&
                    backStackEntry.lifecycle.currentState == Lifecycle.State.RESUMED
                ) {
                    appContainer.analyticsTracker.track(
                        PublicRoomReaderOpened(route.publicRoomId, entryPoint, route.listVisitId, route.bookId),
                        detailVisit.confirmedProperties() + card?.properties.orEmpty(),
                    )
                    navigateToPublicRoomReader(route.publicRoomId)
                }
            },
            onRetry = bookDetailViewModel::retry,
            onMetadataViewed = { section ->
                detailVisit.observe(route.bookId, entryPoint.value, section)?.let {
                    appContainer.analyticsTracker.track(it)
                }
            },
        )
    }
}
