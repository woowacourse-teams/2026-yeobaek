package com.yeobaek.feature.reader.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CommentedSentencesUiModelTest {
    @Test
    fun updateCommentCountChangesOnlyMatchingSentence() {
        val model = CommentedSentencesUiModel(
            sentences = listOf(
                commentedSentence(sentenceId = 1, commentCount = 2),
                commentedSentence(sentenceId = 2, commentCount = 4),
            ),
        )

        val updated = model.updateCommentCount(sentenceId = 1, commentCount = 3)

        assertEquals(listOf(3, 4), updated.sentences.map { sentence -> sentence.commentCount })
    }

    @Test
    fun updateCommentCountRemovesSentenceWhenLastCommentIsDeleted() {
        val model = CommentedSentencesUiModel(
            sentences = listOf(commentedSentence(sentenceId = 1, commentCount = 1)),
        )

        val updated = model.updateCommentCount(sentenceId = 1, commentCount = 0)

        assertEquals(emptyList(), updated.sentences)
    }

    @Test
    fun markCommentsViewedClearsUnreadCountOnlyForMatchingSentence() {
        val model = CommentedSentencesUiModel(
            sentences = listOf(
                commentedSentence(sentenceId = 1, unreadCommentCount = 2),
                commentedSentence(sentenceId = 2, unreadCommentCount = 1),
            ),
        )

        val updated = model.markCommentsViewed(sentenceId = 1)

        assertEquals(listOf(0, 1), updated.sentences.map { sentence -> sentence.unreadCommentCount })
    }

    private fun commentedSentence(
        sentenceId: Long,
        commentCount: Int = 2,
        unreadCommentCount: Int = 0,
    ) = CommentedSentenceUiModel(
        sentenceId = sentenceId,
        passageId = sentenceId,
        content = "문장 $sentenceId",
        passageSequence = sentenceId.toInt(),
        sentenceSequence = 1,
        progress = 10f,
        isFuture = false,
        requiresReveal = false,
        commentCount = commentCount,
        unreadCommentCount = unreadCommentCount,
    )
}
