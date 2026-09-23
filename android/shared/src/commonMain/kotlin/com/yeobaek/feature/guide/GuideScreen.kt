package com.yeobaek.feature.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.guide.component.group.detail.GroupDetailGuideCard
import com.yeobaek.feature.guide.component.home.HomeGuideCard
import com.yeobaek.feature.guide.component.reader.ReaderGuideCard
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    uiState: GuideUiState,
    onCurrentPage: (() -> Unit) -> Unit,
    onSuccessGuide: () -> Unit,
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    isLast: Boolean,
    currentPageText: String,
    onClickCommentSentence: () -> Unit,
    onClickUnCommentSentence: () -> Unit,
    onCancel: () -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isClickCommentSentence) {
        if (uiState.isClickCommentSentence) {
            snackbarHostState.currentSnackbarData?.dismiss()
            delay(50.milliseconds)
            snackbarHostState.showSnackbar(
                message = "댓글창 뒤의 배경을 눌러 댓글창을 닫을 수 있어요!",
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(uiState.isClickUnCommentSentence) {
        if (uiState.isClickUnCommentSentence) {
            snackbarHostState.currentSnackbarData?.dismiss()
            delay(50.milliseconds)
            snackbarHostState.showSnackbar(
                message = "밑줄이 있는 문장을 클릭해보세요!",
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(uiState.isSuccessGuide) {
        onSuccessGuide()
    }

    LaunchedEffect(uiState.currentPage) {
        onCurrentPage {
            navigateToHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("여백 사용법")
                },
                actions = {
                    TextButton(
                        onClick = {
                            navigateToHome()
                        },
                    ) {
                        Text("건너뛰기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.previousEnabled) {
                    YeobaekButton(
                        onClick = {
                            onClickPrevious()
                        },
                        text = "이전",
                        modifier = Modifier.weight(1f),
                        enabled = uiState.previousEnabled,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f))
                }
                YeobaekButton(
                    onClick = {
                        onClickNext()
                    },
                    text = if (isLast) "여백 시작하기" else "다음",
                    modifier = Modifier.weight(1f),
                    enabled = uiState.nextEnabled,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).padding(horizontal = 16.dp).fillMaxSize(),
        ) {
            Text(currentPageText, color = MaterialTheme.colorScheme.secondary)
            when (uiState.currentPage) {
                1 -> HomeGuideCard(
                    modifier = Modifier.weight(1f),
                )

                2 -> GroupDetailGuideCard(
                    onClickCopy = {},
                    modifier = Modifier.weight(1f),
                )

                3 -> ReaderGuideCard(
                    sentences = uiState.sentences,
                    onClickCommentSentence = {
                        onClickCommentSentence()
                    },
                    onClickUnCommentSentence = {
                        onClickUnCommentSentence()
                    },
                    onCancel = {
                        onCancel()
                    },
                    sentenceEnabled = uiState.isClickUnCommentSentence,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "가이드화면")
@Composable
private fun GuideScreenPreview() {
    YeobaekTheme {
        GuideScreen(
            navigateToHome = {},
            uiState = GuideUiState(),
            onCurrentPage = {},
            onSuccessGuide = {},
            onClickPrevious = {},
            onClickNext = {},
            isLast = false,
            currentPageText = "1/3",
            onClickCommentSentence = {},
            onClickUnCommentSentence = {},
            onCancel = {},
        )
    }
}
