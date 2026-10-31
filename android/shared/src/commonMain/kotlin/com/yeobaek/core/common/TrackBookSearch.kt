package com.yeobaek.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yeobaek.core.analytics.BookSearchEnd
import com.yeobaek.core.analytics.BookSearchTracker
import kotlinx.coroutines.delay

@Composable
fun TrackBookSearch(
    tracker: BookSearchTracker,
    isVisible: Boolean,
    resultVersion: Long?,
    resultCount: Int?,
    resultDebounceMillis: Long = 0,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val queryVersion = tracker.queryVersion

    DisposableEffect(tracker) {
        onDispose { tracker.close(BookSearchEnd.NAVIGATION) }
    }
    LaunchedEffect(tracker, isVisible, isResumed, queryVersion, resultVersion, resultCount) {
        when {
            !isVisible -> tracker.close(BookSearchEnd.NAVIGATION)

            !isResumed -> tracker.close(BookSearchEnd.BACKGROUND)

            else -> {
                tracker.activate()
                if (resultVersion != null && resultCount != null) {
                    if (resultDebounceMillis > 0) delay(resultDebounceMillis)
                    tracker.resultsShown(resultVersion, resultCount)
                }
            }
        }
    }
}
