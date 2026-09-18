package com.yeobaek.feature.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.yeobaek.feature.reader.model.LoadedPassages
import com.yeobaek.feature.reader.model.PassageUiModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Stable
class ReaderListState(
    val listState: LazyListState,
) {
    var hasPositionedInitialPassage by mutableStateOf(false)
    var positionBeforeFontSizeChange by mutableStateOf<PassagePosition?>(null)

    fun savePositionBeforeFontSizeChange(passages: LoadedPassages) {
        firstVisiblePosition(passages)?.let { position ->
            positionBeforeFontSizeChange = position
        }
    }

    fun firstVisiblePosition(passages: LoadedPassages): PassagePosition? {
        val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
        val passage = passages.findByItemKey(firstVisibleItem.key) ?: return null
        return PassagePosition(
            passageId = passage.passageId,
            scrollOffset = listState.firstVisibleItemScrollOffset,
        )
    }
}

data class PassagePosition(
    val passageId: Long,
    val scrollOffset: Int,
)

@Composable
fun rememberReaderListState(
    uiState: ReaderUiState,
    actions: ReaderActions,
): ReaderListState {
    val listState = rememberLazyListState()
    val readerListState = remember { ReaderListState(listState) }

    ScrollToInitialPassageEffect(
        state = readerListState,
        uiState = uiState,
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
) {
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
    LaunchedEffect(uiState.fontSize) {
        val passagePosition = state.positionBeforeFontSizeChange ?: return@LaunchedEffect
        val passageIndex = uiState.passages.indexOfPassageId(passagePosition.passageId)
        if (passageIndex >= 0) {
            state.listState.scrollToItem(passageIndex)
            val itemSize = state.listState.layoutInfo.visibleItemsInfo
                .firstOrNull { item -> item.index == passageIndex }
                ?.size
                ?: 0
            state.listState.scrollToItem(
                index = passageIndex,
                scrollOffset = passagePosition.scrollOffset.coerceAtMost(
                    maximumValue = (itemSize - 1).coerceAtLeast(0),
                ),
            )
        }
        state.positionBeforeFontSizeChange = null
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

            val isScrolledByCode = state.positionBeforeFontSizeChange != null ||
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
