package com.yeobaek.feature.group.detail.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.yeobaek.core.designsystem.component.noRippleClickable
import com.yeobaek.core.designsystem.theme.YeobaekError
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun MemberActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true),
            ) {
                Box(modifier = Modifier.width(100.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Column {
                            MemberActionMenuItem(
                                text = "차단",
                                color = YeobaekError,
                                onClick = onDelete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberActionMenuItem(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .noRippleClickable(onClick = onClick)
            .padding(
                start = 12.8.dp,
                top = 0.75.dp,
                end = 13.dp,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Preview(showBackground = true, name = "댓글 메뉴")
@Composable
private fun MemberActionMenuPreview() {
    YeobaekTheme {
        MemberActionMenu(
            expanded = true,
            onDismissRequest = {},
            onDelete = {},
        )
    }
}
