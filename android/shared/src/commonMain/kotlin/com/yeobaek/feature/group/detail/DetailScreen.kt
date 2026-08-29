package com.yeobaek.feature.group.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yeobaek.core.common.ScreenState
import com.yeobaek.core.common.shouldCopySnackbar
import com.yeobaek.core.common.toClipEntry
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.noRippleClickable
import com.yeobaek.core.designsystem.theme.YeobaekError
import com.yeobaek.core.designsystem.theme.YeobaekNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekOnError
import com.yeobaek.core.designsystem.theme.YeobaekOnNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.component.DetailTopAppBar
import com.yeobaek.feature.group.detail.component.GroupBookCard
import com.yeobaek.feature.group.detail.component.GroupUserCard
import com.yeobaek.feature.group.detail.component.InviteCodeCard
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBackClick: () -> Unit,
    onReadClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

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

@Composable
private fun GroupBookInfoCard(
    title: String,
    authors: List<String>,
    currentProgress: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text(authors.joinToString(), style = MaterialTheme.typography.bodySmall)
        }
        Column {
            Text(
                "독서 진행률 $currentProgress%",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = {
                        currentProgress.toFloat() / 100
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    strokeCap = StrokeCap.Butt,
                    drawStopIndicator = {},
                    modifier = Modifier.clip(shape = RoundedCornerShape(45.dp)).weight(1f),
                    gapSize = 0.dp,
                )
            }
        }
    }
}

@Composable
fun DeleteGroupDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(244.dp),
            shape = RoundedCornerShape(15.dp),
            color = YeobaekSurfaceElevated,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = 16.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "그룹에서 나갈까요?",
                    color = YeobaekTextStrong,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(17.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeleteDialogButton(
                        text = "취소",
                        containerColor = YeobaekNeutralContainer,
                        contentColor = YeobaekOnNeutralContainer,
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                    DeleteDialogButton(
                        text = "나가기",
                        containerColor = YeobaekError,
                        contentColor = YeobaekOnError,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteDialogButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.5.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(10.dp),
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
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
        )
    }
}
