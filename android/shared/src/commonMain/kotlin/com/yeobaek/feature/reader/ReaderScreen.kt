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
            vertical = 32.dp,
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
                passages = ReaderPreviewData.passages,
                currentSequence = 4,
                totalPassageCount = ReaderPreviewData.passages.size,
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
