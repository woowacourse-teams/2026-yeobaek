package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.CommentSheetUiState
import com.yeobaek.feature.reader.model.CommentUiModel

@Composable
fun CommentList(
    uiState: CommentSheetUiState,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onReport: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when {
        uiState.isLoading -> CommentListMessage(
            text = "댓글을 불러오는 중이에요.",
            modifier = modifier,
        )

        uiState.loadErrorMessage != null -> CommentListMessage(
            text = uiState.loadErrorMessage,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )

        uiState.comments.isEmpty() -> CommentListMessage(
            text = "아직 댓글이 없어요.\n첫 번째 생각을 남겨보세요.",
            modifier = modifier,
        )

        else -> {
            LazyColumn(
                modifier = modifier,
                state = listState,
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(
                    items = uiState.comments,
                    key = { comment -> comment.commentId },
                ) { comment ->
                    CommentItem(
                        comment = comment,
                        onEdit = { onEdit(comment.commentId) },
                        onDelete = { onDelete(comment.commentId) },
                        onReport = { onReport(comment.commentId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentListMessage(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true, name = "댓글 목록")
@Composable
private fun CommentListPreview() {
    YeobaekTheme {
        CommentList(
            uiState = CommentSheetUiState(
                sentenceId = 501,
                comments = listOf(
                    CommentUiModel(
                        commentId = 10,
                        memberId = 5,
                        nickname = "하윤",
                        content = "젊은 선생님을 바라보는 시선이 재미있어요.",
                        createdAt = "2026-08-08T08:45:00",
                        updatedAt = null,
                        isMine = false,
                    ),
                    CommentUiModel(
                        commentId = 11,
                        memberId = 6,
                        nickname = "도윤",
                        content = "거짓 품위를 보이지 않았다는 말에 공감했어요.",
                        createdAt = "2026-08-08T18:20:00",
                        updatedAt = null,
                        isMine = false,
                    ),
                    CommentUiModel(
                        commentId = 12,
                        memberId = 1,
                        nickname = "나",
                        content = "호감의 이유가 아주 선명하게 드러나는 문단 같아요.",
                        createdAt = "2026-08-09T13:10:00",
                        updatedAt = null,
                        isMine = true,
                    ),
                ),
            ),
            onEdit = {},
            onDelete = {},
            onReport = {},
        )
    }
}
