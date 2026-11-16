package com.yeobaek.feature.reader

import androidx.compose.runtime.mutableStateMapOf
import com.yeobaek.data.model.CommentLikeModel
import com.yeobaek.feature.reader.model.CommentUiModel
import com.yeobaek.feature.reader.model.SentenceUiModel
import com.yeobaek.feature.reader.model.toggleLikeOptimistically
import com.yeobaek.feature.reader.model.withLikeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface ReaderPanelThread {
    data class Loaded(val comments: List<CommentUiModel>) : ReaderPanelThread
    data object Failed : ReaderPanelThread
}

enum class ReaderPanelCommentAction {
    Edit,
    Delete,
    Report,
}

class ReaderPanelComments(
    private val scope: CoroutineScope,
    private val onCommentsViewed: (Long) -> Unit = {},
    private val fetch: suspend (Long) -> List<CommentUiModel>,
) {
    private val entries = mutableStateMapOf<Long, ReaderPanelThread>()
    val threads: Map<Long, ReaderPanelThread> get() = entries

    private var visibleCounts: Map<Long, Int> = emptyMap()
    private val cachedCounts = mutableMapOf<Long, Int>()
    private val requests = mutableMapOf<Long, Request>()
    private val pendingLikes = mutableMapOf<Long, CommentUiModel>()
    private val permits = Semaphore(3)

    fun updateVisible(sentences: List<SentenceUiModel>) {
        val previouslyVisible = visibleCounts.keys
        visibleCounts = sentences.associate { it.sentenceId to it.commentCount }

        (requests.keys - visibleCounts.keys).forEach(::cancel)
        visibleCounts.forEach { (id, count) ->
            if (cachedCounts[id] != count || (id !in previouslyVisible && entries[id] == ReaderPanelThread.Failed)) {
                load(id, count)
            }
        }
        trimCache()
    }

    fun refresh(sentenceId: Long) {
        cancel(sentenceId)
        cachedCounts.remove(sentenceId)
        if (sentenceId !in visibleCounts) {
            entries.remove(sentenceId)
        } else {
            load(sentenceId, visibleCounts.getValue(sentenceId))
        }
    }

    fun replace(sentenceId: Long, comments: List<CommentUiModel>) {
        cancel(sentenceId)
        save(sentenceId, comments.size, ReaderPanelThread.Loaded(comments))
    }

    fun isLikePending(commentId: Long): Boolean = commentId in pendingLikes

    fun beginLike(sentenceId: Long, commentId: Long): CommentUiModel? {
        if (isLikePending(commentId)) return null
        val thread = entries[sentenceId] as? ReaderPanelThread.Loaded ?: return null
        val comment = thread.comments.firstOrNull { it.commentId == commentId && !it.isLikePending } ?: return null
        val optimistic = comment.toggleLikeOptimistically()
        pendingLikes[commentId] = optimistic
        updateLike(sentenceId, optimistic)
        return comment
    }

    fun finishLike(sentenceId: Long, previous: CommentUiModel, result: CommentLikeModel?): CommentUiModel {
        pendingLikes.remove(previous.commentId)
        val settled = previous.copy(
            likeCount = result?.likeCount ?: previous.likeCount,
            likedByMe = result?.likedByMe ?: previous.likedByMe,
            isLikePending = false,
        )
        updateLike(sentenceId, settled)
        return settled
    }

    fun updateLike(sentenceId: Long, liked: CommentUiModel) {
        val thread = entries[sentenceId] as? ReaderPanelThread.Loaded ?: return
        entries[sentenceId] = thread.copy(
            comments = thread.comments.map { comment ->
                if (comment.commentId == liked.commentId) comment.withLikeFrom(liked) else comment
            },
        )
        if (!liked.isLikePending && sentenceId in requests) refresh(sentenceId)
    }

    private fun load(sentenceId: Long, expectedCount: Int) {
        if (requests[sentenceId]?.expectedCount == expectedCount) return
        cancel(sentenceId)
        if (entries[sentenceId] == ReaderPanelThread.Failed) entries.remove(sentenceId)

        val job = scope.launch(start = CoroutineStart.LAZY) {
            val ownJob = currentCoroutineContext()[Job]
            try {
                val comments = permits.withPermit { fetch(sentenceId) }
                currentCoroutineContext().ensureActive()
                save(sentenceId, expectedCount, ReaderPanelThread.Loaded(comments))
                onCommentsViewed(sentenceId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                save(sentenceId, expectedCount, ReaderPanelThread.Failed)
            } finally {
                if (requests[sentenceId]?.job === ownJob) requests.remove(sentenceId)
            }
        }
        requests[sentenceId] = Request(expectedCount, job)
        job.start()
    }

    private fun save(sentenceId: Long, count: Int, thread: ReaderPanelThread) {
        cachedCounts.remove(sentenceId)
        cachedCounts[sentenceId] = count
        entries[sentenceId] = if (thread is ReaderPanelThread.Loaded && pendingLikes.isNotEmpty()) {
            thread.copy(
                comments = thread.comments.map { comment ->
                    pendingLikes[comment.commentId]?.let(comment::withLikeFrom) ?: comment
                },
            )
        } else {
            thread
        }
        trimCache()
    }

    private fun cancel(sentenceId: Long) {
        requests.remove(sentenceId)?.job?.cancel()
    }

    private fun trimCache() {
        val excess = (cachedCounts.size - CACHE_SIZE).coerceAtLeast(0)
        if (excess == 0) return
        cachedCounts.keys.filterNot(visibleCounts::containsKey).take(excess).forEach { id ->
            cachedCounts.remove(id)
            entries.remove(id)
        }
    }

    private data class Request(val expectedCount: Int, val job: Job)

    private companion object {
        const val CACHE_SIZE = 64
    }
}
