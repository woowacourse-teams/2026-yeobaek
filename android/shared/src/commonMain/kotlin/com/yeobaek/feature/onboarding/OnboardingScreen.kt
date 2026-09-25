package com.yeobaek.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.onboarding.component.OnboardingBookCard
import com.yeobaek.feature.onboarding.component.OnboardingBottomSheetContent
import com.yeobaek.feature.onboarding.component.OnboardingJoinCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    navigateToHome: () -> Unit,
    navigateToJoin: () -> Unit,
    onSelectBook: (Long) -> Unit,
    onDismissBottomSheet: () -> Unit,
    onClickPublicRoom: () -> Unit,
    onClickCreateRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

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
                columns = GridCells.Fixed(3),
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
                            println("onClick: ${bookUiModel.id}")
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
                    onClickPublicRoom = onClickPublicRoom,
                    onClickCreateRoom = onClickCreateRoom,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "온보딩 화면")
@Composable
private fun OnboardingScreenPreview() {
    YeobaekTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                bookUiModelList = OnboardingViewModel.mockBookList,
            ),
            navigateToHome = {},
            navigateToJoin = {},
            onSelectBook = {},
            onDismissBottomSheet = {},
            onClickPublicRoom = {},
            onClickCreateRoom = {},
        )
    }
}
