package com.yeobaek.feature.group.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.common.toClipEntry
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.component.DetailTopAppBar
import com.yeobaek.feature.group.detail.component.GroupBookCard
import com.yeobaek.feature.group.detail.component.GroupUserCard
import com.yeobaek.feature.group.detail.component.InviteCodeCard
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    detailStateHolder: DetailStateHolder = DetailStateHolder(),
    modifier: Modifier = Modifier,
) {
    val uiState by remember { mutableStateOf(detailStateHolder.uiState) }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                title = uiState.groupUiModel.name,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                YeobaekButton(
                    text = "책 이어 읽기 ->",
                    onClick = {},
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp),
                )
            }
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
                    author = uiState.bookUiModel.author,
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
    }
}

@Composable
private fun GroupBookInfoCard(
    title: String,
    author: String,
    currentProgress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text(author, style = MaterialTheme.typography.bodySmall)
        }
        Column {
            Text(
                "독서 진행률 ${(currentProgress * 100).toInt()}%",
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
                        currentProgress
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

@Preview(showBackground = true, name = "모임 상세 화면")
@Composable
private fun DetailScreenPreview() {
    YeobaekTheme {
        DetailScreen()
    }
}
