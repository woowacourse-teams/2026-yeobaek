package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekHighlight
import com.yeobaek.core.designsystem.theme.YeobaekLine
import com.yeobaek.core.designsystem.theme.YeobaekMaruBuri
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel

@Composable
fun PassageItem(
    passage: PassageUiModel,
    fontSize: Int,
    onSentenceClick: (SentenceUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnSentenceClick by rememberUpdatedState(onSentenceClick)
    val underlineOffset = 8.dp
    val passageTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = YeobaekMaruBuri,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 2f).sp,
        letterSpacing = 1.sp,
    )
    val (passageText, underlineTextRanges) = remember(passage.sentences) {
        val commentedSentenceRanges = mutableListOf<TextRange>()
        val text = buildAnnotatedString {
            passage.sentences.forEach { sentence ->
                val sentenceStart = length
                withLink(
                    LinkAnnotation.Clickable(
                        tag = sentence.sentenceId.toString(),
                        styles = TextLinkStyles(
                            pressedStyle = SpanStyle(
                                background = YeobaekHighlight,
                            ),
                        ),
                        linkInteractionListener = {
                            currentOnSentenceClick(sentence)
                        },
                    ),
                ) {
                    append(sentence.content.allowCharacterBreaks())
                }

                if (sentence.hasComment && sentenceStart < length) {
                    commentedSentenceRanges += TextRange(
                        start = sentenceStart,
                        end = length,
                    )
                }
            }
        }
        val underlineRanges = commentedSentenceRanges.mapNotNull { sentenceRange ->
            text.text.underlineRangeOf(sentenceRange)
        }
        text to underlineRanges
    }
    var textLayoutResult by remember(passageText) {
        mutableStateOf<TextLayoutResult?>(null)
    }
    Text(
        text = passageText,
        onTextLayout = { result ->
            textLayoutResult = result
        },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val layoutResult = textLayoutResult ?: return@drawBehind

                underlineTextRanges.forEach { underlineRange ->
                    val firstLine = layoutResult.getLineForOffset(underlineRange.start)
                    val lastLine = layoutResult.getLineForOffset(underlineRange.end - 1)

                    for (lineIndex in firstLine..lastLine) {
                        val lineStart = layoutResult.getLineStart(lineIndex)
                        val lineEnd = layoutResult.getLineEnd(
                            lineIndex = lineIndex,
                            visibleEnd = true,
                        )
                        val underlineStart = maxOf(underlineRange.start, lineStart)
                        val underlineEnd = minOf(underlineRange.end, lineEnd)

                        if (underlineStart >= underlineEnd) continue

                        val startX = if (underlineStart == lineStart) {
                            layoutResult.getLineLeft(lineIndex)
                        } else {
                            layoutResult.getHorizontalPosition(
                                offset = underlineStart,
                                usePrimaryDirection = true,
                            )
                        }
                        val endX = if (underlineEnd == lineEnd) {
                            layoutResult.getLineRight(lineIndex)
                        } else {
                            layoutResult.getHorizontalPosition(
                                offset = underlineEnd,
                                usePrimaryDirection = true,
                            )
                        }
                        val underlineY = layoutResult.getLineBaseline(lineIndex) +
                            underlineOffset.toPx()

                        drawLine(
                            color = YeobaekLine,
                            start = Offset(x = startX, y = underlineY),
                            end = Offset(x = endX, y = underlineY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
            },
        style = passageTextStyle,
    )
}

internal fun String.underlineRangeOf(range: TextRange): TextRange? {
    var end = range.end

    while (end > range.start && this[end - 1].isWhitespaceOrZeroWidthSpace()) {
        end--
    }

    return if (end > range.start) TextRange(start = range.start, end = end) else null
}

private fun Char.isWhitespaceOrZeroWidthSpace(): Boolean = isWhitespace() || this == '\u200B'

private fun String.allowCharacterBreaks(): String =
    buildString {
        this@allowCharacterBreaks.forEach { character ->
            append(character)

            if (!character.isWhitespace()) {
                append('\u200B')
            }
        }
    }

@Preview(showBackground = true)
@Composable
private fun PassageItemPreview() {
    YeobaekTheme {
        PassageItem(
            passage = PassageUiModel(
                passageId = 2,
                sequence = 2,
                chapterId = 1,
                sentences = listOf(
                    SentenceUiModel(
                        sentenceId = 201,
                        sequence = 1,
                        content = "\"새는 알에서 나오려고 투쟁한다.",
                        commentCount = 2,
                    ),
                    SentenceUiModel(
                        sentenceId = 202,
                        sequence = 2,
                        content = "알은 세계이다.",
                        commentCount = 0,
                    ),
                    SentenceUiModel(
                        sentenceId = 203,
                        sequence = 3,
                        content = "태어나려는 자는 하나의 세계를 깨뜨려야 한다.\"",
                        commentCount = 1,
                    ),
                ),
            ),
            fontSize = 18,
            onSentenceClick = {},
        )
    }
}
