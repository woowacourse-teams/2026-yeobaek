package com.yeobaek.feature.reader

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextLayoutResult
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Stable
class ReaderListState(
    val listState: LazyListState,
) {
    var hasPositionedInitialPassage by mutableStateOf(false)
    internal var fontSizeRestoreRequest by mutableStateOf<FontSizeRestoreRequest?>(null)
        private set
    private val textLayouts = mutableStateMapOf<Long, PassageTextLayout>()
    private var nextRestoreId = 0L
    private var fontSizeAnchor: ReaderTextPosition? = null

    fun savePositionBeforeFontSizeChange(
        passages: LoadedPassages,
        currentFontSize: Int,
        targetFontSize: Int,
    ) {
        val anchor = fontSizeAnchor ?: firstVisibleCharacter(passages, currentFontSize)
        fontSizeAnchor = anchor
        fontSizeRestoreRequest = anchor?.let {
            FontSizeRestoreRequest(++nextRestoreId, it, targetFontSize)
        }
    }

    fun updateTextLayout(passageId: Long, fontSize: Int, result: TextLayoutResult) {
        textLayouts[passageId] = PassageTextLayout(fontSize, result)
    }

    fun removeTextLayout(passageId: Long) {
        textLayouts.remove(passageId)
    }

    fun cancelFontSizeRestore() {
        fontSizeAnchor = null
        fontSizeRestoreRequest = null
    }

    internal fun textLayoutFor(passageId: Long, fontSize: Int): TextLayoutResult? =
        textLayouts[passageId]?.takeIf { it.fontSize == fontSize }?.result

    internal fun finishFontSizeRestore(request: FontSizeRestoreRequest) {
        if (fontSizeRestoreRequest?.id == request.id) fontSizeRestoreRequest = null
    }

    internal fun firstVisibleCharacter(passages: LoadedPassages, fontSize: Int): ReaderTextPosition? {
        val viewport = listState.layoutInfo
        for (item in viewport.visibleItemsInfo) {
            val passage = passages.findByItemKey(item.key) ?: continue
            val layout = textLayoutFor(passage.passageId, fontSize) ?: continue
            val text = ReaderPassageText(passage.sentences)
            if (layout.layoutInput.text.text != text.text) continue

            for (line in 0 until layout.lineCount) {
                if (item.offset + layout.getLineBottom(line) <= viewport.viewportStartOffset) continue
                if (item.offset + layout.getLineTop(line) >= viewport.viewportEndOffset) break
                val position = text.firstCharacterIn(
                    start = layout.getLineStart(line),
                    end = layout.getLineEnd(line, visibleEnd = true),
                ) ?: continue
                return position
            }
        }
        return null
    }
}

internal data class FontSizeRestoreRequest(
    val id: Long,
    val anchor: ReaderTextPosition,
    val fontSize: Int,
)

private data class PassageTextLayout(
    val fontSize: Int,
    val result: TextLayoutResult,
)

@Composable
fun rememberReaderListState(
    uiState: ReaderUiState,
    actions: ReaderActions,
): ReaderListState {
    val listState = rememberLazyListState()
    val readerListState = rememberSaveable(
        listState,
        saver = Saver<ReaderListState, Boolean>(
            save = { it.hasPositionedInitialPassage },
            restore = { positioned ->
                ReaderListState(listState).apply { hasPositionedInitialPassage = positioned }
            },
        ),
    ) { ReaderListState(listState) }

    ScrollToInitialPassageEffect(
        state = readerListState,
        uiState = uiState,
        onPositionApplied = actions.onInitialPositionApplied,
    )
    ScrollToTargetPassageEffect(
        state = readerListState,
        uiState = uiState,
        onTargetPassageReached = actions.onTargetPassageReached,
        onTargetPassageNotFound = actions.onTargetPassageNotFound,
    )
    RestorePositionAfterFontSizeChangeEffect(
        state = readerListState,
        uiState = uiState,
    )
    CancelFontSizeRestoreOnDragEffect(readerListState)
    LaunchedEffect(uiState.mode) {
        if (uiState.mode != ReaderMode.Idle) {
            readerListState.cancelFontSizeRestore()
        }
    }
    ReportVisiblePassageEffect(
        state = readerListState,
        uiState = uiState,
        onVisiblePassageChange = actions.onVisiblePassageChange,
    )
    LoadPassagesNearEdgesEffect(
        state = readerListState,
        onLoadPrevious = actions.onLoadPrevious,
        onLoadNext = actions.onLoadNext,
    )

    return readerListState
}

