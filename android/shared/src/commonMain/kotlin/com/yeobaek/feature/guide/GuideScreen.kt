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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private const val TOTAL_PAGES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPage by remember { mutableStateOf(1) }
    var previousEnabled by remember { mutableStateOf(false) }
    var nextEnabled by remember { mutableStateOf(false) }

    var isClickJoin by remember { mutableStateOf(false) }
    var isClickCreate by remember { mutableStateOf(false) }

    var isClickCopy by remember { mutableStateOf(false) }

    var isClickCommentSentence by remember { mutableStateOf(false) }
    var isClickUnCommentSentence by remember { mutableStateOf(false) }
    var isSuccessCancel by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isClickCreate) {
        if (isClickCreate) {
            snackbarHostState.currentSnackbarData?.dismiss()

            snackbarHostState.showSnackbar(
                message = "새 모임 만들기를 눌러 모임을 만들 수 있어요!",
                duration = SnackbarDuration.Short,
            )
        }
        if (isClickCreate && isClickJoin) {
            nextEnabled = true
        }
    }

    LaunchedEffect(isClickJoin) {
        if (isClickJoin) {
            snackbarHostState.currentSnackbarData?.dismiss()

            snackbarHostState.showSnackbar(
                message = "모임 참여하기를 눌러 모임에 참여할 수 있어요!",
                duration = SnackbarDuration.Short,
            )
        }
        if (isClickJoin && isClickCreate) {
            nextEnabled = true
        }
    }

    LaunchedEffect(isClickCopy) {
        if (isClickCopy) {
            nextEnabled = true

            snackbarHostState.showSnackbar(
                message = "초대 코드를 복사할 수 있어요!",
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(isClickCommentSentence) {
        snackbarHostState.currentSnackbarData?.dismiss()
        delay(50.milliseconds)
        if (isClickCommentSentence) {
            snackbarHostState.showSnackbar(
                message = "댓글창 뒤의 배경을 눌러 댓글창을 닫을 수 있어요!",
                duration = SnackbarDuration.Short,
            )
            isClickCommentSentence = false
        }
    }

    LaunchedEffect(isClickUnCommentSentence) {
        snackbarHostState.currentSnackbarData?.dismiss()
        delay(50.milliseconds)
        if (isClickUnCommentSentence) {
            snackbarHostState.showSnackbar(
                message = "밑줄이 있는 문장을 눌러 다른 사람의 생각을 읽어봐요!",
                duration = SnackbarDuration.Short,
            )
            isClickUnCommentSentence = false
        }
    }

    LaunchedEffect(isSuccessCancel) {
        if (isSuccessCancel) {
            nextEnabled = true
        }
    }

    LaunchedEffect(currentPage) {
        nextEnabled = false
        when (currentPage) {
            1 -> {
                isClickJoin = false
                isClickCreate = false
            }

            2 -> {
                isClickCopy = false
            }

            3 -> {
                isSuccessCancel = false
            }

            else -> {
                navigateToHome()
            }
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
                modifier = Modifier.navigationBarsPadding().padding(10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (previousEnabled) {
                    YeobaekButton(
                        onClick = {
                            if (currentPage > 1) currentPage -= 1
                            if (currentPage == 1) previousEnabled = false
                            if (currentPage < TOTAL_PAGES) nextEnabled = true
                        },
                        text = "이전",
                        modifier = Modifier.weight(1f),
                        enabled = previousEnabled,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f))
                }
                YeobaekButton(
                    onClick = {
                        currentPage += 1
                        if (currentPage > 1) previousEnabled = true
                        if (currentPage == TOTAL_PAGES) nextEnabled = false

                        snackbarHostState.currentSnackbarData?.dismiss()
                    },
                    text = if (currentPage == TOTAL_PAGES) "여백 시작하기" else "다음",
                    modifier = Modifier.weight(1f),
                    enabled = nextEnabled,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).padding(horizontal = 20.dp).fillMaxSize(),
        ) {
            Text("${minOf(currentPage, TOTAL_PAGES)} / $TOTAL_PAGES")
            when (currentPage) {
                1 -> HomeGuideCard(
                    onClickJoin = {
                        isClickJoin = true
                    },
                    onClickCreate = {
                        isClickCreate = true
                    },
                    isJoinEnabled = !isClickJoin,
                    isCreateEnabled = !isClickCreate,
                    modifier = Modifier.weight(1f),
                )

                2 -> GroupDetailGuideCard(
                    onClickCopy = {
                        isClickCopy = true
                    },
                    enabled = !isClickCopy,
                    modifier = Modifier.weight(1f),
                )

                3 -> ReaderGuideCard(
                    onClickCommentSentence = {
                        isClickCommentSentence = true
                    },
                    onClickUnCommentSentence = {
                        isClickUnCommentSentence = true
                    },
                    onCancel = {
                        isSuccessCancel = true
                    },
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
        )
    }
}
