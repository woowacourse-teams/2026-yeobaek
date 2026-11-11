package com.yeobaek.core.common

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yeobaek.core.analytics.BookListEnd
import com.yeobaek.core.analytics.BookListVisitTracker

@Composable
fun TrackBookList(
    tracker: BookListVisitTracker,
    gridState: LazyGridState,
    bookIds: List<Long>?,
    onFullyVisibleBooks: ((List<Long>) -> Unit)? = null,
) {
    val onBooksVisible by rememberUpdatedState(onFullyVisibleBooks)
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val isResumed = lifecycleState == Lifecycle.State.RESUMED
    val isDragged = gridState.interactionSource.collectIsDraggedAsState()
    DisposableEffect(tracker) {
        onDispose { tracker.close(BookListEnd.NAVIGATION) }
    }
    LaunchedEffect(tracker, gridState, bookIds, isResumed) {
        if (!isResumed) {
            tracker.close(BookListEnd.BACKGROUND)
            return@LaunchedEffect
        }
        if (bookIds == null) return@LaunchedEffect
        if (bookIds.isEmpty()) {
            withFrameNanos { }
            tracker.observe(bookIds, position = null)
            return@LaunchedEffect
        }

        val positions = bookIds.withIndex().associate { (index, id) -> id to index + 1 }
        snapshotFlow {
            val layout = gridState.layoutInfo
            val position = layout.visibleItemsInfo.mapNotNull { positions[it.key] }.maxOrNull()
            val fullyVisible = if (onBooksVisible == null) {
                emptyList()
            } else {
                layout.visibleItemsInfo.mapNotNull { item ->
                    val id = item.key as? Long
                    id?.takeIf {
                        it in positions &&
                            item.offset.y >= layout.viewportStartOffset &&
                            item.offset.y + item.size.height <= layout.viewportEndOffset
                    }
                }
            }
            position to fullyVisible
        }.collect { (position, fullyVisible) ->
            if (position != null) tracker.observe(bookIds, position)
            if (fullyVisible.isNotEmpty()) onBooksVisible?.invoke(fullyVisible)
        }
    }
    LaunchedEffect(tracker, gridState, isResumed) {
        if (!isResumed) return@LaunchedEffect
        var previous = gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { position ->
            if (isDragged.value && position != previous) tracker.onUserScrolled()
            previous = position
        }
    }
}
