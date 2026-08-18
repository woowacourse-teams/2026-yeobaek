package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.PassageCommentSheetUiState
import com.yeobaek.feature.reader.ReaderPreviewData

@Composable
fun CommentList(
    uiState: PassageCommentSheetUiState,
    onEditComment: (Int) -> Unit,
    onDeleteComment: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "댓글을 불러오는 중이에요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.loadErrorMessage != null -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.loadErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.comments.isEmpty() -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "아직 댓글이 없어요.\n첫 번째 생각을 남겨보세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier,
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
                        onEdit = { onEditComment(comment.commentId) },
                        onDelete = { onDeleteComment(comment.commentId) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "댓글 목록")
@Composable
private fun CommentListPreview() {
    YeobaekTheme {
        CommentList(
            uiState = PassageCommentSheetUiState(
                passageId = 5,
                comments = ReaderPreviewData.commentsByPassageId.getValue(5),
            ),
            onEditComment = {},
            onDeleteComment = {},
        )
    }
}
