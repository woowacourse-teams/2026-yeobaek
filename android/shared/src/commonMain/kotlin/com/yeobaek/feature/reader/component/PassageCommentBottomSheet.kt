package com.yeobaek.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.yeobaek.feature.reader.PassageCommentSheetUiState
import com.yeobaek.feature.reader.ReportState
import com.yeobaek.feature.reader.model.SentenceUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassageCommentBottomSheet(
    sentence: SentenceUiModel,
    uiState: PassageCommentSheetUiState,
    onDismissRequest: () -> Unit,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onEditComment: (Long) -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteComment: (Long) -> Unit,
    onCommentReport: (Long) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commentListState = rememberLazyListState()
    var previousCommentCount by remember(uiState.sentenceId) { mutableIntStateOf(-1) }

    LaunchedEffect(uiState.sentenceId, uiState.isLoading, uiState.comments.size) {
        if (uiState.isLoading) return@LaunchedEffect

        val commentCount = uiState.comments.size
        if (previousCommentCount in 0..<commentCount) {
            commentListState.animateScrollToItem(commentCount - 1)
        }
        previousCommentCount = commentCount
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.08f,
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape,
                        ),
                )
            }
        },
    ) {
        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        },
                    )
                },
        ) {
            PassageQuote(
                content = sentence.content,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(22.dp))
            CommentList(
                uiState = uiState,
                onEditComment = onEditComment,
                onDeleteComment = onDeleteComment,
                onCommentReport = onCommentReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listState = commentListState,
            )
            PassageCommentInput(
                value = uiState.input,
                enabled = !uiState.isSubmitting,
                isEditing = uiState.editingCommentId != null,
                onValueChange = onInputChange,
                onSubmit = {
                    onSubmit()
                    focusManager.clearFocus()
                },
                onCancelEdit = {
                    onCancelEdit()
                    focusManager.clearFocus()
                },
            )
        }
    }

    if (uiState.deletingCommentId != null) {
        DeleteCommentDialog(
            onDismissRequest = onCancelDelete,
            onConfirm = onConfirmDelete,
        )
    }
}
