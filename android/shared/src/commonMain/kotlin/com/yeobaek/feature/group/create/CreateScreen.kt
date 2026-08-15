package com.yeobaek.feature.group.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTopAppBar
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.create.component.CreateBookChooseCard
import com.yeobaek.feature.group.create.component.CreateGroupNameCard

@Composable
fun CreateScreen(
    uiState: CreateUiState,
    updateGroupNameValue: (String) -> Unit,
    selectBook: (Int) -> Unit,
    onBackClick: () -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            YeobaekTopAppBar(
                title = "새로운 모임 만들기",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            YeobaekButton(
                text = "모임 생성하고 친구 초대하기",
                onClick = navigateToHome,
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            CreateGroupNameCard(
                value = uiState.groupNameValue,
                onValueChange = {
                    updateGroupNameValue(it)
                },
                placeholder = if (uiState.groupNameCondition) "제목을 입력해주세요." else "예: 일요일 아침, 함께 읽기",
                isError = uiState.groupNameCondition,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            CreateBookChooseCard(
                books = uiState.bookList,
                onClickBook = {
                    selectBook(it)
                },
                subTitle = if (uiState.selectedBookCondition) "책을 선택해주세요." else "함께 읽을 책을 선택해주세요.",
                isError = uiState.selectedBookCondition,
                isLoading = uiState.successBookLoading,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "모임 생성 화면")
@Composable
private fun CreateScreenPreview() {
    YeobaekTheme {
        CreateScreen(
            uiState = CreateUiState(),
            updateGroupNameValue = {},
            selectBook = {},
            onBackClick = {},
            navigateToHome = {},
        )
    }
}
