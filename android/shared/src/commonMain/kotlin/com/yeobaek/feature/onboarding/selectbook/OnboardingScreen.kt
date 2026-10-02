package com.yeobaek.feature.onboarding.selectbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.common.getGridCount
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.onboarding.selectbook.component.OnboardingBookCard
import com.yeobaek.feature.onboarding.selectbook.component.OnboardingBottomSheetContent
import com.yeobaek.feature.onboarding.selectbook.component.OnboardingJoinCard
import com.yeobaek.feature.onboarding.selectbook.model.OnboardingBookUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    navigateToHome: () -> Unit,
    navigateToJoin: () -> Unit,
    onSelectBook: (Long) -> Unit,
    onDismissBottomSheet: () -> Unit,
    onClickPublicRoom: (Long) -> Unit,
    onClickCreateRoom: (Long) -> Unit,
    closeBottomSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(uiState.initBookState) {
        when (uiState.initBookState) {
            is InitBookState.Loading -> {
                isLoading = true
            }

            is InitBookState.Success -> {
                isLoading = false
            }

            is InitBookState.Failure -> {
                isLoading = false
                snackbarHostState.showSnackbar(
                    message = (uiState.initBookState).message,
                )
            }

            is InitBookState.Idle -> return@LaunchedEffect
        }
    }

    LaunchedEffect(uiState.publicRoomState) {
        when (uiState.publicRoomState) {
            is PublicRoomState.Loading -> {
                isLoading = true
            }

            is PublicRoomState.Success -> {
                isLoading = false
                closeBottomSheet()
                navigateToHome()
            }

            is PublicRoomState.Failure -> {
                isLoading = false
                snackbarHostState.showSnackbar(
                    message = (uiState.publicRoomState).message,
                )
            }

            is PublicRoomState.Idle -> return@LaunchedEffect
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(
                        onClick = navigateToHome,
                    ) {
                        Text("건너뛰기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).padding(horizontal = 20.dp).fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("어떤 책을 읽어볼까요?", style = MaterialTheme.typography.headlineLarge)
            Text("책을 고르면 함께 읽는 방식을 선택할 수 있어요.", style = MaterialTheme.typography.bodyMedium)
            OnboardingJoinCard(
                navigateToJoin = navigateToJoin,
            )
            Text("책 목록", style = MaterialTheme.typography.headlineMedium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(getGridCount()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(uiState.bookUiModelList, key = { it.id }) { bookUiModel ->
                    OnboardingBookCard(
                        title = bookUiModel.title,
                        authors = bookUiModel.authors,
                        coverUrl = bookUiModel.coverUrl,
                        onClick = {
                            onSelectBook(bookUiModel.id)
                        },
                    )
                }
            }
        }

        if (uiState.selectedBookUiState.isSelected) {
            ModalBottomSheet(
                onDismissRequest = onDismissBottomSheet,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                OnboardingBottomSheetContent(
                    title = uiState.selectedBookUiState.selectedBook?.title ?: "제목없음",
                    onClickPublicRoom = {
                        onClickPublicRoom(uiState.selectedBookUiState.selectedBook?.id ?: 0L)
                    },
                    onClickCreateRoom = {
                        onClickCreateRoom(uiState.selectedBookUiState.selectedBook?.id ?: 0L)
                    },
                )
            }
        }
    }
    if (isLoading) {
        LoadingIndicator()
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true, name = "온보딩 화면")
@Composable
private fun OnboardingScreenPreview() {
    YeobaekTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                bookUiModelList = listOf(
                    OnboardingBookUiModel(
                        id = 0L,
                        title = "The Great Gatsby",
                        authors = "F. Scott Fitzgerald",
                        coverUrl = "https://template.canva.com/EAF8mKE9JzY/1/0/1003w-F9EPCH-Tgf0.jpg",
                    ),
                    OnboardingBookUiModel(
                        id = 1L,
                        title = "To Kill a Mockingbird",
                        authors = "Harper Lee",
                    ),
                    OnboardingBookUiModel(
                        id = 2L,
                        title = "1984",
                        authors = "George Orwell",
                    ),
                    OnboardingBookUiModel(
                        id = 3L,
                        title = "Pride and Prejudice",
                        authors = "Jane Austen",
                    ),
                    OnboardingBookUiModel(
                        id = 4L,
                        title = "The Catcher in the Rye",
                        authors = "J.D. Salinger",
                    ),
                ),
            ),
            navigateToHome = {},
            navigateToJoin = {},
            onSelectBook = {},
            onDismissBottomSheet = {},
            onClickPublicRoom = {},
            onClickCreateRoom = {},
            closeBottomSheet = {},
        )
    }
}
