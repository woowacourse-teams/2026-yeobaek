package com.yeobaek

import androidx.compose.ui.text.TextRange
import com.yeobaek.feature.reader.ReaderPassageText
import com.yeobaek.feature.reader.model.SentenceUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderPassageTextVisibilityTest {
    private val sentences = listOf(
        SentenceUiModel(1, 1, "가나다 ", 1),
        SentenceUiModel(2, 2, "라마바 ", 2),
        SentenceUiModel(3, 3, "사아자", 0),
    )
    private val text = ReaderPassageText(sentences)

    @Test
    fun visibleRangesCanIncludePartsOfMultipleSentencesInBookOrder() {
        val firstRange = text.sentenceRanges[0]
        val secondRange = text.sentenceRanges[1]

        assertEquals(
            listOf(1L, 2L),
            text.commentedSentencesIn(
                TextRange(firstRange.end - 3, secondRange.start + 1),
                sentences,
            ).map { it.sentenceId },
        )
    }

    @Test
    fun trailingWhitespaceDoesNotMakeASentenceVisible() {
        val firstEnd = text.sentenceRanges[0].end

        assertTrue(text.commentedSentencesIn(TextRange(firstEnd - 1, firstEnd), sentences).isEmpty())
    }

    @Test
    fun usesUpdatedCommentCountsWithTheSameTextLayout() {
        val updated = listOf(sentences[0].copy(commentCount = 0), sentences[1], sentences[2].copy(commentCount = 5))

        assertTrue(text.matches(updated))
        assertEquals(
            listOf(2 to 2, 3 to 5),
            text.commentedSentencesIn(TextRange(0, text.text.length), updated)
                .map { it.sequence to it.commentCount },
        )
        assertFalse(text.matches(listOf(sentences[0].copy(content = "바뀐 문장 ")) + sentences.drop(1)))
    }
}
