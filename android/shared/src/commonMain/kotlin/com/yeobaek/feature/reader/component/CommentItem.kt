package com.yeobaek.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.mockCommentsByPassageId
import com.yeobaek.feature.reader.model.PassageCommentUiModel

@Composable
fun CommentItem(
    comment: PassageCommentUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        CommentAvatar(nickname = comment.nickname)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.nickname,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = comment.createdAt.toDisplayDate(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (comment.mine) {
                    CommentActionMenu(
                        onEdit = onEdit,
                        onDelete = onDelete,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CommentAvatar(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.firstOrNull()?.toString().orEmpty(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

private fun String.toDisplayDate(): String {
    if (length < 10) return this
    return "${substring(0, 4)}.${substring(5, 7)}.${substring(8, 10)}"
}

@Preview(showBackground = true, name = "댓글")
@Composable
private fun CommentItemPreview() {
    YeobaekTheme {
        CommentItem(
            comment = mockCommentsByPassageId.getValue(2).first(),
            onEdit = {},
            onDelete = {},
        )
    }
}
