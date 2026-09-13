package com.yeobaek.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import com.yeobaek.feature.reader.comment.CommentSheetActions
import com.yeobaek.feature.reader.comment.CommentSheetUiState
import com.yeobaek.feature.reader.component.CommentBottomSheet
import com.yeobaek.feature.reader.component.PassageItem
import com.yeobaek.feature.reader.component.ReaderProgressBar
import com.yeobaek.feature.reader.component.ReaderTableOfContents
import com.yeobaek.feature.reader.component.ReaderTopBar
import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    commentSheet: CommentSheetUiState?,
    actions: ReaderActions,
    commentSheetActions: CommentSheetActions,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(onBack = actions.onBackClick)

    // 본문 목록의 스크롤 상태. 초기 위치 이동, 목표 문단 이동, 위치 복원, 앞뒤 문단 요청을 맡는다.
    val readerListState = rememberReaderListState(
        uiState = uiState,
        actions = actions,
    )
    val selectedSentence = commentSheet?.let { sheet ->
        uiState.passages.findSentence(sheet.sentenceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopBar(
                title = uiState.title,
                author = uiState.author,
                fontSize = uiState.fontSize,
                isTextSettingMenuExpanded = uiState.isTextSettingMenuExpanded,
                onBackClick = actions.onBackClick,
                onTableOfContentsClick = actions.onTableOfContentsClick,
                onTextSettingClick = actions.onTextSettingClick,
                onTextSettingDismiss = actions.onTextSettingDismiss,
                onFontSizeChange = { fontSize ->
                    if (fontSize != uiState.fontSize) {
                        readerListState.savePositionBeforeFontSizeChange(uiState.passages)
                        actions.onFontSizeChange(fontSize)
                    }
                },
                modifier = Modifier.zIndex(1f),
            )
        },
        bottomBar = {
            if (uiState.loadState == ReaderLoadState.Ready) {
                ReaderProgressBar(
                    progress = uiState.displayProgress,
                    onProgressChange = actions.onProgressChange,
                    onProgressChangeFinished = actions.onProgressChangeFinished,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { innerPadding ->
        when (val loadState = uiState.loadState) {
            ReaderLoadState.Loading -> ReaderLoading(
                modifier = Modifier.padding(innerPadding),
            )

            is ReaderLoadState.Failed -> ReaderLoadError(
                message = loadState.message,
                modifier = Modifier.padding(innerPadding),
            )

            ReaderLoadState.Ready -> ReaderContent(
                passages = uiState.passages.items,
                fontSize = uiState.fontSize,
                listState = readerListState.listState,
                onSentenceClick = actions.onSentenceClick,
                modifier = Modifier
                    .padding(innerPadding)
                    .clipToBounds(),
            )
        }
    }

    if (commentSheet != null && selectedSentence != null) {
        CommentBottomSheet(
            sentence = selectedSentence,
            uiState = commentSheet,
            onDismissRequest = commentSheetActions.onDismiss,
            onInputChange = commentSheetActions.onInputChange,
            onSubmit = commentSheetActions.onSubmit,
            onEdit = commentSheetActions.onEdit,
            onEditCancel = commentSheetActions.onEditCancel,
            onReport = commentSheetActions.onReport,
            onReportResultConsumed = commentSheetActions.onReportResultConsumed,
            onDelete = commentSheetActions.onDelete,
            onDeleteCancel = commentSheetActions.onDeleteCancel,
            onDeleteConfirm = commentSheetActions.onDeleteConfirm,
        )
    }

    if (uiState.isTableOfContentsVisible) {
        ReaderTableOfContents(
            chapters = uiState.chapters,
            readingPassageSequence = uiState.readingSequence,
            onDismissRequest = actions.onTableOfContentsDismiss,
            onChapterClick = actions.onChapterClick,
        )
    }
}

// 리더 화면에서 일어나는 사용자 동작과, 본문 목록(ReaderListState)이 ViewModel에 보내는 알림.
class ReaderActions(
    val onBackClick: () -> Unit,
    val onSentenceClick: (SentenceUiModel) -> Unit,
    val onTableOfContentsClick: () -> Unit,
    val onTableOfContentsDismiss: () -> Unit,
    val onChapterClick: (ChapterUiModel) -> Unit,
    val onTextSettingClick: () -> Unit,
    val onTextSettingDismiss: () -> Unit,
    val onFontSizeChange: (Int) -> Unit,
    val onProgressChange: (Float) -> Unit,
    val onProgressChangeFinished: () -> Unit,
    val onLoadPrevious: () -> Boolean,
    val onLoadNext: () -> Unit,
    val onVisiblePassageChange: (PassageUiModel) -> Unit,
    val onTargetPassageReached: (PassageUiModel) -> Unit,
    val onTargetPassageNotFound: (Int) -> Unit,
)

@Composable
private fun ReaderLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReaderLoadError(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ReaderContent(
    passages: List<PassageUiModel>,
    fontSize: Int,
    listState: LazyListState,
    onSentenceClick: (SentenceUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        items(
            items = passages,
            key = { passage -> passage.passageId },
        ) { passage ->
            PassageItem(
                passage = passage,
                fontSize = fontSize,
                onSentenceClick = onSentenceClick,
            )
        }
    }
}

@Preview(showBackground = true, name = "전자책 리더 화면")
@Composable
private fun ReaderScreenPreview() {
    YeobaekTheme {
        ReaderScreen(
            uiState = ReaderUiState(
                loadState = ReaderLoadState.Ready,
                title = "데미안",
                author = "헤르만 헤세",
                passages = LoadedPassages(
                    items = listOf(
                        PassageUiModel(
                            passageId = 1,
                            sequence = 1,
                            chapterId = 1,
                            sentences = listOf(
                                SentenceUiModel(
                                    sentenceId = 101,
                                    sequence = 1,
                                    content =
                                        "종이를 만지작거리다 아무 생각 없이 폈는데 그 안에 몇 마디 " +
                                            "말이 적혀 있는 것을 보았다.",
                                    commentCount = 0,
                                ),
                                SentenceUiModel(
                                    sentenceId = 102,
                                    sequence = 2,
                                    content =
                                        "그 위로 시선을 한 번 던지고는 말 한마디에 사로잡혀 버렸다.",
                                    commentCount = 1,
                                ),
                            ),
                        ),
                        PassageUiModel(
                            passageId = 2,
                            sequence = 2,
                            chapterId = 1,
                            sentences = listOf(
                                SentenceUiModel(201, 1, "\"새는 알에서 나오려고 투쟁한다.", 2),
                                SentenceUiModel(202, 2, "알은 세계이다.", 0),
                                SentenceUiModel(
                                    203,
                                    3,
                                    "태어나려는 자는 하나의 세계를 깨뜨려야 한다.\"",
                                    0,
                                ),
                            ),
                        ),
                        PassageUiModel(
                            passageId = 3,
                            sequence = 3,
                            chapterId = 1,
                            sentences = listOf(
                                SentenceUiModel(
                                    301,
                                    1,
                                    "이 글줄을 몇 차례 읽은 뒤 나는 깊은 생각에 빠졌다.",
                                    1,
                                ),
                                SentenceUiModel(302, 2, "어떤 의심도 불가능했다.", 0),
                            ),
                        ),
                        PassageUiModel(
                            passageId = 4,
                            sequence = 4,
                            chapterId = 1,
                            sentences = listOf(
                                SentenceUiModel(401, 1, "내 그림을 그가 받은 것이다.", 0),
                                SentenceUiModel(
                                    402,
                                    2,
                                    "그는 이해하였고 내가 해석하는 것을 도운 것이다.",
                                    0,
                                ),
                            ),
                        ),
                        PassageUiModel(
                            passageId = 5,
                            sequence = 5,
                            chapterId = 1,
                            sentences = listOf(
                                SentenceUiModel(
                                    501,
                                    1,
                                    "수업을 조금도 듣지 못한 채 그 시간이 갔다.",
                                    3,
                                ),
                                SentenceUiModel(502, 2, "다음 시간이 시작되었다.", 0),
                            ),
                        ),
                    ),
                ),
                readingSequence = 4,
                totalPassageCount = 5,
            ),
            commentSheet = null,
            actions = ReaderActions(
                onBackClick = {},
                onSentenceClick = {},
                onTableOfContentsClick = {},
                onTableOfContentsDismiss = {},
                onChapterClick = {},
                onTextSettingClick = {},
                onTextSettingDismiss = {},
                onFontSizeChange = {},
                onProgressChange = {},
                onProgressChangeFinished = {},
                onLoadPrevious = { false },
                onLoadNext = {},
                onVisiblePassageChange = {},
                onTargetPassageReached = {},
                onTargetPassageNotFound = {},
            ),
            commentSheetActions = CommentSheetActions(
                onDismiss = {},
                onInputChange = {},
                onSubmit = {},
                onEdit = {},
                onEditCancel = {},
                onDelete = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
                onReport = {},
                onReportResultConsumed = {},
            ),
        )
    }
}
