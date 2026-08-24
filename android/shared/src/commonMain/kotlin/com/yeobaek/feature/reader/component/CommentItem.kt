package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_menu
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.ReaderPreviewData
import com.yeobaek.feature.reader.model.PassageCommentUiModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommentItem(
    comment: PassageCommentUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActionMenuExpanded by remember(comment.commentId) {
        mutableStateOf(false)
    }
    val longPressModifier = if (comment.mine) {
        Modifier
            .pointerInput(comment.commentId) {
                detectTapGestures(
                    onLongPress = {
                        isActionMenuExpanded = true
                    },
                )
            }
            .semantics {
                onLongClick(label = "댓글 수정 및 삭제 메뉴 열기") {
                    isActionMenuExpanded = true
                    true
                }
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(longPressModifier),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                CommentAvatar(nickname = comment.nickname)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.nickname,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = comment.createdAt.toDisplayDate(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (comment.mine) {
                    IconButton(
                        onClick = { isActionMenuExpanded = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_menu),
                            contentDescription = "댓글 메뉴 열기",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(42.dp))
                Text(
                    text = comment.content,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        CommentActionMenu(
            expanded = isActionMenuExpanded,
            onDismissRequest = { isActionMenuExpanded = false },
            onEdit = {
                isActionMenuExpanded = false
                onEdit()
            },
            onDelete = {
                isActionMenuExpanded = false
                onDelete()
            },
        )
    }
}

@Composable
private fun CommentAvatar(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
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

@Preview(showBackground = true, name = "타인 댓글")
@Composable
private fun OtherCommentItemPreview() {
    YeobaekTheme {
        CommentItem(
            comment = ReaderPreviewData.commentsByPassageId.getValue(2).first(),
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true, name = "내 댓글")
@Composable
private fun MyCommentItemPreview() {
    YeobaekTheme {
        CommentItem(
            comment = ReaderPreviewData.commentsByPassageId.getValue(5).last(),
            onEdit = {},
            onDelete = {},
        )
    }
}
