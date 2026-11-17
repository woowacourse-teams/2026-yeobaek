package com.yeobaek

import com.yeobaek.data.model.CommentLikeModel
import com.yeobaek.feature.reader.ReaderPanelComments
import com.yeobaek.feature.reader.ReaderPanelThread
import com.yeobaek.feature.reader.model.CommentUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPanelCommentsTest {
    @Test
    fun loadsEveryVisibleThreadAndReusesResultsOnReturn() = runTest {
        val requested = mutableListOf<Long>()
        val viewed = mutableListOf<Long>()
        val panel = ReaderPanelComments(this, onCommentsViewed = { viewed.add(it) }) { id ->
            requested.add(id)
            listOf(comment(id))
        }
        val first = SentenceUiModel(1, 1, "첫 문장", 1)
        val second = SentenceUiModel(2, 2, "둘째 문장", 1)

        panel.updateVisible(listOf(first, second))
        panel.updateVisible(listOf(first, second))
        runCurrent()
        panel.updateVisible(emptyList())
        panel.updateVisible(listOf(second))
        runCurrent()

        assertEquals(listOf(1L, 2L), requested)
        assertEquals(requested, viewed)
        assertEquals(ReaderPanelThread.Loaded(listOf(comment(2))), panel.threads[2])
    }

    @Test
    fun leavingTheViewportCancelsAnUnfinishedPreview() = runTest {
        val pending = CompletableDeferred<List<CommentUiModel>>()
        val viewed = mutableListOf<Long>()
        val panel = ReaderPanelComments(this, onCommentsViewed = { viewed.add(it) }) { pending.await() }
        val sentence = SentenceUiModel(1, 1, "문장", 1)

        panel.updateVisible(listOf(sentence))
        runCurrent()
        panel.updateVisible(emptyList())
        pending.complete(listOf(comment(1)))
        runCurrent()

        assertFalse(panel.threads.containsKey(1L))
        assertEquals(emptyList(), viewed)
    }

    @Test
    fun replacingAThreadCancelsTheOlderResponse() = runTest {
        val pending = CompletableDeferred<List<CommentUiModel>>()
        val panel = ReaderPanelComments(this) { pending.await() }
        val sentence = SentenceUiModel(1, 1, "문장", 1)

        panel.updateVisible(listOf(sentence))
        runCurrent()
        panel.replace(1, listOf(comment(2)))
        pending.complete(listOf(comment(1)))
        runCurrent()

        assertEquals(ReaderPanelThread.Loaded(listOf(comment(2))), panel.threads[1])
    }

    @Test
    fun settlingLikeKeepsOtherCommentsAndUsesTheServerCount() = runTest {
        val panel = ReaderPanelComments(this) { emptyList() }
        val original = comment(1)
        val other = comment(2)
        panel.replace(1, listOf(original, other))

        val previous = assertNotNull(panel.beginLike(1, 1))
        assertNull(panel.beginLike(1, 1))
        panel.replace(1, listOf(original, other))
        assertTrue((panel.threads[1] as ReaderPanelThread.Loaded).comments.first().isLikePending)
        panel.finishLike(1, previous, CommentLikeModel(likeCount = 3, likedByMe = true))

        assertEquals(
            ReaderPanelThread.Loaded(listOf(original.copy(likeCount = 3, likedByMe = true), other)),
            panel.threads[1],
        )
    }

    @Test
    fun failedUnlikeRestoresTheOriginalHeartAndCount() = runTest {
        val panel = ReaderPanelComments(this) { emptyList() }
        val original = comment(1).copy(likeCount = 1, likedByMe = true)
        panel.replace(1, listOf(original))

        val previous = assertNotNull(panel.beginLike(1, 1))
        panel.finishLike(1, previous, result = null)

        assertEquals(ReaderPanelThread.Loaded(listOf(original)), panel.threads[1])
        assertFalse(panel.isLikePending(1))
    }

    private fun comment(id: Long) = CommentUiModel(
        commentId = id,
        memberId = 1,
        nickname = "독자",
        content = "댓글",
        createdAt = "2026-11-16T12:00:00",
        updatedAt = null,
        isMine = false,
    )
}
