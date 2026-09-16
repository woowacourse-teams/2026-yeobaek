package com.yeobaek.feature.guide.component.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.zIndex
import com.yeobaek.core.designsystem.theme.YeobaekHighlight
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.designsystem.theme.YeobaekUnderline
import com.yeobaek.feature.guide.model.SentenceGuideUiModel
import com.yeobaek.feature.reader.component.underlineRangeOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderGuideCard(
    sentences: List<SentenceGuideUiModel>,
    onClickCommentSentence: () -> Unit,
    onClickUnCommentSentence: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    sentenceEnabled: Boolean = false,
) {
    var showCommentBottomSheet by remember { mutableStateOf(false) }

    val (passageText, underlineTextRanges) = remember {
        val commentedSenteceRanges = mutableListOf<TextRange>()
        val text = buildAnnotatedString {
            sentences.forEach { sentence ->
                val sentenceStart = length
                withLink(
                    LinkAnnotation.Clickable(
                        tag = sentenceStart.toString(),
                        styles = TextLinkStyles(
                            pressedStyle = SpanStyle(
                                background = YeobaekHighlight,
                            ),
                        ),
                        linkInteractionListener = {
                            if (sentence.isComment) {
                                onClickCommentSentence()
                                showCommentBottomSheet = true
                            } else {
                                onClickUnCommentSentence()
                            }
                        },
                    ),
                ) {
                    append(sentence.content)
                }

                if (sentence.isComment && sentenceStart < length) {
                    commentedSenteceRanges += TextRange(
                        start = sentenceStart,
                        end = length,
                    )
                }
            }
        }
        val underlineRanges = commentedSenteceRanges.mapNotNull { sentenceRange ->
            text.text.underlineRangeOf(sentenceRange)
        }

        text to underlineRanges
    }

    var textLayoutResult by remember(passageText) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "밑줄을 눌러 다른 사람의 \n여백을 읽어보세요",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 30.sp,
                    lineHeight = 40.sp,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "밑줄이 있는 문장에는 함께 읽는 사람의 생각이 있어요. \n먼저 문장을 눌러 여백을 열어보세요.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF7D746B),
                ),
            )
        }
        Box(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxSize(),
        ) {
            Text(
                text = passageText,
                onTextLayout = { result ->
                    textLayoutResult = result
                },
                modifier = Modifier
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
                                    6.dp.toPx()

                                val overlineY = layoutResult.getLineTop(lineIndex)

                                drawLine(
                                    color = YeobaekUnderline,
                                    start = Offset(x = startX, y = underlineY),
                                    end = Offset(x = endX, y = underlineY),
                                    strokeWidth = 1.dp.toPx(),
                                )
                                if (sentenceEnabled) {
                                    drawRoundRect(
                                        color = Color.Yellow.copy(
                                            alpha = 0.5f,
                                        ),
                                        topLeft = Offset(x = startX - 6.dp.toPx(), y = underlineY),
                                        size = Size(
                                            width = endX - startX + 10.dp.toPx(),
                                            height = overlineY - underlineY,
                                        ),
                                        cornerRadius = CornerRadius(x = 8.dp.toPx(), y = 8.dp.toPx()),
                                        style = Stroke(
                                            width = 3.dp.toPx(),
                                            cap = StrokeCap.Round,
                                        ),
                                    )
                                }
                            }
                        }
                    },
            )
            if (showCommentBottomSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                            .fillMaxWidth()
                            .fillMaxHeight(0.78f),
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                        ),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        CommentGuideBottomSheetContent(
                            passage = sentences.first { it.isComment }.content,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Gray.copy(
                                    alpha = 0.3f,
                                ),
                            )
                            .clickable {
                                showCommentBottomSheet = false
                                onCancel()
                            },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "리더화면 가이드 카드")
@Composable
private fun ReaderGuideCardPreview() {
    YeobaekTheme {
        ReaderGuideCard(
            sentences = emptyList(),
            onClickCommentSentence = {},
            onClickUnCommentSentence = {},
            onCancel = {},
        )
    }
}
