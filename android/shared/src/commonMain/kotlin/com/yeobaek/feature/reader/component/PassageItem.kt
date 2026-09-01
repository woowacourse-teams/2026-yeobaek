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
    val (passageText, sentenceTextRanges) = remember(passage.sentences) {
        val sentenceRanges = mutableListOf<SentenceTextRange>()
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

                if (sentenceStart < length) {
                    sentenceRanges += SentenceTextRange(
                        sentence = sentence,
                        start = sentenceStart,
                        end = length,
                    )
                }
            }
        }
        text to sentenceRanges
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

                sentenceTextRanges
                    .filter { sentenceRange -> sentenceRange.sentence.hasComment }
                    .forEach { sentenceRange ->
                        val sentenceStart = sentenceRange.start
                        val sentenceEnd = sentenceRange.end
                        val firstLine = layoutResult.getLineForOffset(sentenceStart)
                        val lastLine = layoutResult.getLineForOffset(sentenceEnd - 1)

                        for (lineIndex in firstLine..lastLine) {
                            val lineStart = layoutResult.getLineStart(lineIndex)
                            val lineEnd = layoutResult.getLineEnd(
                                lineIndex = lineIndex,
                                visibleEnd = true,
                            )
                            val underlineStart = maxOf(sentenceStart, lineStart)
                            val underlineEnd = minOf(sentenceEnd, lineEnd)

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

private data class SentenceTextRange(
    val sentence: SentenceUiModel,
    val start: Int,
    val end: Int,
)

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