@Composable
private fun ScrollToInitialPassageEffect(
    state: ReaderListState,
    uiState: ReaderUiState,
    onPositionApplied: (Int?) -> Unit,
) {
    val currentOnPositionApplied by rememberUpdatedState(onPositionApplied)
    LaunchedEffect(
        uiState.passages,
        uiState.readingSequence,
        uiState.loadState,
    ) {
        if (!state.hasPositionedInitialPassage && uiState.loadState == ReaderLoadState.Success) {
            val readingPassageIndex = uiState.passages.indexOfSequence(uiState.readingSequence)
            if (readingPassageIndex >= 0) {
                state.listState.scrollToItem(readingPassageIndex)
            }
            state.hasPositionedInitialPassage = true
            currentOnPositionApplied(uiState.readingSequence.takeIf { readingPassageIndex >= 0 })
        }
    }
}

@Composable
private fun ScrollToTargetPassageEffect(
    state: ReaderListState,
    uiState: ReaderUiState,
    onTargetPassageReached: (PassageUiModel) -> Unit,
    onTargetPassageNotFound: (Int) -> Unit,
) {
    val currentOnTargetPassageReached by rememberUpdatedState(onTargetPassageReached)
    val currentOnTargetPassageNotFound by rememberUpdatedState(onTargetPassageNotFound)
    val loadedTargetSequence = (uiState.mode as? ReaderMode.MovingTo)
        ?.takeIf { movingTo -> movingTo.isTargetLoaded }
        ?.targetSequence

    LaunchedEffect(
        loadedTargetSequence,
        uiState.passages,
    ) {
        val targetSequence = loadedTargetSequence ?: return@LaunchedEffect
        val targetIndex = uiState.passages.indexOfSequence(targetSequence)
        if (targetIndex < 0) {
            currentOnTargetPassageNotFound(targetSequence)
            return@LaunchedEffect
        }

        state.listState.scrollToItem(targetIndex)
        uiState.passages.getOrNull(targetIndex)?.let(currentOnTargetPassageReached)
    }
}

@Composable
private fun RestorePositionAfterFontSizeChangeEffect(
    state: ReaderListState,
    uiState: ReaderUiState,
) {
    val currentUiState by rememberUpdatedState(uiState)
    val request = state.fontSizeRestoreRequest
    val targetPassageId = request?.let { restoreRequest ->
        uiState.passages.items.firstOrNull { passage ->
            passage.sentences.any { sentence -> sentence.sentenceId == restoreRequest.anchor.sentenceId }
        }?.passageId
    }

    LaunchedEffect(request, uiState.fontSize, uiState.mode, targetPassageId) {
        if (request == null || request.fontSize != uiState.fontSize) return@LaunchedEffect
        try {
            if (uiState.mode != ReaderMode.Idle || targetPassageId == null) return@LaunchedEffect
            val passageIndex = currentUiState.passages.indexOfPassageId(targetPassageId)
            if (passageIndex < 0) return@LaunchedEffect

            state.listState.scrollToItem(passageIndex)
            val layout = snapshotFlow { state.textLayoutFor(targetPassageId, request.fontSize) }
                .filterNotNull()
                .first()
            val passage = currentUiState.passages.findByPassageId(targetPassageId) ?: return@LaunchedEffect
            val text = ReaderPassageText(passage.sentences)
            if (layout.layoutInput.text.text != text.text) return@LaunchedEffect
            val displayOffset = text.displayOffsetOf(request.anchor) ?: return@LaunchedEffect
            val index = currentUiState.passages.indexOfPassageId(targetPassageId)
            if (index < 0) return@LaunchedEffect
            val lineTop = layout.getLineTop(layout.getLineForOffset(displayOffset))
            val scrollOffset = (lineTop - state.listState.layoutInfo.viewportStartOffset).roundToInt()
            state.listState.scrollToItem(index, scrollOffset)
        } finally {
            state.finishFontSizeRestore(request)
        }
    }
}

