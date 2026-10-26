package com.yeobaek.feature.reader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.ReaderActions
import com.yeobaek.feature.reader.ReaderCommentTarget
import com.yeobaek.feature.reader.ReaderScreen
import com.yeobaek.feature.reader.ReaderTarget
import com.yeobaek.feature.reader.ReaderViewModel
import kotlinx.serialization.Serializable

@Serializable
data class Reader(
    val groupId: Long,
    val passageSequence: Int? = null,
    val sentenceId: Long? = null,
)

@Serializable
data class PublicRoomReader(
    val publicRoomId: Long,
    val passageSequence: Int? = null,
    val sentenceId: Long? = null,
)

fun NavController.navigateToReader(
    groupId: Long,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Reader(
            groupId = groupId,
        ),
        navOptions = navOptions,
    )
}

fun NavController.navigateToPublicRoomReader(
    publicRoomId: Long,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = PublicRoomReader(
            publicRoomId = publicRoomId,
        ),
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.readerNavGraph(
    appContainer: AppContainer,
    navController: NavHostController,
) {
    composable<Reader> { backStackEntry ->
        val route = backStackEntry.toRoute<Reader>()
        ReaderRoute(
            readerTarget = ReaderTarget.Group(route.groupId),
            commentTarget = ReaderCommentTarget.from(route.passageSequence, route.sentenceId),
            trackedScreen = TrackedScreen.READER,
            appContainer = appContainer,
            onBackClick = navController::popBackStack,
        )
    }
}

fun NavGraphBuilder.publicRoomReaderNavGraph(
    appContainer: AppContainer,
    navController: NavHostController,
) {
    composable<PublicRoomReader> { backStackEntry ->
        val route = backStackEntry.toRoute<PublicRoomReader>()
        ReaderRoute(
            readerTarget = ReaderTarget.PublicRoom(route.publicRoomId),
            commentTarget = ReaderCommentTarget.from(route.passageSequence, route.sentenceId),
            trackedScreen = TrackedScreen.PUBLIC_ROOM_READER,
            appContainer = appContainer,
            onBackClick = navController::popBackStack,
        )
    }
}

@Composable
private fun ReaderRoute(
    readerTarget: ReaderTarget,
    commentTarget: ReaderCommentTarget?,
    trackedScreen: TrackedScreen,
    appContainer: AppContainer,
    onBackClick: () -> Unit,
) {
    val readerViewModel = viewModel<ReaderViewModel>(
        factory = ReaderViewModel.readerViewModelFactory(
            readerTarget = readerTarget,
            commentTarget = commentTarget,
            bookRepository = appContainer.bookRepository,
            groupRepository = appContainer.groupRepository,
            publicRoomRepository = appContainer.publicRoomRepository,
            readerRepository = appContainer.readerRepository,
            readerPreferences = appContainer.readerPreferences,
            commentRepository = appContainer.commentRepository,
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            commentNotifications = appContainer.commentNotifications,
        ),
    )
    TrackScreen(
        crashReporter = appContainer.crashReporter,
        analyticsTracker = appContainer.analyticsTracker,
        screen = if (readerViewModel.uiState.isCommentCollectionsVisible) {
            TrackedScreen.COMMENT_COLLECTION
        } else {
            trackedScreen
        },
    )
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        readerViewModel.onScreenStarted()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        readerViewModel.onScreenStopped()
    }
    val commentSheet = readerViewModel.commentSheet
    val actions = remember(readerViewModel, onBackClick) {
        ReaderActions(
            onBackClick = {
                readerViewModel.finishReadingSession()
                readerViewModel.saveReadingProgress(onComplete = onBackClick)
            },
            onSentenceClick = readerViewModel::openSentenceComments,
            onTableOfContentsClick = readerViewModel::openTableOfContents,
            onTableOfContentsDismiss = readerViewModel::dismissTableOfContents,
            onChapterClick = readerViewModel::selectChapter,
            onTextSettingClick = readerViewModel::toggleTextSettingMenu,
            onTextSettingDismiss = readerViewModel::dismissTextSettingMenu,
            onFontSizeChange = readerViewModel::updateFontSize,
            onProgressChange = readerViewModel::selectProgress,
            onProgressChangeFinished = readerViewModel::moveToSelectedProgress,
            onLoadPrevious = readerViewModel::loadPreviousPassages,
            onLoadNext = readerViewModel::loadNextPassages,
            onVisiblePassageChange = readerViewModel::updateReadingPassage,
            onTargetPassageReached = readerViewModel::completeMoveToPassage,
            onTargetPassageNotFound = readerViewModel::cancelMoveToPassage,
            onCommentCollectionsClick = readerViewModel::openCommentCollections,
            onCommentCollectionsDismiss = readerViewModel::dismissCommentCollections,
            onCommentCollectionsRetry = readerViewModel::getCommentCollections,
            onCommentCardClick = readerViewModel::openSentenceCommentsByCollection,
            onCommentSentenceReveal = readerViewModel::onCommentSentenceRevealed,
            onMoveToComment = readerViewModel::moveToSelectedComment,
            onReturnToPreviousReadingPosition = readerViewModel::returnToReadingAnchor,
        )
    }
    val commentSheetActions = remember(commentSheet) {
        CommentSheetActions(
            onDismiss = commentSheet::dismiss,
            onRetry = commentSheet::retryLoad,
            onInputChange = commentSheet::updateInput,
            onSubmit = commentSheet::submit,
            onEdit = commentSheet::startEditing,
            onEditCancel = commentSheet::cancelEditing,
            onDelete = commentSheet::requestDelete,
            onDeleteCancel = commentSheet::cancelDelete,
            onDeleteConfirm = commentSheet::confirmDelete,
            onReport = commentSheet::report,
            onReportResultConsumed = commentSheet::consumeReportResult,
            onScrollToLatestConsumed = commentSheet::consumeScrollToLatest,
            onLike = commentSheet::toggleLike,
            onLikeErrorConsumed = commentSheet::consumeLikeError,
        )
    }

    ReaderScreen(
        uiState = readerViewModel.uiState,
        commentSheet = commentSheet.uiState,
        actions = actions,
        commentSheetActions = commentSheetActions,
    )
}
