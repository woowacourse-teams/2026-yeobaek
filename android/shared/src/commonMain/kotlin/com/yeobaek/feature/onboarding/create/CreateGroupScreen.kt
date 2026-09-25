package com.yeobaek.feature.onboarding.create

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.BookCoverImage
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onClickBack: () -> Unit,
    selectOtherBook: () -> Unit,
    onValueChangeGroupName: (String) -> Unit,
    onClickCreateGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    Text("선택한 데미안으로 함께 읽을 모임을 만들어요.", style = MaterialTheme.typography.bodyMedium)
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
}

@Composable
private fun SelectBookCard(
    title: String,
    authors: String,
    coverUrl: String?,
    selectOtherBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text("선택한 책", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors().copy(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary),
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                BookCoverImage(
                    imageUrl = coverUrl,
                    modifier = Modifier.height(100.dp),
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceAround,
                ) {
                    Text(title)
                    Text(authors)
                    Text("")
                }
                TextButton(
                    onClick = selectOtherBook,
                ) {
                    Text("다른 책 고르기")
                }
            }
        }
    }
}

@Composable
private fun GroupNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text("모임 이름", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value.take(20),
                onValueChange = {
                    onValueChange(it)
                },
                isError = isError,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                    disabledBorderColor = MaterialTheme.colorScheme.secondary,
                ),
                placeholder = {
                    Text("모임 이름을 입력해주세요")
                },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isError) "모임 이름이 공백이면 안됩니다!" else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.error,
                    ),
                )
                Text(
                    text = "${value.length}/20",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "온보딩 모임 생성 화면")
@Composable
private fun CreateGroupScreenPreview() {
    YeobaekTheme {
        CreateGroupScreen(
            uiState = CreateGroupUiState(),
            onClickBack = {},
            selectOtherBook = {},
            onValueChangeGroupName = {},
            onClickCreateGroup = {},
        )
    }
}
