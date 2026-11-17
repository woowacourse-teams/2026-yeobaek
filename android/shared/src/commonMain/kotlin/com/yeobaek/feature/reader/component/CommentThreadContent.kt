package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.reader_go_to_sentence
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.yeobaek.feature.reader.CommentSheetActions
import com.yeobaek.feature.reader.CommentSheetUiState
import com.yeobaek.feature.reader.ReportState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CommentThreadContent(
    uiState: CommentSheetUiState,
    actions: CommentSheetActions,
    isLikeEnabled: Boolean,
    onGoToText: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    val commentListState = rememberLazyListState()
    val snackbarHostState = rememberCommentThreadFeedback(uiState, actions, isVisible)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(
        uiState.sentenceId,
        uiState.scrollToLatestPending,
        uiState.isLoading,
        uiState.loadErrorMessage,
    ) {
        if (!uiState.scrollToLatestPending || uiState.isLoading || uiState.loadErrorMessage != null) {
            return@LaunchedEffect
        }
        if (uiState.comments.isNotEmpty()) {
            commentListState.scrollToItem(uiState.comments.lastIndex)
        }
        actions.onScrollToLatestConsumed(uiState.sentenceId)
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(focusManager) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
        ) {
            SentenceQuote(
                content = uiState.sentenceContent,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(22.dp))
            CommentList(
                uiState = uiState,
                isLikeEnabled = isLikeEnabled,
                onEdit = actions.onEdit,
                onDelete = actions.onDelete,
                onReport = actions.onReport,
                onLike = actions.onLike,
                onRetry = actions.onRetry,
                modifier = Modifier.fillMaxWidth().weight(1f),
                listState = commentListState,
            )
            if (uiState.targetPassageSequence != null) {
                Button(
                    onClick = onGoToText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(Res.string.reader_go_to_sentence))
                }
            }
            CommentThreadInput(uiState, actions)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 72.dp),
        )
    }
}

@Composable
fun rememberCommentThreadFeedback(
    uiState: CommentSheetUiState,
    actions: CommentSheetActions,
    isVisible: Boolean,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): SnackbarHostState {
    val coroutineScope = rememberCoroutineScope()

    val contentsVisible = isVisible && !uiState.isLoading && uiState.loadErrorMessage == null
    LaunchedEffect(uiState.sentenceId, contentsVisible) {
        if (contentsVisible) {
            withFrameNanos { }
            actions.onContentsShown(uiState.sentenceId)
        }
    }

    LaunchedEffect(uiState.reportState) {
        val message = when (val reportState = uiState.reportState) {
            is ReportState.Success -> "댓글을 신고했습니다."
            is ReportState.Failure -> reportState.message
            is ReportState.Idle, ReportState.Loading -> null
        } ?: return@LaunchedEffect

        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
        actions.onReportResultConsumed()
    }

    LaunchedEffect(uiState.likeErrorMessage) {
        val message = uiState.likeErrorMessage ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        actions.onLikeErrorConsumed()
    }

    return snackbarHostState
}

@Composable
fun CommentThreadInput(
    uiState: CommentSheetUiState,
    actions: CommentSheetActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    onBeforeSubmit: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    CommentInput(
        value = uiState.input,
        enabled = !uiState.isSubmitting,
        isEditing = uiState.editingCommentId != null,
        onValueChange = actions.onInputChange,
        onSubmit = {
            onBeforeSubmit()
            actions.onSubmit()
            focusManager.clearFocus()
        },
        modifier = modifier,
        contentPadding = contentPadding,
        onEditCancel = {
            actions.onEditCancel()
            focusManager.clearFocus()
        },
    )
}
