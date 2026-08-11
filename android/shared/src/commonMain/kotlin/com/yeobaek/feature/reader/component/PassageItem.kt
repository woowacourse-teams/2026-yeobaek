package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.noRippleClickable
import com.yeobaek.core.designsystem.theme.YeobaekLine
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.mockPassages
import com.yeobaek.feature.reader.model.PassageUiModel

@Composable
fun PassageItem(
    passage: PassageUiModel,
    fontSize: Int,
    onClick: () -> Unit,
    showUnderline: Boolean,
    modifier: Modifier = Modifier,
) {
    if (showUnderline) {
        UnderlinedPassageText(
            text = passage.content,
            fontSize = fontSize,
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        Text(
            text = passage.content,
            modifier = modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 2f).sp,
                letterSpacing = 1.sp,
            ),
        )
    }
}

@Composable
private fun UnderlinedPassageText(
    text: String,
    fontSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val underlineOffset = 8.dp

    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Text(
        text = text,
        onTextLayout = { result ->
            textLayoutResult = result
        },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val layoutResult = textLayoutResult ?: return@drawBehind

                for (lineIndex in 0 until layoutResult.lineCount) {
                    val lineLeft =
                        layoutResult.getLineLeft(lineIndex)

                    val lineRight =
                        layoutResult.getLineRight(lineIndex)

                    val baseline =
                        layoutResult.getLineBaseline(lineIndex)

                    drawLine(
                        color = YeobaekLine,
                        start = Offset(
                            x = lineLeft,
                            y = baseline + underlineOffset.toPx(),
                        ),
                        end = Offset(
                            x = lineRight,
                            y = baseline + underlineOffset.toPx(),
                        ),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .noRippleClickable(onClick = onClick),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 2f).sp,
            letterSpacing = 1.sp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun PassageItemPreview() {
    YeobaekTheme {
        PassageItem(
            passage = mockPassages[0],
            fontSize = 18,
            onClick = {},
            showUnderline = true,
        )
    }
}
