package com.yeobaek.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yeobaek.core.designsystem.component.noRippleClickable
import com.yeobaek.core.designsystem.theme.YeobaekError
import com.yeobaek.core.designsystem.theme.YeobaekNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekOnNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTextSubtle
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun DeleteCommentDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.width(244.dp),
            shape = RoundedCornerShape(15.dp),
            color = YeobaekSurfaceElevated,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = 16.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "댓글을 삭제할까요?",
                    color = YeobaekTextStrong,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(8.5.dp))
                Text(
                    text = "삭제한 댓글은 다시 복구할 수 없어요.",
                    color = YeobaekTextSubtle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp,
                        lineHeight = 20.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(17.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DeleteDialogButton(
                        text = "취소",
                        containerColor = YeobaekNeutralContainer,
                        contentColor = YeobaekOnNeutralContainer,
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                    )
                    DeleteDialogButton(
                        text = "삭제",
                        containerColor = YeobaekError,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteDialogButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.5.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(10.dp),
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Preview(showBackground = true, name = "댓글 삭제 다이얼로그")
@Composable
private fun DeleteCommentDialogPreview() {
    YeobaekTheme {
        DeleteCommentDialog(
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
