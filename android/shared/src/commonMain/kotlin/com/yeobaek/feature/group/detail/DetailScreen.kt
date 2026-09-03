package com.yeobaek.feature.group.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.common.shouldCopySnackbar
import com.yeobaek.core.common.toClipEntry
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.component.DeleteGroupDialog
import com.yeobaek.feature.group.detail.component.DetailTopAppBar
import com.yeobaek.feature.group.detail.component.GroupBookCard
import com.yeobaek.feature.group.detail.component.GroupBookInfoCard
import com.yeobaek.feature.group.detail.component.GroupUserCard
import com.yeobaek.feature.group.detail.component.InviteCodeCard
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBackClick: () -> Unit,
    onReadClick: () -> Unit,
    onExitClick: () -> Unit,
    onBlockUser: (otherUserId: Int) -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exitState) {
        when (uiState.exitState) {
            is ExitState.Success -> {
                navigateToHome()
            }

            is ExitState.Failure -> {
                snackbarHostState.showSnackbar(
                    message = uiState.exitState.message,
                    duration = SnackbarDuration.Short,
                )
            }

            is ExitState.Idle, ExitState.Loading -> return@LaunchedEffect
        }
    }

    LaunchedEffect(uiState.blockState) {
        when (uiState.blockState) {
            is BlockState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "사용자를 차단했습니다.",
                    duration = SnackbarDuration.Short,
                )
            }

            is BlockState.Failure -> {
                snackbarHostState.showSnackbar(
                    message = uiState.blockState.message,
                    duration = SnackbarDuration.Short,
                )
            }

            is BlockState.Idle, BlockState.Loading -> return@LaunchedEffect
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = when (uiState.screenState) {
                    is ScreenState.Success -> uiState.groupUiModel.name
                    is ScreenState.Loading -> uiState.screenState.message
                    is ScreenState.Error -> uiState.screenState.message
                },
                onBackClick = onBackClick,
                onExitClick = {
                    showDeleteDialog = true
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                YeobaekButton(
                    text = "책 이어 읽기 ->",
                    onClick = onReadClick,
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp),
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            // 책 정보
            GroupBookCard(
                uri = uiState.bookUiModel.uri,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                GroupBookInfoCard(
                    title = uiState.bookUiModel.title,
                    authors = uiState.bookUiModel.author,
                    currentProgress = uiState.bookUiModel.currentProgress,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            // 초대 코드 컴포넌트
            InviteCodeCard(
                groupCode = uiState.groupUiModel.groupCode,
                onClick = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(uiState.groupUiModel.groupCode.toClipEntry())
                        snackbarHostState.currentSnackbarData?.dismiss()

                        if (shouldCopySnackbar()) {
                            snackbarHostState.showSnackbar(
                                message = "초대 코드를 복사했습니다.",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            // 모임에 참여한 사람들
            GroupUserCard(
                users = uiState.groupUiModel.users,
                onBlockUser = onBlockUser,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (showDeleteDialog) {
            DeleteGroupDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    onExitClick()
                },
            )
        }
    }
}

@Preview(showBackground = true, name = "모임 상세 화면")
@Composable
private fun DetailScreenPreview() {
    YeobaekTheme {
        DetailScreen(
            uiState = DetailUiState(),
            onBackClick = {},
            onReadClick = {},
            onExitClick = {},
            onBlockUser = {},
            navigateToHome = {},
        )
    }
}
