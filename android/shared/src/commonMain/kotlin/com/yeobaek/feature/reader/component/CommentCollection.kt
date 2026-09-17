package com.yeobaek.feature.reader.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.model.CommentedSentenceUiModel
import kotlin.math.roundToInt

@Composable
fun CommentCollectionContent(
    commentedSentenceUiModel: CommentedSentenceUiModel,
    modifier: Modifier = Modifier,
) {
    val isNewComment = commentedSentenceUiModel.isNewComment()
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
        ) {
            Text(
                "${commentedSentenceUiModel.progress.roundToInt()}%",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(commentedSentenceUiModel.content)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                CommentCard(text = "댓글", count = commentedSentenceUiModel.commentCount)
                Spacer(modifier = Modifier.width(8.dp))
                if (isNewComment) {
                    CommentCard(
                        text = "새 댓글",
                        count = commentedSentenceUiModel.unreadCommentCount,
                        isNewComment = isNewComment,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    text: String,
    count: Int,
    modifier: Modifier = Modifier,
    isNewComment: Boolean = false,
) {
    Row(
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(
                if (isNewComment) Color(0xFFC3692C) else MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                if (isNewComment) Color(0xFFC3692C) else MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )
    }
}

@Preview(showBackground = true, name = "댓글 모아보기 댓글 카드")
@Composable
private fun CommentCollectionContentPreview() {
    YeobaekTheme {
        CommentCollectionContent(
            commentedSentenceUiModel = CommentedSentenceUiModel(
                sentenceId = 1,
                passageId = 1,
                content = "이 글줄을 몇 차례 읽은 뒤 나는 깊은 생각에 빠졌다...",
                progress = 18.0f,
                isVisible = true,
                commentCount = 3,
                unreadCommentCount = 1,
                passageSequence = 10,
            ),
        )
    }
}
