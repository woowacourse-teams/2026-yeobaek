package com.yeobaek.feature.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderStateHolderTest {
    @Test
    fun editingMineCommentUpdatesContentWithoutChangingCount() {
        val stateHolder = ReaderStateHolder(
            initialUiState = ReaderUiState(passages = mockPassages),
        )

        stateHolder.openPassageComments(mockPassages[4])
        stateHolder.startEditingComment(commentId = 12L)
        stateHolder.updateCommentInput("수정한 댓글")
        stateHolder.submitComment()

        val commentSheet = stateHolder.uiState.commentSheet
        val editedComment = commentSheet?.comments?.first { comment ->
            comment.commentId == 12L
        }
        assertEquals("수정한 댓글", editedComment?.content)
        assertEquals(3, commentSheet?.comments?.size)
        assertEquals(3, stateHolder.uiState.passages[4].commentCount)
        assertNull(commentSheet?.editingCommentId)
        assertEquals("", commentSheet?.input)
    }

    @Test
    fun deletingMineCommentRemovesItAndUpdatesPassageCount() {
        val stateHolder = ReaderStateHolder(
            initialUiState = ReaderUiState(passages = mockPassages),
        )

        stateHolder.openPassageComments(mockPassages[4])
        stateHolder.requestDeleteComment(commentId = 12L)
        stateHolder.confirmDeleteComment()

        val commentSheet = stateHolder.uiState.commentSheet
        assertEquals(listOf(10L, 11L), commentSheet?.comments?.map { it.commentId })
        assertEquals(2, stateHolder.uiState.passages[4].commentCount)
        assertNull(commentSheet?.deletingCommentId)
    }

    @Test
    fun otherMembersCommentCannotBeEditedOrDeleted() {
        val stateHolder = ReaderStateHolder(
            initialUiState = ReaderUiState(passages = mockPassages),
        )

        stateHolder.openPassageComments(mockPassages[4])
        stateHolder.startEditingComment(commentId = 10L)
        stateHolder.requestDeleteComment(commentId = 10L)

        val commentSheet = stateHolder.uiState.commentSheet
        assertNull(commentSheet?.editingCommentId)
        assertNull(commentSheet?.deletingCommentId)
        assertEquals("", commentSheet?.input)
    }
}
