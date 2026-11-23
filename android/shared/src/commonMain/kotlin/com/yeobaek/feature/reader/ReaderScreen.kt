package com.yeobaek.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yeobaek.core.analytics.CommentCollectionAction
import com.yeobaek.core.analytics.CommentPanelAction
import com.yeobaek.core.analytics.CommentPresentation
import com.yeobaek.core.analytics.CommentSentenceRevealTrigger
import com.yeobaek.core.analytics.CommentSheetSource
import com.yeobaek.core.common.isLandscape
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import com.yeobaek.data.model.CommentedSentenceFilter
import com.yeobaek.feature.reader.component.CommentBottomSheet
import com.yeobaek.feature.reader.component.CommentCollectionContents
import com.yeobaek.feature.reader.component.DeleteCommentDialog
import com.yeobaek.feature.reader.component.PassageItem
import com.yeobaek.feature.reader.component.ReaderCommentPanel
import com.yeobaek.feature.reader.component.ReaderCommentPanelReopenButton
import com.yeobaek.feature.reader.component.ReaderProgressBar
import com.yeobaek.feature.reader.component.ReaderReturnButton
import com.yeobaek.feature.reader.component.ReaderTableOfContents
import com.yeobaek.feature.reader.component.ReaderTopBar
import com.yeobaek.feature.reader.model.ChapterUiModel
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel

