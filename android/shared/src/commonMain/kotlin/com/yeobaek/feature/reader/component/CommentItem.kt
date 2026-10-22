package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_like
import android.shared.generated.resources.ic_like_click
import android.shared.generated.resources.ic_menu
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekLike
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.model.CommentUiModel
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommentItem(
    comment: CommentUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember(comment.commentId) {
        mutableStateOf(false)
    }

    val menuItems = if (comment.isMine) {
        listOf(
            CommentMenuItem(text = "수정", onClick = onEdit),
            CommentMenuItem(text = "삭제", isWarning = true, onClick = onDelete),
        )
    } else {
        listOf(
            CommentMenuItem(text = "신고", isWarning = true, onClick = onReport),
        )
    }
    val longPressLabel = if (comment.isMine) "댓글 수정 및 삭제 메뉴 열기" else "댓글 신고 메뉴 열기"
    val menuButtonDescription = if (comment.isMine) "댓글 메뉴 열기" else "댓글 신고하기"

    val longPressModifier = Modifier
        .pointerInput(comment.commentId) {
            detectTapGestures(
                onLongPress = {
                    isMenuExpanded = true
                },
            )
        }
        .semantics {
            onLongClick(label = longPressLabel) {
                isMenuExpanded = true
                true
            }
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(longPressModifier),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
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
                            text = comment.displayCreatedAt,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
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
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.TopEnd,
            ) {
                Column(
                    modifier = Modifier,
                ) {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_menu),
                            contentDescription = menuButtonDescription,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        modifier = Modifier.width(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onLike,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (comment.likedByMe) Res.drawable.ic_like_click else Res.drawable.ic_like,
                                ),
                                contentDescription = "좋아요 버튼",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Unspecified,
                            )
                        }
                        if (comment.likeCount > 0) {
                            Text(
                                text = comment.likeCount.toString(),
                                color = if (comment.likedByMe) {
                                    YeobaekLike
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }

        CommentMenu(
            expanded = isMenuExpanded,
            items = menuItems,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier.padding(top = 32.dp),
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

@Preview(showBackground = true, name = "타인 댓글")
@Composable
private fun OtherCommentItemPreview() {
    YeobaekTheme {
        CommentItem(
            comment = CommentUiModel(
                commentId = 7,
                memberId = 2,
                nickname = "지수",
                content = "이 문장에서 멈칫했어요.",
                createdAt = "2026-08-05T14:30:00",
                updatedAt = null,
                isMine = false,
                likeCount = 12,
                likedByMe = true,
            ),
            onEdit = {},
            onDelete = {},
            onReport = {},
            onLike = {},
        )
    }
}

@Preview(showBackground = true, name = "내 댓글")
@Composable
private fun MyCommentItemPreview() {
    YeobaekTheme {
        CommentItem(
            comment = CommentUiModel(
                commentId = 12,
                memberId = 1,
                nickname = "나",
                content = "호감의 이유가 아주 선명하게 드러나는 문단 같아요.",
                createdAt = "2026-08-09T13:10:00",
                updatedAt = null,
                isMine = true,
                likeCount = 3,
            ),
            onEdit = {},
            onDelete = {},
            onReport = {},
            onLike = {},
        )
    }
}
