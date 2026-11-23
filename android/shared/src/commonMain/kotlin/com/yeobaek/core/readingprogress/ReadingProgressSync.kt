package com.yeobaek.core.readingprogress

fun interface ReadingProgressSender {
    suspend fun save(progress: ReadingProgress): Int
}

enum class ReadingProgressSyncReason {
    CHECKPOINT,
    BACK,
    BACKGROUND,
    FOREGROUND,
    NETWORK_RESTORED,
    ACCOUNT_CHANGED,
}

class ReadingProgressSyncException(
    val retryable: Boolean,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

sealed interface ReadingProgressSyncEvent {
    data class CheckpointSaved(
        val progress: ReadingProgress,
        val reason: ReadingProgressSyncReason,
    ) : ReadingProgressSyncEvent

    data class CheckpointFailed(
        val key: ReadingProgressKey,
        val passageSequence: Int,
        val error: Exception,
    ) : ReadingProgressSyncEvent

    data class SyncFinished(
        val progress: ReadingProgress,
        val attemptId: String,
        val reason: ReadingProgressSyncReason,
        val durationMillis: Long,
        val storedPassageSequence: Int?,
        val error: Exception?,
    ) : ReadingProgressSyncEvent

    data class PendingReadFailed(val memberId: Int, val error: Exception) : ReadingProgressSyncEvent
}
