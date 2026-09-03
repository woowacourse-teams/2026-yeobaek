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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.yeobaek.feature.reader.model.SentenceUiModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onSentenceClick: (SentenceUiModel) -> Unit,
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
    onCommentReport: (Long) -> Unit,
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

    // 초기 위치는 데이터 로딩이 끝난 뒤 한 번만 맞춘다.
    // passages가 바뀔 때마다 다시 이동하면 사용자가 스크롤한 위치를 잃기 때문에 별도 플래그가 필요하다.
    var hasPositionedInitialPassage by remember { mutableStateOf(false) }

    // 목록 앞에 passage가 추가되거나 글자 크기로 항목 높이가 달라져도 사용자가 보던
    // passage와 그 안의 스크롤 오프셋을 복원하기 위한 기준점이다.
    var previousLoadPassagePosition by remember { mutableStateOf<PassagePosition?>(null) }
    var fontSizePassagePosition by remember { mutableStateOf<PassagePosition?>(null) }

    // 아래의 snapshotFlow는 한 번 시작되면 오래 실행된다. rememberUpdatedState를 사용하면
    // 효과를 재시작하지 않고도 가장 최근 uiState와 콜백을 참조할 수 있다.
    val currentUiState by rememberUpdatedState(uiState)
    val currentOnLoadPrevious by rememberUpdatedState(onLoadPrevious)
    val currentOnLoadNext by rememberUpdatedState(onLoadNext)
    val currentOnFontSizeChange by rememberUpdatedState(onFontSizeChange)
    val currentOnVisiblePassageChange by rememberUpdatedState(onVisiblePassageChange)
    val currentOnProgressSeekCompleted by rememberUpdatedState(onProgressSeekCompleted)
    val commentSheet = uiState.commentSheet

    // 첫 로딩이 끝나면 서버에 저장된 마지막 독서 위치로 목록을 이동한다.
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

    // 진행률 바나 목차에서 정한 목표가 목록에 준비되면 실제 LazyColumn을 이동한다.
    // 이동 완료 콜백은 ViewModel이 isMovingToPassage 상태를 끝낼 수 있게 한다.
    LaunchedEffect(
        uiState.scrollTargetSequence,
        uiState.passages,
    ) {
        val targetSequence = uiState.scrollTargetSequence ?: return@LaunchedEffect
        val targetIndex = uiState.passages.indexOfFirst { passage ->
            passage.sequence == targetSequence
        }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            currentOnProgressSeekCompleted(uiState.passages[targetIndex])
        }
    }

    // 글자 크기가 바뀌면 각 항목의 높이도 바뀐다. 변경 전에 저장한 passage와 오프셋을
    // 다시 적용해 사용자가 읽던 문장이 화면 밖으로 크게 밀려나지 않게 한다.
    LaunchedEffect(uiState.fontSize) {
        val passagePosition = fontSizePassagePosition
        if (passagePosition != null) {
            val passageIndex = uiState.passages.indexOfFirst { passage ->
                passage.passageId == passagePosition.passageId
            }
            if (passageIndex >= 0) {
                // 먼저 항목을 화면에 배치해야 변경된 글자 크기의 실제 높이를 알 수 있다.
                listState.scrollToItem(passageIndex)
                val itemSize = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { item -> item.index == passageIndex }
                    ?.size
                    ?: 0
                listState.scrollToItem(
                    index = passageIndex,
                    // 새 항목 높이보다 큰 예전 오프셋은 유효한 범위 안으로 제한한다.
                    scrollOffset = passagePosition.scrollOffset.coerceAtMost(
                        maximumValue = (itemSize - 1).coerceAtLeast(0),
                    ),
                )
            }
            fontSizePassagePosition = null
        }
    }

    // 현재 보이는 passage를 계속 관찰해 ViewModel의 currentSequence와 진행률을 갱신한다.
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

            // 보통 첫 번째 보이는 passage를 현재 위치로 사용한다. 단, 책의 마지막 passage가
            // 완전히 보이면 그것을 선택해 진행률이 정확히 100%가 되도록 한다.
            val passage = if (isLastPassageFullyVisible) {
                lastVisiblePassage
            } else {
                firstVisiblePassage
            }

            // 코드가 위치를 복원하거나 다른 위치로 이동시키는 동안 발생한 스크롤 이벤트는
            // 사용자의 실제 독서 위치가 아니므로 ViewModel에 전달하지 않는다.
            passage to (
                state.isLoadingPrevious ||
                    previousLoadPassagePosition != null ||
                    fontSizePassagePosition != null ||
                    state.isProgressDragging ||
                    state.isMovingToPassage
                )
        }.distinctUntilChanged().collect { (passage, isRestoringPosition) ->
            if (!isRestoringPosition && passage != null) {
                currentOnVisiblePassageChange(passage)
            }
        }
    }

    // 이전 passage를 목록 앞에 추가하면 기존 항목의 인덱스가 뒤로 밀린다. 요청 전에 저장한
    // passageId를 새 목록에서 다시 찾아 같은 내용과 오프셋이 보이도록 복원한다.
    LaunchedEffect(
        uiState.isLoadingPrevious,
        uiState.passages.firstOrNull()?.passageId,
    ) {
        val passagePosition = previousLoadPassagePosition
        if (passagePosition != null && !uiState.isLoadingPrevious) {
            val passageIndex = uiState.passages.indexOfFirst { passage ->
                passage.passageId == passagePosition.passageId
            }
            if (passageIndex >= 0) {
                listState.scrollToItem(
                    index = passageIndex,
                    scrollOffset = passagePosition.scrollOffset,
                )
            }

            previousLoadPassagePosition = null
        }
    }

    // 목록의 처음 또는 끝에서 PAGINATION_THRESHOLD개 이내에 들어오면 다음 묶음을 요청한다.
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

            // 초기 위치 복원 전이나 다른 이동 중에는 페이지 요청이 목록을 동시에 바꾸지 않게 한다.
            if (
                !hasPositionedInitialPassage ||
                state.isLoading ||
                state.isProgressDragging ||
                state.isMovingToPassage ||
                state.loadErrorMessage != null
            ) {
                return@collect
            }

            if (
                isNearStart &&
                !state.isLoadingPrevious &&
                state.passages.firstOrNull()?.sequence != FIRST_PASSAGE_SEQUENCE &&
                previousLoadPassagePosition == null
            ) {
                val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                val firstVisiblePassage = firstVisibleItem?.let { item ->
                    state.passages.getOrNull(item.index)
                }
                if (firstVisibleItem != null && firstVisiblePassage != null) {
                    // 요청이 실제로 시작된 경우에만 기준점을 보관한다. 요청이 거절됐다면
                    // passage position이 남아 이후 페이지네이션을 막지 않도록 null을 유지한다.
                    previousLoadPassagePosition = positionToRestore(
                        passagePosition = PassagePosition(
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

    val selectedSentence = commentSheet?.let { sheet ->
        uiState.passages
            .asSequence()
            .flatMap { passage -> passage.sentences.asSequence() }
            .firstOrNull { sentence -> sentence.sentenceId == sheet.sentenceId }
    }

    val preservePositionAndChangeFontSize: (Int) -> Unit = { fontSize ->
        if (fontSize != uiState.fontSize) {
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val firstVisiblePassage = firstVisibleItem?.let { item ->
                uiState.passages.getOrNull(item.index)
            }
            if (firstVisiblePassage != null) {
                fontSizePassagePosition = PassagePosition(
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
                onSentenceClick = onSentenceClick,
                modifier = Modifier
                    .padding(innerPadding)
                    .clipToBounds(),
            )
        }
    }

    if (commentSheet != null && selectedSentence != null) {
        PassageCommentBottomSheet(
            sentence = selectedSentence,
            uiState = commentSheet,
            onDismissRequest = onCommentSheetDismiss,
            onInputChange = onCommentInputChange,
            onSubmit = onCommentSubmit,
            onEditComment = onCommentEdit,
            onCancelEdit = onCommentEditCancel,
            onCommentReport = onCommentReport,
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
                title = "데미안",
                author = "헤르만 헤세",
                passages = listOf(
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
                currentSequence = 4,
                totalPassageCount = 5,
            ),
            onSentenceClick = {},
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
            onCommentReport = {},
            onLoadPrevious = { false },
            onLoadNext = {},
            onVisiblePassageChange = {},
            onProgressChange = {},
            onProgressChangeFinished = {},
            onProgressSeekCompleted = {},
        )
    }
}

// 화면의 첫 passage와 그 passage 안에서의 스크롤 위치
internal data class PassagePosition(
    val passageId: Long,
    val scrollOffset: Int,
)

// 이전 페이지 요청이 시작됐을 때 복원에 사용할 문단과 스크롤 위치를 남긴다.
internal fun positionToRestore(
    passagePosition: PassagePosition,
    requestStarted: Boolean,
): PassagePosition? = passagePosition.takeIf { requestStarted } // true이면 원래 객체 반환, false이면 null 반환

private const val FIRST_PASSAGE_SEQUENCE = 1
private const val PAGINATION_THRESHOLD = 5
