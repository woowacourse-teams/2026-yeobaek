package com.yeobaek.feature.reader

import androidx.compose.ui.text.TextRange
import com.yeobaek.feature.reader.model.SentenceUiModel

data class ReaderTextPosition(
    val sentenceId: Long,
    val sourceOffset: Int,
)

class ReaderPassageText(private val sentences: List<SentenceUiModel>) {
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

    fun matches(sentences: List<SentenceUiModel>): Boolean {
        if (this.sentences === sentences) return true
        if (this.sentences.size != sentences.size) return false
        return sentences.indices.all { index ->
            val original = this.sentences[index]
            val current = sentences[index]
            original.sentenceId == current.sentenceId && original.content == current.content
        }
    }

    fun commentedSentencesIn(
        visibleRange: TextRange,
        sentences: List<SentenceUiModel>,
    ): List<SentenceUiModel> = sentences.filterIndexed { index, sentence ->
        val range = sentenceRanges[index]
        val visibleStart = maxOf(range.start, visibleRange.start)
        val visibleEnd = minOf(range.end, visibleRange.end)
        sentence.hasComment && firstCharacterIn(visibleStart, visibleEnd) != null
    }
}
