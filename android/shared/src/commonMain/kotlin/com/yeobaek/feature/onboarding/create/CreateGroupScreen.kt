package com.yeobaek.feature.onboarding.create

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.onboarding.create.component.GroupNameTextField
import com.yeobaek.feature.onboarding.create.component.SelectBookCard
import com.yeobaek.feature.onboarding.create.model.SelectBookUiModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onClickBack: () -> Unit,
    selectOtherBook: () -> Unit,
    onValueChangeGroupName: (String) -> Unit,
    onClickCreateGroup: () -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.createGroupState) {
        when (uiState.createGroupState) {
            is CreateGroupState.Success -> {
                isLoading = false
                navigateToHome()
            }

            is CreateGroupState.Failure -> {
                isLoading = false
                snackbarHostState.showSnackbar(
                    message = uiState.createGroupState.message,
                )
            }

            is CreateGroupState.Loading -> {
                isLoading = true
            }

            else -> return@LaunchedEffect
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("모임 만들기")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClickBack,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back_arrow),
                            contentDescription = "뒤로가기 버튼",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("모임 이름을 정해주세요", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "선택한 \"${uiState.selectBookUiModel.title}\"(으)로 함께 읽을 모임을 만들어요.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                SelectBookCard(
                    title = uiState.selectBookUiModel.title,
                    authors = uiState.selectBookUiModel.authors,
                    coverUrl = uiState.selectBookUiModel.coverUrl,
                    selectOtherBook = selectOtherBook,
                )
                GroupNameTextField(
                    value = uiState.groupName,
                    onValueChange = onValueChangeGroupName,
                    isError = !uiState.isGroupNameValid,
                )
            }
            YeobaekButton(
                text = "모임 만들기",
                onClick = onClickCreateGroup,
                enabled = uiState.isGroupNameValid && uiState.createGroupState !is CreateGroupState.Success,
            )
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

@Preview(showBackground = true, name = "온보딩 모임 생성 화면")
@Composable
private fun CreateGroupScreenPreview() {
    YeobaekTheme {
        CreateGroupScreen(
            uiState = CreateGroupUiState(
                selectBookUiModel = SelectBookUiModel(
                    id = 0L,
                    title = "The Great Gatsby",
                    authors = "F. Scott Fitzgerald",
                ),
            ),
            onClickBack = {},
            selectOtherBook = {},
            onValueChangeGroupName = {},
            onClickCreateGroup = {},
            navigateToHome = {},
        )
    }
}
