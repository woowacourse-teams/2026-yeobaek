package com.yeobaek.feature.group.create

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTopAppBar
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import com.yeobaek.feature.group.create.model.SelectBookUiModel

@Composable
fun CreateScreen(
    uiState: CreateUiState,
    updateGroupNameValue: (String) -> Unit,
    selectBook: (SelectBookUiModel) -> Unit,
    onLastVisibleBookChanged: (lastVisibleIndex: Int) -> Unit,
    onNextStep: () -> Unit,
    onSelectOtherBook: () -> Unit,
    onBackClick: () -> Unit,
    onCreateGroup: () -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val bookGridState = rememberLazyGridState()

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            YeobaekTopAppBar(
                title = "모임 만들기",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            YeobaekButton(
                text = when (uiState.step) {
                    CreateStep.BookSelection -> "다음"
                    CreateStep.GroupName -> "모임 만들기"
                },
                onClick = when (uiState.step) {
                    CreateStep.BookSelection -> onNextStep
                    CreateStep.GroupName -> onCreateGroup
                },
                enabled = when (uiState.step) {
                    CreateStep.BookSelection -> uiState.bookState is BookState.Success && uiState.selectedBook != null

                    CreateStep.GroupName ->
                        uiState.createState !is CreateState.Loading &&
                            uiState.createState !is CreateState.Success
                },
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
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
                    bookState = uiState.bookState,
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
            updateGroupNameValue = {},
            selectBook = {},
            onLastVisibleBookChanged = {},
            onNextStep = {},
            onSelectOtherBook = {},
            onBackClick = {},
            onCreateGroup = {},
            navigateToHome = {},
        )
    }
}
