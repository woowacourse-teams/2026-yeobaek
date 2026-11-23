package com.yeobaek.core.readingprogress

import kotlin.random.Random
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReadingProgressSyncCoordinator(
    private val memberId: StateFlow<Int?>,
    private val store: ReadingProgressStore,
    private val sender: ReadingProgressSender,
    private val scope: CoroutineScope,
    private val onEvent: (ReadingProgressSyncEvent) -> Unit = {},
    private val maxAttempts: Int = 3,
    private val retryDelayMillis: Long = 1_000L,
    private val checkpointSyncDelayMillis: Long = 1_000L,
) {
    private val checkpoints = Channel<StorageCommand>(Channel.UNLIMITED)
    private val wakeups = Channel<ReadingProgressSyncReason>(Channel.CONFLATED)

    init {
        require(maxAttempts in 1..10)
        require(retryDelayMillis in 0..60_000)
        require(checkpointSyncDelayMillis in 0..60_000)
        scope.launch {
            try {
                for (command in checkpoints) {
                    when (command) {
                        is Checkpoint -> persist(command)
                        is RemoveMember -> {
                            try {
                                store.removeMember(command.memberId)
                                command.result.complete(Unit)
                            } catch (exception: CancellationException) {
                                command.result.cancel(exception)
                                throw exception
                            } catch (exception: Exception) {
                                command.result.completeExceptionally(exception)
                            }
                        }
                    }
                }
            } finally {
                checkpoints.close()
                while (true) {
                    val checkpoint = checkpoints.tryReceive().getOrNull() ?: break
                    checkpoint.result.cancel()
                }
            }
        }
        scope.launch {
            memberId.collectLatest { member ->
                if (member == null) return@collectLatest
                requestSync(ReadingProgressSyncReason.ACCOUNT_CHANGED)
                while (true) syncPending(member, nextSyncReason())
            }
        }
    }

    fun queueCheckpoint(
        key: ReadingProgressKey,
        passageId: Long,
        passageSequence: Int,
        reason: ReadingProgressSyncReason = ReadingProgressSyncReason.CHECKPOINT,
    ): Deferred<ReadingProgress> {
        val result = CompletableDeferred<ReadingProgress>()
        if (memberId.value != key.memberId) {
            result.completeExceptionally(ReadingProgressAccountChangedException())
        } else {
            enqueue(Checkpoint(key, passageId, passageSequence, reason, result))
        }
        return result
    }

    fun removeMember(member: Int): Deferred<Unit> {
        val result = CompletableDeferred<Unit>()
        enqueue(RemoveMember(member, result))
        return result
    }

    private fun enqueue(command: StorageCommand) {
        val cancellation = scope.coroutineContext[Job]?.invokeOnCompletion { command.result.cancel() }
        command.result.invokeOnCompletion { cancellation?.dispose() }
        if (checkpoints.trySend(command).isFailure) command.result.cancel()
    }

    fun requestSync(reason: ReadingProgressSyncReason = ReadingProgressSyncReason.CHECKPOINT) {
        wakeups.trySend(reason)
    }

    private suspend fun persist(checkpoint: Checkpoint) {
        try {
            requireCurrentMember(checkpoint.key.memberId)
            val progress = store.checkpoint(checkpoint.key, checkpoint.passageId, checkpoint.passageSequence)
            currentCoroutineContext().ensureActive()
            requireCurrentMember(checkpoint.key.memberId)
            checkpoint.result.complete(progress)
            report(ReadingProgressSyncEvent.CheckpointSaved(progress, checkpoint.reason))
            if (progress.pending) requestSync(checkpoint.reason)
        } catch (exception: CancellationException) {
            checkpoint.result.cancel(exception)
            throw exception
        } catch (exception: Exception) {
            checkpoint.result.completeExceptionally(exception)
            report(ReadingProgressSyncEvent.CheckpointFailed(checkpoint.key, checkpoint.passageSequence, exception))
        }
    }

    private suspend fun nextSyncReason(): ReadingProgressSyncReason {
        var reason = wakeups.receive()
        while (reason == ReadingProgressSyncReason.CHECKPOINT && checkpointSyncDelayMillis > 0) {
            reason = withTimeoutOrNull(checkpointSyncDelayMillis) { wakeups.receive() } ?: return reason
        }
        return reason
    }

    private suspend fun syncPending(member: Int, reason: ReadingProgressSyncReason) {
        try {
            requireCurrentMember(member)
            val pending = store.pending(member)
            for (progress in pending) syncKey(progress.key, reason)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ReadingProgressAccountChangedException) {
            // The new account's collector performs its own pending lookup.
        } catch (exception: Exception) {
            report(ReadingProgressSyncEvent.PendingReadFailed(member, exception))
        }
    }

    private suspend fun syncKey(key: ReadingProgressKey, reason: ReadingProgressSyncReason) {
        var attempts = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            requireCurrentMember(key.memberId)
            val progress = store.get(key)?.takeIf { it.pending } ?: return
            currentCoroutineContext().ensureActive()
            requireCurrentMember(key.memberId)
            val attemptId = "${Random.nextLong().toULong().toString(16)}-${Random.nextLong().toULong().toString(16)}"
            val started = TimeSource.Monotonic.markNow()
            var storedSequence: Int? = null
            var failure: Exception? = null
            try {
                storedSequence = sender.save(progress)
                currentCoroutineContext().ensureActive()
                requireCurrentMember(key.memberId)
                if (storedSequence != progress.passageSequence) {
                    throw ReadingProgressSyncException(false, "Server returned a different reading position")
                }
                store.acknowledge(key, progress.revision)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ReadingProgressAccountChangedException) {
                throw exception
            } catch (exception: Exception) {
                failure = exception
            }
            report(
                ReadingProgressSyncEvent.SyncFinished(
                    progress = progress,
                    attemptId = attemptId,
                    reason = reason,
                    durationMillis = started.elapsedNow().inWholeMilliseconds,
                    storedPassageSequence = storedSequence,
                    error = failure,
                ),
            )
            if (failure == null) {
                attempts = 0
                continue
            }
            attempts++
            if (failure !is ReadingProgressSyncException || !failure.retryable || attempts >= maxAttempts) return
            delay(retryDelayMillis * (1L shl (attempts - 1)))
        }
    }

    private fun requireCurrentMember(expected: Int) {
        if (memberId.value != expected) throw ReadingProgressAccountChangedException()
    }

    private fun report(event: ReadingProgressSyncEvent) {
        try {
            onEvent(event)
        } catch (_: Exception) {
            // Diagnostics must not change the outcome of a durable checkpoint or acknowledgement.
        }
    }

    private sealed interface StorageCommand {
        val result: CompletableDeferred<*>
    }

    private data class RemoveMember(
        val memberId: Int,
        override val result: CompletableDeferred<Unit>,
    ) : StorageCommand

    private data class Checkpoint(
        val key: ReadingProgressKey,
        val passageId: Long,
        val passageSequence: Int,
        val reason: ReadingProgressSyncReason,
        override val result: CompletableDeferred<ReadingProgress>,
    ) : StorageCommand
}

class ReadingProgressAccountChangedException : Exception("The reading progress account changed")