@Composable
private fun CancelFontSizeRestoreOnDragEffect(state: ReaderListState) {
    LaunchedEffect(state.listState) {
        state.listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) state.cancelFontSizeRestore()
        }
    }
}

@Composable
private fun ReportVisiblePassageEffect(
    state: ReaderListState,
    uiState: ReaderUiState,
    onVisiblePassageChange: (PassageUiModel) -> Unit,
) {
    val currentUiState by rememberUpdatedState(uiState)
    val currentOnVisiblePassageChange by rememberUpdatedState(onVisiblePassageChange)

    LaunchedEffect(state.hasPositionedInitialPassage, state.listState) {
        if (!state.hasPositionedInitialPassage) return@LaunchedEffect

        snapshotFlow {
            val latestUiState = currentUiState
            val passage = readingPassage(state.listState, latestUiState)

            val isScrolledByCode = state.fontSizeRestoreRequest != null ||
                latestUiState.mode != ReaderMode.Idle

            passage to isScrolledByCode
        }.distinctUntilChanged().collect { (passage, isScrolledByCode) ->
            if (!isScrolledByCode && passage != null) {
                currentOnVisiblePassageChange(passage)
            }
        }
    }
}

private fun readingPassage(
    listState: LazyListState,
    uiState: ReaderUiState,
): PassageUiModel? {
    val layoutInfo = listState.layoutInfo
    val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
    val firstVisiblePassage = firstVisibleItem?.let { item ->
        uiState.passages.findByItemKey(item.key)
    }
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
    val lastVisiblePassage = lastVisibleItem?.let { item ->
        uiState.passages.findByItemKey(item.key)
    }
    val isLastPassageFullyVisible = lastVisibleItem != null &&
        lastVisiblePassage?.sequence == uiState.totalPassageCount &&
        lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset

    return if (isLastPassageFullyVisible) {
        lastVisiblePassage
    } else {
        firstVisiblePassage
    }
}

@Composable
private fun LoadPassagesNearEdgesEffect(
    state: ReaderListState,
    onLoadPrevious: () -> Unit,
    onLoadNext: () -> Unit,
) {
    val currentOnLoadPrevious by rememberUpdatedState(onLoadPrevious)
    val currentOnLoadNext by rememberUpdatedState(onLoadNext)

    LaunchedEffect(state.listState) {
        snapshotFlow {
            val visibleItems = state.listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                false to false
            } else {
                val lastItemIndex = state.listState.layoutInfo.totalItemsCount - 1
                val isNearStart = visibleItems.first().index <= PAGINATION_THRESHOLD
                val isNearEnd = visibleItems.last().index >= lastItemIndex - PAGINATION_THRESHOLD
                isNearStart to isNearEnd
            }
        }.distinctUntilChanged().collect { (isNearStart, isNearEnd) ->
            if (!state.hasPositionedInitialPassage) return@collect

            if (isNearStart) currentOnLoadPrevious()
            if (isNearEnd) currentOnLoadNext()
        }
    }
}

private fun LoadedPassages.findByItemKey(key: Any): PassageUiModel? =
    (key as? Long)?.let(::findByPassageId)

private const val PAGINATION_THRESHOLD = 5
