package com.yeobaek.feature.reader

import androidx.compose.ui.text.TextRange
import com.yeobaek.feature.reader.model.SentenceUiModel

internal data class ReaderTextPosition(
    val sentenceId: Long,
    val sourceOffset: Int,
)

internal class ReaderPassageText(sentences: List<SentenceUiModel>) {
    private val positions = mutableListOf<ReaderTextPosition?>()
    val sentenceRanges: List<TextRange>
    val text: String

    init {
        val ranges = mutableListOf<TextRange>()
        text = buildString {
            sentences.forEach { sentence ->
                val start = length
                var sourceOffset = 0
                while (sourceOffset < sentence.content.length) {
                    val character = sentence.content[sourceOffset]
                    val isSurrogatePair = character.isHighSurrogate() &&
                        sentence.content.getOrNull(sourceOffset + 1)?.isLowSurrogate() == true
                    val characterLength = if (isSurrogatePair) 2 else 1
                    append(sentence.content, sourceOffset, sourceOffset + characterLength)
                    val isVisible = !character.isWhitespace() && character != '\u200B'
                    positions += if (isVisible) {
                        ReaderTextPosition(sentence.sentenceId, sourceOffset)
                    } else {
                        null
                    }
                    if (isSurrogatePair) positions += null
                    if (isVisible) {
                        append('\u200B')
                        positions += null
                    }
                    sourceOffset += characterLength
                }
                ranges += TextRange(start, length)
            }
        }
        sentenceRanges = ranges
    }

    fun firstCharacterIn(start: Int, end: Int): ReaderTextPosition? =
        (start.coerceAtLeast(0) until end.coerceAtMost(positions.size))
            .firstNotNullOfOrNull { positions[it] }

    fun displayOffsetOf(position: ReaderTextPosition): Int? =
        positions.indexOf(position).takeIf { it >= 0 }
}