private val ReaderHorizontalPadding = 36.dp

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    commentSheet: CommentSheetUiState?,
    actions: ReaderActions,
    commentSheetActions: CommentSheetActions,
    isCommentLikeEnabled: Boolean,
    modifier: Modifier = Modifier,
    panelThreads: Map<Long, ReaderPanelThread> = emptyMap(),
    panelLikeError: String? = null,
    progressSaveError: String? = null,
    readingSessionId: String? = null,
) {
    if (progressSaveError != null) {
        AlertDialog(
            onDismissRequest = actions.onProgressSaveErrorDismiss,
            text = { Text(progressSaveError) },
            confirmButton = {
                TextButton(onClick = actions.onProgressSaveErrorDismiss) { Text("확인") }
            },
        )
    }
    var isCommentPanelCollapsed by rememberSaveable { mutableStateOf(false) }
    val canShowCommentPanel = isLandscape() && uiState.loadState == ReaderLoadState.Success
    val showCommentPanel = canShowCommentPanel && !isCommentPanelCollapsed
    val visibleThread = commentSheet?.takeUnless { it.isPanelDeleteOnly }
    val panelThread = visibleThread?.takeIf {
        canShowCommentPanel && it.source != CommentSheetSource.COMMENT_COLLECTION
    }
    val modalThread = visibleThread?.takeIf { panelThread == null && !it.openedInPanel }
    val isReaderOverlayVisible = uiState.isTableOfContentsVisible || uiState.isCommentCollectionsVisible
    val panelThreadToRecord = panelThread?.takeUnless { it.openedInPanel }?.sentenceId
    val notificationSentenceToReveal = panelThread?.takeIf {
        it.source == CommentSheetSource.NOTIFICATION && it.scrollToLatestPending
    }?.sentenceId
    LaunchedEffect(panelThreadToRecord) {
        panelThreadToRecord?.let(actions.onPanelThreadShown)
    }
    LaunchedEffect(notificationSentenceToReveal) {
        if (notificationSentenceToReveal != null) isCommentPanelCollapsed = false
    }

    PlatformBackHandler(
        onBack = {
            when {
                commentSheet?.deletingCommentId != null -> commentSheetActions.onDeleteCancel()
                modalThread != null -> commentSheetActions.onDismiss()
                uiState.isCommentCollectionsVisible -> actions.onCommentCollectionsDismiss()
                uiState.isTableOfContentsVisible -> actions.onTableOfContentsDismiss()
                showCommentPanel && panelThread != null -> commentSheetActions.onDismiss()
                else -> actions.onBackClick()
            }
        },
    )

    val readerListState = rememberReaderListState(
        uiState = uiState,
        actions = actions,
    )
    val commentPanelListState = rememberLazyListState()
    val visibleCommentedSentences = if (showCommentPanel) {
        rememberVisibleCommentedSentences(readerListState, uiState)
    } else {
        emptyList()
    }
    val sentencesToLoad = if (modalThread == null && !isReaderOverlayVisible) {
        visibleCommentedSentences
    } else {
        emptyList()
    }
    LaunchedEffect(sentencesToLoad) {
        actions.onVisibleCommentSentencesChange(sentencesToLoad)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ReaderTopBar(
                    title = uiState.title,
                    author = uiState.author,
                    fontSize = uiState.fontSize,
                    isNewComment = uiState.isNewComment,
                    isTextSettingMenuExpanded = uiState.isTextSettingMenuExpanded,
                    onBackClick = actions.onBackClick,
                    onTableOfContentsClick = actions.onTableOfContentsClick,
                    onTextSettingClick = actions.onTextSettingClick,
                    onTextSettingDismiss = actions.onTextSettingDismiss,
                    onCommentCollectionsClick = actions.onCommentCollectionsClick,
                    onFontSizeChange = { fontSize ->
                        if (fontSize != uiState.fontSize || readerListState.fontSizeRestoreRequest != null) {
                            readerListState.savePositionBeforeFontSizeChange(
                                passages = uiState.passages,
                                currentFontSize = uiState.fontSize,
                                targetFontSize = fontSize,
                            )
                            actions.onFontSizeChange(fontSize)
                        }
                    },
                    modifier = Modifier.zIndex(1f),
                )
            },
            bottomBar = {
                if (uiState.loadState == ReaderLoadState.Success) {
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

                ReaderLoadState.Success -> BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    val panelWidth = minOf(maxWidth / 3, 352.dp)
                    val showReopenButton = canShowCommentPanel && isCommentPanelCollapsed
                    Row(modifier = Modifier.fillMaxSize()) {
                        ReaderContent(
                            passages = uiState.passages.items,
                            fontSize = uiState.fontSize,
                            readerListState = readerListState,
                            contentPadding = PaddingValues(
                                horizontal = ReaderHorizontalPadding,
                                vertical = if (showReopenButton) 32.dp else 0.dp,
                            ),
                            onSentenceClick = { sentence ->
                                if (showReopenButton) isCommentPanelCollapsed = false
                                if (panelThread.canOpenSentence(sentence.sentenceId)) {
                                    if (canShowCommentPanel) {
                                        actions.onPanelThreadOpen(sentence)
                                    } else {
                                        actions.onSentenceClick(sentence)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (showCommentPanel) {
                            ReaderCommentPanel(
                                sentences = panelSentences(visibleCommentedSentences, panelThread, uiState.passages),
                                previews = panelThreads,
                                thread = panelThread,
                                listState = commentPanelListState,
                                actions = commentSheetActions,
                                onClose = {
                                    isCommentPanelCollapsed = true
                                    actions.onPanelToggled(CommentPanelAction.CLOSE)
                                },
                                onWriteComment = { sentence ->
                                    if (panelThread.canOpenSentence(sentence.sentenceId)) {
                                        actions.onPanelThreadOpen(sentence)
                                        commentSheetActions.onComposeStarted(
                                            sentence.sentenceId,
                                            CommentPresentation.SIDE_PANEL,
                                        )
                                    }
                                },
                                onPreviewRetry = actions.onPanelThreadRetry,
                                onCommentAction = { sentence, commentId, action ->
                                    if (panelThread.canOpenSentence(sentence.sentenceId)) {
                                        actions.onPanelCommentAction(sentence, commentId, action)
                                    }
                                },
                                onPanelLike = actions.onPanelLike,
                                panelLikeError = panelLikeError,
                                onPanelLikeErrorConsumed = actions.onPanelLikeErrorConsumed,
                                isLikeEnabled = isCommentLikeEnabled,
                                isThreadVisible = !isReaderOverlayVisible,
                                readingSessionId = readingSessionId.takeUnless {
                                    modalThread != null || commentSheet?.deletingCommentId != null
                                },
                                onContentsShown = actions.onPanelContentsShown,
                                modifier = Modifier.width(panelWidth).fillMaxHeight(),
                            )
                        }
                    }
                    if (showReopenButton) {
                        ReaderCommentPanelReopenButton(
                            onClick = {
                                isCommentPanelCollapsed = false
                                actions.onPanelToggled(CommentPanelAction.REOPEN)
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                                .padding(top = 24.dp)
                                .width(ReaderHorizontalPadding),
                        )
                    }
                }
            }
        }

        if (uiState.returnPassageSequence != null && uiState.mode == ReaderMode.Idle) {
            if (modalThread == null && !isReaderOverlayVisible) {
                LaunchedEffect(uiState.returnPassageSequence) { actions.onReturnButtonShown() }
            }
            ReaderReturnButton(
                onClick = actions.onReturnToPreviousReadingPosition,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, bottom = 72.dp)
                    .zIndex(1f),
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

        if (uiState.isCommentCollectionsVisible) {
            CommentCollectionContents(
                commentedSentences = uiState.commentedSentences,
                mode = uiState.commentedSentenceMode,
                selectedFilter = uiState.commentCollectionFilter,
                onFilterSelected = actions.onCommentCollectionFilterSelected,
                onShowAll = actions.onShowAllComments,
                onSpoilerExposure = actions.onSpoilerExposure,
                onCommentCardClick = actions.onCommentCardClick,
                onSentenceReveal = actions.onCommentSentenceReveal,
                onRetry = actions.onCommentCollectionsRetry,
                onDismissRequest = actions.onCommentCollectionsDismiss,
                modifier = Modifier.zIndex(2f),
            )
        }
    }

    if (modalThread != null) {
        CommentBottomSheet(
            uiState = modalThread,
            actions = commentSheetActions,
            isLikeEnabled = isCommentLikeEnabled,
            onGoToText = actions.onMoveToComment,
        )
    }

    if (commentSheet?.deletingCommentId != null) {
        DeleteCommentDialog(
            onDismissRequest = commentSheetActions.onDeleteCancel,
            onConfirm = commentSheetActions.onDeleteConfirm,
            errorMessage = commentSheet.deleteErrorMessage,
        )
    }
}

@Composable
private fun rememberVisibleCommentedSentences(
    readerListState: ReaderListState,
    uiState: ReaderUiState,
): List<SentenceUiModel> {
    val currentUiState by rememberUpdatedState(uiState)
    var lastVisibleSentences by remember(readerListState) { mutableStateOf(emptyList<SentenceUiModel>()) }
    val visibleSentences by remember(readerListState) {
        derivedStateOf {
            val state = currentUiState
            if (!readerListState.hasPositionedInitialPassage ||
                readerListState.fontSizeRestoreRequest != null ||
                state.mode is ReaderMode.MovingTo
            ) {
                null
            } else {
                readerListState.visibleCommentedSentences(state.passages, state.fontSize)
            }
        }
    }
    val currentSentences = visibleSentences ?: return lastVisibleSentences
    SideEffect { lastVisibleSentences = currentSentences }
    return currentSentences
}

private fun CommentSheetUiState?.canOpenSentence(sentenceId: Long): Boolean {
    if (this == null) return true
    return this.sentenceId != sentenceId && canSwitchSentence
}

private fun panelSentences(
    visible: List<SentenceUiModel>,
    thread: CommentSheetUiState?,
    passages: LoadedPassages,
): List<SentenceUiModel> {
    if (thread == null || visible.any { it.sentenceId == thread.sentenceId }) return visible
    val ids = visible.mapTo(mutableSetOf(thread.sentenceId)) { it.sentenceId }
    val ordered = passages.items.flatMap { passage -> passage.sentences.filter { it.sentenceId in ids } }
    return if (ordered.none { it.sentenceId == thread.sentenceId }) {
        listOf(SentenceUiModel(thread.sentenceId, 0, thread.sentenceContent, thread.comments.size)) + visible
    } else {
        ordered
    }
}

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
    val onLoadPrevious: () -> Unit,
    val onLoadNext: () -> Unit,
    val onVisiblePassageChange: (PassageUiModel) -> Unit,
    val onTargetPassageReached: (PassageUiModel) -> Unit,
    val onTargetPassageNotFound: (Int) -> Unit,
    val onCommentCollectionsClick: () -> Unit,
    val onCommentCollectionsDismiss: () -> Unit,
    val onCommentCollectionsRetry: () -> Unit,
    val onCommentCollectionFilterSelected: (CommentedSentenceFilter) -> Unit,
    val onCommentCardClick: (CommentedSentenceUiModel) -> Unit,
    val onCommentSentenceReveal: (CommentedSentenceUiModel, CommentSentenceRevealTrigger) -> Unit,
    val onMoveToComment: () -> Unit,
    val onReturnToPreviousReadingPosition: () -> Unit,
    val onInitialPositionApplied: (Int?) -> Unit = {},
    val onShowAllComments: () -> Unit = { onCommentCollectionFilterSelected(CommentedSentenceFilter.ALL) },
    val onSpoilerExposure: (CommentCollectionAction) -> Unit = {},
    val onReturnButtonShown: () -> Unit = {},
    val onVisibleCommentSentencesChange: (List<SentenceUiModel>) -> Unit = {},
    val onPanelThreadOpen: (SentenceUiModel) -> Unit = onSentenceClick,
    val onPanelThreadShown: (Long) -> Unit = {},
    val onPanelThreadRetry: (Long) -> Unit = {},
    val onPanelCommentAction: (SentenceUiModel, Long, ReaderPanelCommentAction) -> Unit = { _, _, _ -> },
    val onPanelLike: (Long, Long) -> Unit = { _, _ -> },
    val onPanelLikeErrorConsumed: () -> Unit = {},
    val onProgressSaveErrorDismiss: () -> Unit = {},
    val onPanelContentsShown: () -> Unit = {},
    val onPanelToggled: (CommentPanelAction) -> Unit = {},
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
    readerListState: ReaderListState,
    contentPadding: PaddingValues,
    onSentenceClick: (SentenceUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = readerListState.listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        items(
            items = passages,
            key = { passage -> passage.passageId },
        ) { passage ->
            DisposableEffect(readerListState, passage.passageId) {
                onDispose { readerListState.removeTextLayout(passage.passageId) }
            }
            PassageItem(
                passage = passage,
                fontSize = fontSize,
                onSentenceClick = onSentenceClick,
                onTextLayout = { text, result ->
                    readerListState.updateTextLayout(passage.passageId, fontSize, text, result)
                },
            )
        }
    }
}

@Preview(showBackground = true, name = "전자책 리더 화면")
@Composable
private fun ReaderScreenPreview() {
    YeobaekTheme {
        ReaderScreen(
            isCommentLikeEnabled = true,
            uiState = ReaderUiState(
                loadState = ReaderLoadState.Success,
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
                onLoadPrevious = {},
                onLoadNext = {},
                onVisiblePassageChange = {},
                onTargetPassageReached = {},
                onTargetPassageNotFound = {},
                onCommentCollectionsClick = {},
                onCommentCollectionsDismiss = {},
                onCommentCollectionsRetry = {},
                onCommentCollectionFilterSelected = {},
                onCommentCardClick = {},
                onCommentSentenceReveal = { _, _ -> },
                onMoveToComment = {},
                onReturnToPreviousReadingPosition = {},
            ),
            commentSheetActions = CommentSheetActions(
                onDismiss = {},
                onRetry = {},
                onInputChange = {},
                onSubmit = {},
                onEdit = {},
                onEditCancel = {},
                onDelete = {},
                onDeleteCancel = {},
                onDeleteConfirm = {},
                onReport = {},
                onReportResultConsumed = {},
                onScrollToLatestConsumed = {},
                onLike = {},
                onLikeErrorConsumed = {},
            ),
        )
    }
}
