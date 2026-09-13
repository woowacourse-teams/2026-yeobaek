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

// 리더 본문 목록의 스크롤 상태.
//
// 본문 목록은 사용자가 스크롤하는 것 외에도, ViewModel 상태에 맞춰 코드가 직접 스크롤해야 하는 경우가 많다.
// 그 동작들은 rememberReaderListState 안에서 이름 붙은 효과로 나눠 실행한다.
// 코드가 스크롤하는 동안 생긴 위치 변화는 사용자가 읽은 위치가 아니므로 ViewModel에 전달하지 않는다.
@Stable
class ReaderListState(
    val listState: LazyListState,
) {
    // 첫 로딩 뒤 마지막 독서 위치로 이동을 마쳤는지.
    // passages가 바뀔 때마다 다시 이동하면 사용자가 스크롤한 위치를 잃으므로 한 번만 이동한다.
    var hasPositionedInitialPassage by mutableStateOf(false)

    // 글자 크기를 바꾸기 전에 보던 위치. 항목 높이가 달라진 뒤 이 위치로 되돌린다.
    var positionBeforeFontSizeChange by mutableStateOf<PassagePosition?>(null)

    // 글자 크기를 바꾸기 직전에 호출해, 지금 보던 위치를 기억해 둔다.
    fun savePositionBeforeFontSizeChange(passages: LoadedPassages) {
        firstVisiblePosition(passages)?.let { position ->
            positionBeforeFontSizeChange = position
        }
    }

    // 화면 맨 위에 보이는 문단과, 그 문단 안에서 스크롤된 정도
    fun firstVisiblePosition(passages: LoadedPassages): PassagePosition? {
        val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
        val passage = passages.findByItemKey(firstVisibleItem.key) ?: return null
        return PassagePosition(
            passageId = passage.passageId,
            scrollOffset = listState.firstVisibleItemScrollOffset,
        )
    }
}

// 화면의 첫 passage와 그 passage 안에서의 스크롤 위치
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

// 첫 로딩이 끝나면 서버에 저장된 마지막 독서 위치로 목록을 이동한다.
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
        if (!state.hasPositionedInitialPassage && uiState.loadState == ReaderLoadState.Ready) {
            val readingPassageIndex = uiState.passages.indexOfSequence(uiState.readingSequence)
            if (readingPassageIndex >= 0) {
                state.listState.scrollToItem(readingPassageIndex)
            }
            state.hasPositionedInitialPassage = true
        }
    }
}

// 진행률 바나 목차에서 정한 목표 문단이 목록에 불러와지면 그 문단으로 스크롤한다.
// 스크롤을 마치면 ViewModel이 이동 상태를 끝낼 수 있도록 알린다.
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

// 글자 크기가 바뀌면 각 항목의 높이도 바뀐다. 바꾸기 전에 기억한 passage와 오프셋을
// 다시 적용해 사용자가 읽던 문장이 화면 밖으로 크게 밀려나지 않게 한다.
@Composable
private fun RestorePositionAfterFontSizeChangeEffect(
    state: ReaderListState,
    uiState: ReaderUiState,
) {
    LaunchedEffect(uiState.fontSize) {
        val passagePosition = state.positionBeforeFontSizeChange ?: return@LaunchedEffect
        val passageIndex = uiState.passages.indexOfPassageId(passagePosition.passageId)
        if (passageIndex >= 0) {
            // 먼저 항목을 화면에 배치해야 변경된 글자 크기의 실제 높이를 알 수 있다.
            state.listState.scrollToItem(passageIndex)
            val itemSize = state.listState.layoutInfo.visibleItemsInfo
                .firstOrNull { item -> item.index == passageIndex }
                ?.size
                ?: 0
            state.listState.scrollToItem(
                index = passageIndex,
                // 새 항목 높이보다 큰 예전 오프셋은 유효한 범위 안으로 제한한다.
                scrollOffset = passagePosition.scrollOffset.coerceAtMost(
                    maximumValue = (itemSize - 1).coerceAtLeast(0),
                ),
            )
        }
        state.positionBeforeFontSizeChange = null
    }
}

// 화면에 보이는 passage를 계속 관찰해 ViewModel의 readingSequence와 진행률을 갱신한다.
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

            // 글자 크기 변경 뒤 위치를 복원하거나 다른 위치로 이동시키는 동안 생긴 스크롤은
            // 사용자의 실제 독서 위치가 아니므로 ViewModel에 전달하지 않는다.
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

// 지금 읽고 있다고 볼 passage. 보통은 화면 맨 위에 보이는 passage를 쓴다.
// 단, 책의 마지막 passage가 끝까지 보이면 그것을 골라 진행률이 정확히 100%가 되게 한다.
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

// 목록의 처음 또는 끝에서 PAGINATION_THRESHOLD개 이내에 들어오면 앞뒤 문단을 더 요청한다.
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
            // 초기 위치로 이동하기 전에는 목록이 맨 앞에 있어 앞쪽 문단을 잘못 요청하게 되므로 기다린다.
            // 지금 요청해도 되는지(다른 로딩·이동 중인지, 책의 처음이나 끝인지)는 ViewModel이 판단한다.
            if (!state.hasPositionedInitialPassage) return@collect

            // 앞쪽 문단이 목록 앞에 추가돼도 LazyColumn이 key(문단 id)를 기준으로 보던 문단의 위치를 유지하므로
            // 따로 위치를 기억했다가 복원하지 않는다.
            if (isNearStart) currentOnLoadPrevious()
            if (isNearEnd) currentOnLoadNext()
        }
    }
}

// 본문 목록의 항목 key는 문단 id다. 앞에 문단이 추가되면 항목 번호(index)는 밀리지만 key는 그대로라,
// 레이아웃 정보가 아직 갱신되기 전이라도 key로 찾으면 실제로 보이는 문단을 정확히 찾는다.
private fun LoadedPassages.findByItemKey(key: Any): PassageUiModel? =
    (key as? Long)?.let(::findByPassageId)

private const val PAGINATION_THRESHOLD = 5
