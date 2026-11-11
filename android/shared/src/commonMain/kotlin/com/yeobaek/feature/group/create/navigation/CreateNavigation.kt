package com.yeobaek.feature.group.create.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.yeobaek.core.analytics.BookDetailEntryPoint
import com.yeobaek.core.analytics.BookListEnd
import com.yeobaek.core.analytics.BookSearchEnd
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupCreateSubmitted
import com.yeobaek.core.analytics.InvalidReason
import com.yeobaek.core.analytics.bookDetailScreenProperties
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.common.TrackBookSearch
import com.yeobaek.core.common.TrackScreen
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.feature.group.create.CreateScreen
import com.yeobaek.feature.group.create.CreateStep
import com.yeobaek.feature.group.create.CreateViewModel
import com.yeobaek.feature.group.create.SearchState
import kotlinx.serialization.Serializable

@Serializable
data object Create

fun NavController.navigateToCreate(
    navOptions: NavOptions? = null,
) {
    navigate(
        route = Create,
        navOptions = navOptions,
    )
}

fun NavGraphBuilder.createNavGraph(
    appContainer: AppContainer,
    navigateToBack: (entryId: String, beforePop: () -> Unit) -> Unit,
    navigateToHome: () -> Unit,
) {
    composable<Create> { entry ->
        val createViewModel: CreateViewModel = viewModel(
            factory = CreateViewModel.createViewModelFactory(
                groupRepository = appContainer.groupRepository,
                bookRepository = appContainer.bookRepository,
                crashReporter = appContainer.crashReporter,
                analyticsTracker = appContainer.analyticsTracker,
            ),
        )

        val uiState = createViewModel.uiState
        val detailBookId = uiState.selectedBook?.id
        val isBookDetailVisible = uiState.step == CreateStep.BookDetail && detailBookId != null
        TrackScreen(
            crashReporter = appContainer.crashReporter,
            analyticsTracker = appContainer.analyticsTracker,
            screen = if (isBookDetailVisible) TrackedScreen.BOOK_DETAIL else TrackedScreen.GROUP_CREATE,
            properties = if (isBookDetailVisible) {
                bookDetailScreenProperties(
                    bookId = requireNotNull(detailBookId),
                    entryPoint = BookDetailEntryPoint.GROUP_CREATE.value,
                    detailVisitId = createViewModel.detailVisit.id,
                )
            } else {
                emptyMap()
            },
        )
        TrackBookSearch(
            tracker = createViewModel.bookSearchTracker,
            isVisible = uiState.step == CreateStep.BookSelection,
            resultVersion = uiState.searchResultVersion,
            resultCount = uiState.bookList.size.takeIf { uiState.searchState is SearchState.Success },
        )

        CreateScreen(
            uiState = createViewModel.uiState,
            updateSearchTextValue = createViewModel::updateSearchTextValue,
            updateGroupNameValue = createViewModel::updateGroupNameValue,
            selectBook = createViewModel::selectBook,
            onLastVisibleBookChanged = createViewModel::onLastVisibleBookChanged,
            onNextStep = createViewModel::moveToGroupName,
            bookListTracker = createViewModel.bookListTracker,
            onMetadataViewed = createViewModel::onMetadataViewed,
            onRetryBookDetail = createViewModel::retryBookDetail,
            onSelectOtherBook = createViewModel::moveToBookSelection,
            onBackClick = back@{
                if (uiState.step != createViewModel.uiState.step) return@back
                when (uiState.step) {
                    CreateStep.GroupName -> createViewModel.moveToBookDetail()

                    CreateStep.BookDetail -> createViewModel.moveToBookSelection()

                    CreateStep.BookSelection -> {
                        navigateToBack(entry.id) {
                            createViewModel.bookSearchTracker.close(BookSearchEnd.NAVIGATION)
                            createViewModel.bookListTracker.close(BookListEnd.NAVIGATION)
                            createViewModel.abandon()
                        }
                    }
                }
            },
            onCreateGroup = {
                if (createViewModel.createConditionCheck()) {
                    val reason = if (!createViewModel.uiState.isGroupNameValid) {
                        InvalidReason.NAME_BLANK
                    } else {
                        InvalidReason.BOOK_NOT_SELECTED
                    }
                    appContainer.analyticsTracker.track(
                        GroupCreateSubmitted(
                            result = EventResult.INVALID,
                            attemptId = createViewModel.attemptId,
                            reason = reason,
                            bookList = createViewModel.bookListExposure(),
                        ),
                    )
                } else {
                    createViewModel.createGroup()
                }
            },
            navigateToHome = navigateToHome,
        )
    }
}
