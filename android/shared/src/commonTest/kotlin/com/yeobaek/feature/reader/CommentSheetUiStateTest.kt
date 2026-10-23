package com.yeobaek.feature.reader

import com.yeobaek.feature.reader.model.CommentUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommentSheetUiStateTest {
    @Test
    fun preservesPendingLikeWhenEditReplacesComment() {
        val current = comment(likeCount = 4L, likedByMe = true, isLikePending = true)
        val saved = current.copy(content = "수정된 댓글", likeCount = 3L, likedByMe = false, isLikePending = false)
        val sheet = CommentSheetUiState(
            sentenceId = 1,
            sentenceContent = "문장",
            comments = listOf(current),
            editingCommentId = current.commentId,
            isSubmitting = true,
        )

        val result = sheet.withCommentSaved(saved, current.commentId)

        assertEquals("수정된 댓글", result.comments.single().content)
        assertEquals(4L, result.comments.single().likeCount)
        assertTrue(result.comments.single().likedByMe)
        assertTrue(result.comments.single().isLikePending)
        assertFalse(result.isSubmitting)
    }

    @Test
    fun preservesResolvedLikeWhenLateEditResponseReplacesComment() {
        val current = comment(likeCount = 4L, likedByMe = true, isLikePending = false)
        val saved = current.copy(content = "수정된 댓글", likeCount = 3L, likedByMe = false)
        val sheet = CommentSheetUiState(
            sentenceId = 1,
            sentenceContent = "문장",
            comments = listOf(current),
            editingCommentId = current.commentId,
            isSubmitting = true,
        )

        val result = sheet.withCommentSaved(saved, current.commentId)

        assertEquals("수정된 댓글", result.comments.single().content)
        assertEquals(4L, result.comments.single().likeCount)
        assertTrue(result.comments.single().likedByMe)
        assertFalse(result.comments.single().isLikePending)
    }

    private fun comment(
        likeCount: Long,
        likedByMe: Boolean,
        isLikePending: Boolean,
    ) = CommentUiModel(
        commentId = 7,
        memberId = 9,
        nickname = "독자",
        content = "기존 댓글",
        createdAt = "2026-10-23T10:00:00",
        updatedAt = null,
        isMine = true,
        likeCount = likeCount,
        likedByMe = likedByMe,
        isLikePending = isLikePending,
    )
}
