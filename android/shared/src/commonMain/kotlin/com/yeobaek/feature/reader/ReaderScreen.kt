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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import com.yeobaek.feature.reader.component.PassageCommentBottomSheet
import com.yeobaek.feature.reader.component.PassageItem
import com.yeobaek.feature.reader.component.ReaderProgressBar
import com.yeobaek.feature.reader.component.ReaderTableOfContents
import com.yeobaek.feature.reader.component.ReaderTopBar
import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.PassageUiModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onPassageClick: (PassageUiModel) -> Unit,
    onBackClick: () -> Unit,
    onTableOfContentsClick: () -> Unit,
    onTableOfContentsDismiss: () -> Unit,
    onChapterClick: (ChapterUiModel) -> Unit,
    onTextSettingClick: () -> Unit,
    onTextSettingDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onCommentSheetDismiss: () -> Unit,
    onCommentInputChange: (String) -> Unit,
    onCommentSubmit: () -> Unit,
    onCommentEdit: (Long) -> Unit,
    onCommentEditCancel: () -> Unit,
    onCommentDelete: (Long) -> Unit,
    onCommentDeleteCancel: () -> Unit,
    onCommentDeleteConfirm: () -> Unit,
    onLoadPrevious: () -> Boolean,
    onLoadNext: () -> Unit,
    onVisiblePassageChange: (PassageUiModel) -> Unit,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    onProgressSeekCompleted: (PassageUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(onBack = onBackClick)

    val listState = rememberLazyListState()
    var hasPositionedInitialPassage by remember { mutableStateOf(false) }
    var previousLoadAnchor by remember { mutableStateOf<PassageAnchor?>(null) }
    var fontSizeAnchor by remember { mutableStateOf<PassageAnchor?>(null) }
    val currentUiState by rememberUpdatedState(uiState)
    val currentOnLoadPrevious by rememberUpdatedState(onLoadPrevious)
    val currentOnLoadNext by rememberUpdatedState(onLoadNext)
    val currentOnFontSizeChange by rememberUpdatedState(onFontSizeChange)
    val currentOnVisiblePassageChange by rememberUpdatedState(onVisiblePassageChange)
    val currentOnProgressSeekCompleted by rememberUpdatedState(onProgressSeekCompleted)
    val commentSheet = uiState.commentSheet

    LaunchedEffect(
        uiState.passages,
        uiState.currentSequence,
        uiState.isLoading,
        uiState.loadErrorMessage,
    ) {
        if (!hasPositionedInitialPassage && !uiState.isLoading && uiState.loadErrorMessage == null) {
            val currentPassageIndex = uiState.passages.indexOfFirst { passage ->
                passage.sequence == uiState.currentSequence
            }
            if (currentPassageIndex >= 0) {
                listState.scrollToItem(currentPassageIndex)
            }
            hasPositionedInitialPassage = true
        }
    }

    LaunchedEffect(
        uiState.seekTargetSequence,
        uiState.passages,
    ) {
        val targetSequence = uiState.seekTargetSequence ?: return@LaunchedEffect
        val targetIndex = uiState.passages.indexOfFirst { passage ->
            passage.sequence == targetSequence
        }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            currentOnProgressSeekCompleted(uiState.passages[targetIndex])
        }
    }

    LaunchedEffect(uiState.fontSize) {
        val anchor = fontSizeAnchor
        if (anchor != null) {
            val anchorIndex = uiState.passages.indexOfFirst { passage ->
                passage.passageId == anchor.passageId
            }
            if (anchorIndex >= 0) {
                listState.scrollToItem(anchorIndex)
                val itemSize = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { item -> item.index == anchorIndex }
                    ?.size
                    ?: 0
                listState.scrollToItem(
                    index = anchorIndex,
                    scrollOffset = anchor.scrollOffset.coerceAtMost(
                        maximumValue = (itemSize - 1).coerceAtLeast(0),
                    ),
                )
            }
            fontSizeAnchor = null
        }
    }

    LaunchedEffect(hasPositionedInitialPassage, listState) {
        if (!hasPositionedInitialPassage) return@LaunchedEffect

        snapshotFlow {
            val state = currentUiState
            val layoutInfo = listState.layoutInfo
            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
            val firstVisiblePassage = firstVisibleItem?.let { item ->
                state.passages.getOrNull(item.index)
            }
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val lastVisiblePassage = lastVisibleItem?.let { item ->
                state.passages.getOrNull(item.index)
            }
            val isLastPassageFullyVisible = lastVisibleItem != null &&
                lastVisiblePassage?.sequence == state.totalPassageCount &&
                lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset
            val passage = if (isLastPassageFullyVisible) {
                lastVisiblePassage
            } else {
                firstVisiblePassage
            }
            passage to (
                state.isLoadingPrevious ||
                    previousLoadAnchor != null ||
                    fontSizeAnchor != null ||
                    state.isProgressDragging ||
                    state.isSeeking
                )
        }.distinctUntilChanged().collect { (passage, isRestoringPosition) ->
            if (!isRestoringPosition && passage != null) {
                currentOnVisiblePassageChange(passage)
            }
        }
    }

    LaunchedEffect(
        uiState.isLoadingPrevious,
        uiState.passages.firstOrNull()?.passageId,
    ) {
        val anchor = previousLoadAnchor
        if (anchor != null && !uiState.isLoadingPrevious) {
            val anchorIndex = uiState.passages.indexOfFirst { passage ->
                passage.passageId == anchor.passageId
            }
            if (anchorIndex >= 0) {
                listState.scrollToItem(
                    index = anchorIndex,
                    scrollOffset = anchor.scrollOffset,
                )
            }
            previousLoadAnchor = null
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                false to false
            } else {
                val lastItemIndex = listState.layoutInfo.totalItemsCount - 1
                val isNearStart = visibleItems.first().index <= PAGINATION_THRESHOLD
                val isNearEnd = visibleItems.last().index >= lastItemIndex - PAGINATION_THRESHOLD
                isNearStart to isNearEnd
            }
        }.distinctUntilChanged().collect { (isNearStart, isNearEnd) ->
            val state = currentUiState
            if (
                !hasPositionedInitialPassage ||
                state.isLoading ||
                state.isProgressDragging ||
                state.isSeeking ||
                state.loadErrorMessage != null
            ) {
                return@collect
            }

            if (
                isNearStart &&
                !state.isLoadingPrevious &&
                state.passages.firstOrNull()?.sequence != FIRST_PASSAGE_SEQUENCE &&
                previousLoadAnchor == null
            ) {
                val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                val firstVisiblePassage = firstVisibleItem?.let { item ->
                    state.passages.getOrNull(item.index)
                }
                if (firstVisibleItem != null && firstVisiblePassage != null) {
                    previousLoadAnchor = previousLoadAnchorAfterRequest(
                        anchor = PassageAnchor(
                            passageId = firstVisiblePassage.passageId,
                            scrollOffset = listState.firstVisibleItemScrollOffset,
                        ),
                        requestStarted = currentOnLoadPrevious(),
                    )
                }
            }

            if (
                isNearEnd &&
                !state.isLoadingNext &&
                state.passages.lastOrNull()?.sequence != state.totalPassageCount
            ) {
                currentOnLoadNext()
            }
        }
    }

    val selectedPassage = commentSheet?.let { sheet ->
        uiState.passages.firstOrNull { passage ->
            passage.passageId == sheet.passageId
        }
    }
    val preservePositionAndChangeFontSize: (Int) -> Unit = { fontSize ->
        if (fontSize != uiState.fontSize) {
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val firstVisiblePassage = firstVisibleItem?.let { item ->
                uiState.passages.getOrNull(item.index)
            }
            if (firstVisiblePassage != null) {
                fontSizeAnchor = PassageAnchor(
                    passageId = firstVisiblePassage.passageId,
                    scrollOffset = listState.firstVisibleItemScrollOffset,
                )
            }
            currentOnFontSizeChange(fontSize)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopBar(
                title = uiState.title,
                author = uiState.author,
                fontSize = uiState.fontSize,
                isTextSettingMenuExpanded = uiState.isTextSettingMenuExpanded,
                onBackClick = onBackClick,
                onTableOfContentsClick = onTableOfContentsClick,
                onTextSettingClick = onTextSettingClick,
                onTextSettingDismiss = onTextSettingDismiss,
                onFontSizeChange = preservePositionAndChangeFontSize,
                modifier = Modifier.zIndex(1f),
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.loadErrorMessage == null) {
                ReaderProgressBar(
                    progress = uiState.displayProgress,
                    onProgressChange = onProgressChange,
                    onProgressChangeFinished = onProgressChangeFinished,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> ReaderLoading(
                modifier = Modifier.padding(innerPadding),
            )

            uiState.loadErrorMessage != null -> ReaderLoadError(
                message = uiState.loadErrorMessage,
                modifier = Modifier.padding(innerPadding),
            )

            else -> ReaderContent(
                passages = uiState.passages,
                fontSize = uiState.fontSize,
                listState = listState,
                onPassageClick = onPassageClick,
                modifier = Modifier
                    .padding(innerPadding)
                    .clipToBounds(),
            )
        }
    }

    if (commentSheet != null && selectedPassage != null) {
        PassageCommentBottomSheet(
            passage = selectedPassage,
            uiState = commentSheet,
            onDismissRequest = onCommentSheetDismiss,
            onInputChange = onCommentInputChange,
            onSubmit = onCommentSubmit,
            onEditComment = onCommentEdit,
            onCancelEdit = onCommentEditCancel,
            onDeleteComment = onCommentDelete,
            onCancelDelete = onCommentDeleteCancel,
            onConfirmDelete = onCommentDeleteConfirm,
        )
    }

    if (uiState.isTableOfContentsVisible) {
        ReaderTableOfContents(
            chapters = uiState.chapters,
            currentPassageSequence = uiState.currentSequence,
            onDismissRequest = onTableOfContentsDismiss,
            onChapterClick = onChapterClick,
        )
    }
}

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
    onPassageClick: (PassageUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        items(
            items = passages,
            key = { passage -> passage.passageId },
        ) { passage ->
            PassageItem(
                passage = passage,
                fontSize = fontSize,
                onClick = {
                    onPassageClick(passage)
                },
                showUnderline = passage.hasComment,
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
                title = "데미안",
                author = "헤르만 헤세",
                passages = listOf(
                    PassageUiModel(
                        passageId = 1,
                        sequence = 1,
                        chapterId = 1,
                        content =
                            "종이를 만지작거리다 아무 생각 없이 폈는데 그 안에 몇 마디 말이 적혀 " +
                                "있는 것을 보았다. 그 위로 시선을 한 번 던지고는 말 한마디에 " +
                                "사로잡혀 버렸다. 나는 놀라 읽었다. 그사이 나의 가슴은 운명 앞에서 " +
                                "큰 추위가 닥친 듯 오그라들었다.",
                        commentCount = 0,
                    ),
                    PassageUiModel(
                        passageId = 2,
                        sequence = 2,
                        chapterId = 1,
                        content =
                            "\"새는 알에서 나오려고 투쟁한다. 알은 세계이다. 태어나려는 자는 하나의 " +
                                "세계를 깨뜨려야 한다. 새는 신에게로 날아간다. 신의 이름은 아브락사스.\"",
                        commentCount = 2,
                    ),
                    PassageUiModel(
                        passageId = 3,
                        sequence = 3,
                        chapterId = 1,
                        content =
                            "이 글줄을 몇 차례 읽은 뒤 나는 깊은 생각에 빠졌다. 어떤 의심도 불가능했다. " +
                                "이것은 데미안이 보낸 답장이었다. 나와 그 말고 그 새에 대해 아는 " +
                                "사람은 있을 수 없었다.",
                        commentCount = 1,
                    ),
                    PassageUiModel(
                        passageId = 4,
                        sequence = 4,
                        chapterId = 1,
                        content =
                            "내 그림을 그가 받은 것이다. 그는 이해하였고 내가 해석하는 것을 도운 것이다. " +
                                "하지만 이 모든 것이 서로 무슨 관련이 있단 말인가? 그리고 무엇보다 " +
                                "나를 괴롭힌 것은 아브락사스란 무엇인가 하는 의문이었다. 들어 본 적도 " +
                                "읽어 본 적도 없는 말이었다. \"신의 이름은 아브락사스!\"",
                        commentCount = 0,
                    ),
                    PassageUiModel(
                        passageId = 5,
                        sequence = 5,
                        chapterId = 1,
                        content =
                            "수업을 조금도 듣지 못한 채 그 시간이 갔다. 다음 시간이 시작되었다. " +
                                "오전의 마지막 수업이었다. 그 시간은 젊은 보조 선생님 담당이었다. " +
                                "대학을 갓 졸업했는데, 그렇게 젊다는 것 그리고 우리에게 거짓 품위를 " +
                                "보이려 들지 않았다는 것만으로도 벌써 우리의 호감을 산 분이었다.",
                        commentCount = 3,
                    ),
                ),
                currentSequence = 4,
                totalPassageCount = 5,
            ),
            onPassageClick = {},
            onBackClick = {},
            onTableOfContentsClick = {},
            onTableOfContentsDismiss = {},
            onChapterClick = {},
            onTextSettingClick = {},
            onTextSettingDismiss = {},
            onFontSizeChange = {},
            onCommentSheetDismiss = {},
            onCommentInputChange = {},
            onCommentSubmit = {},
            onCommentEdit = {},
            onCommentEditCancel = {},
            onCommentDelete = {},
            onCommentDeleteCancel = {},
            onCommentDeleteConfirm = {},
            onLoadPrevious = { false },
            onLoadNext = {},
            onVisiblePassageChange = {},
            onProgressChange = {},
            onProgressChangeFinished = {},
            onProgressSeekCompleted = {},
        )
    }
}

internal data class PassageAnchor(
    val passageId: Long,
    val scrollOffset: Int,
)

internal fun previousLoadAnchorAfterRequest(
    anchor: PassageAnchor,
    requestStarted: Boolean,
): PassageAnchor? = anchor.takeIf { requestStarted }

private const val FIRST_PASSAGE_SEQUENCE = 1
private const val PAGINATION_THRESHOLD = 5
