package com.yeobaek.feature.group.create

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.analytics.BookListVisitTracker
import com.yeobaek.core.analytics.BookMetadataSection
import com.yeobaek.core.common.TrackBookList
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTopAppBar
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import com.yeobaek.feature.book.detail.BookDetailContent
import com.yeobaek.feature.group.create.model.SelectBookUiModel

@Composable
fun CreateScreen(
    uiState: CreateUiState,
    updateGroupNameValue: (String) -> Unit,
    updateSearchTextValue: (String) -> Unit,
    selectBook: (SelectBookUiModel) -> Unit,
    onLastVisibleBookChanged: (lastVisibleIndex: Int) -> Unit,
    onNextStep: () -> Unit,
    onRetryBookDetail: () -> Unit,
    onSelectOtherBook: () -> Unit,
    onBackClick: () -> Unit,
    onCreateGroup: () -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    bookListTracker: BookListVisitTracker? = null,
    onMetadataViewed: (BookMetadataSection) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val bookGridState = rememberLazyGridState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    PlatformBackHandler(onBack = onBackClick)

    LaunchedEffect(uiState.createState) {
        when (uiState.createState) {
            is CreateState.Success -> navigateToHome()

            is CreateState.Failure -> snackbarHostState.showSnackbar(
                message = uiState.createState.message,
                duration = SnackbarDuration.Short,
            )

            is CreateState.Loading, CreateState.Idle -> return@LaunchedEffect
        }
    }

    LaunchedEffect(uiState.searchTextValue) {
        bookGridState.scrollToItem(0)
    }

    if (uiState.step == CreateStep.BookDetail) {
        BookDetailContent(
            state = uiState.bookDetailState,
            onBackClick = onBackClick,
            onReadClick = onNextStep,
            onRetry = onRetryBookDetail,
            modifier = modifier,
            onMetadataViewed = onMetadataViewed,
        )
        return
    }

    if (uiState.step == CreateStep.BookSelection && bookListTracker != null) {
        val displayedBooks = uiState.bookList.takeIf {
            uiState.bookState is BookState.Success &&
                uiState.searchState !is SearchState.Loading &&
                uiState.searchState !is SearchState.Failure
        }
        TrackBookList(bookListTracker, bookGridState, displayedBooks?.map { it.id })
    }

    Scaffold(
        modifier = modifier.fillMaxSize().pointerInput(focusManager, keyboardController) {
            detectTapGestures {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        },
        topBar = {
            YeobaekTopAppBar(
                title = "모임 만들기",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            if (uiState.step == CreateStep.GroupName) {
                YeobaekButton(
                    text = "모임 만들기",
                    onClick = onCreateGroup,
                    enabled = uiState.createState !is CreateState.Loading &&
                        uiState.createState !is CreateState.Success,
                    modifier = Modifier.navigationBarsPadding().padding(16.dp),
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            when (uiState.step) {
                CreateStep.BookSelection -> CreateBookSelectionContent(
                    books = uiState.bookList,
                    searchTextValue = uiState.searchTextValue,
                    updateSearchTextValue = updateSearchTextValue,
                    bookState = uiState.bookState,
                    searchState = uiState.searchState,
                    selectedBookId = uiState.selectedBook?.id,
                    gridState = bookGridState,
                    onClickBook = selectBook,
                    onLastVisibleBookChanged = onLastVisibleBookChanged,
                )

                CreateStep.GroupName -> CreateGroupContent(
                    selectedBook = requireNotNull(uiState.selectedBook),
                    groupName = uiState.groupNameValue,
                    isGroupNameValid = uiState.isGroupNameValid,
                    selectOtherBook = onSelectOtherBook,
                    onValueChangeGroupName = updateGroupNameValue,
                )

                CreateStep.BookDetail -> Unit
            }
        }
    }
}

@Preview(showBackground = true, name = "모임 생성 화면")
@Composable
private fun CreateScreenPreview() {
    YeobaekTheme {
        CreateScreen(
            uiState = CreateUiState(
                bookState = BookState.Success,
                bookList = listOf(
                    SelectBookUiModel(
                        id = 1L,
                        title = "데미안",
                        authors = "헤르만 헤세",
                    ),
                ),
            ),
            updateSearchTextValue = {},
            updateGroupNameValue = {},
            selectBook = {},
            onLastVisibleBookChanged = {},
            onNextStep = {},
            onRetryBookDetail = {},
            onSelectOtherBook = {},
            onBackClick = {},
            onCreateGroup = {},
            navigateToHome = {},
        )
    }
}
