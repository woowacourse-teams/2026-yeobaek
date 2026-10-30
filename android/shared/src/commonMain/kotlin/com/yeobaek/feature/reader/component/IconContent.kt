package com.yeobaek.feature.reader.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_like
import android.shared.generated.resources.ic_like_click
import android.shared.generated.resources.ic_menu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekLike
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommentActions(
    likedByMe: Boolean,
    likeCount: Long,
    isLikeEnabled: Boolean,
    onMenuClick: () -> Unit,
    onLike: () -> Unit,
    menuButtonDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            modifier = Modifier,
        ) {
            MenuIconButton(
                onMenuClick = onMenuClick,
                menuButtonDescription = menuButtonDescription,
            )
            if (isLikeEnabled) {
                LikeButtonContent(
                    likedByMe = likedByMe,
                    likeCount = likeCount,
                    onLike = onLike,
                )
            }
        }
    }
}

@Composable
private fun MenuIconButton(
    onMenuClick: () -> Unit,
    menuButtonDescription: String,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onMenuClick,
        modifier = modifier.size(32.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_menu),
            contentDescription = menuButtonDescription,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LikeButtonContent(
    likedByMe: Boolean,
    likeCount: Long,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            modifier = Modifier.size(32.dp),
            onClick = onLike,
        ) {
            Icon(
                painter = painterResource(
                    if (likedByMe) Res.drawable.ic_like_click else Res.drawable.ic_like,
                ),
                contentDescription = if (likedByMe) "댓글 좋아요" else "댓글 좋아요 취소",
                modifier = Modifier.size(16.dp),
                tint = Color.Unspecified,
            )
        }
        if (likeCount > 0) {
            Text(
                text = likeCount.toString(),
                color = if (likedByMe) {
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
