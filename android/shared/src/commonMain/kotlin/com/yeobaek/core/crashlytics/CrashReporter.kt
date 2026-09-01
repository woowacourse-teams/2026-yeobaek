package com.yeobaek.core.network

import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.FirebaseCrashSink

class CrashReporter(
    private val isDebug: Boolean,
    private val sink: FirebaseCrashSink = FirebaseCrashSink,
) {
    fun track(
        level: CrashLogLevel,
        context: CrashContext,
    ) {
        if (level == CrashLogLevel.DEBUG && !isDebug) return

        sink.setCustomKeys(context.toCustomKeys(errorType = NO_ERROR))
        sink.log(context.toLogLine(level = level))
    }

    fun updateContext(context: CrashContext) {
        sink.setCustomKeys(context.toCustomKeys(errorType = NO_ERROR))
    }

    fun recordException(
        throwable: Throwable,
        context: CrashContext,
    ) {
        val errorType = throwable::class.simpleName ?: UNKNOWN_ERROR
        sink.setCustomKeys(context.toCustomKeys(errorType = errorType))
        sink.log(context.toLogLine(level = CrashLogLevel.ERROR, errorType = errorType))
        sink.recordException(throwable)
    }
}

private fun CrashContext.toCustomKeys(errorType: String): Map<String, Any> = mapOf(
    KEY_SCREEN to screen.value,
    KEY_OPERATION to operation.value,
    KEY_BOOK_ID to (bookId ?: EMPTY_LONG),
    KEY_CHAPTER_SEQUENCE to (chapterSequence ?: EMPTY_INT),
    KEY_PASSAGE_SEQUENCE to (passageSequence ?: EMPTY_INT),
    KEY_ITEM_COUNT to (itemCount ?: EMPTY_INT),
    KEY_ERROR_TYPE to errorType,
)

private fun CrashContext.toLogLine(
    level: CrashLogLevel,
    errorType: String? = null,
): String = buildString {
    append("level=")
    append(level.name)
    append(" operation=")
    append(operation.value)
    append(" screen=")
    append(screen.value)
    bookId?.let { append(" book_id=").append(it) }
    chapterSequence?.let { append(" chapter_sequence=").append(it) }
    passageSequence?.let { append(" passage_sequence=").append(it) }
    itemCount?.let { append(" item_count=").append(it) }
    errorType?.let { append(" error_type=").append(it) }
}

private const val KEY_SCREEN = "screen"
private const val KEY_OPERATION = "operation"
private const val KEY_BOOK_ID = "book_id"
private const val KEY_CHAPTER_SEQUENCE = "chapter_sequence"
private const val KEY_PASSAGE_SEQUENCE = "passage_sequence"
private const val KEY_ITEM_COUNT = "item_count"
private const val KEY_ERROR_TYPE = "error_type"
private const val NO_ERROR = "none"
private const val UNKNOWN_ERROR = "unknown"
private const val EMPTY_INT = -1
private const val EMPTY_LONG = -1L
