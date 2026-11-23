package com.yeobaek.core.network

import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashSink
import com.yeobaek.core.crashlytics.FirebaseCrashSink

class CrashReporter(
    private val isDebug: Boolean,
    private val sink: CrashSink = FirebaseCrashSink,
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

internal fun CrashContext.toCustomKeys(errorType: String): Map<String, Any> = mapOf(
    KEY_SCREEN to screen.value,
    KEY_OPERATION to operation.value,
    KEY_BOOK_ID to (bookId ?: EMPTY_LONG),
    KEY_PUBLIC_ROOM_ID to (publicRoomId ?: EMPTY_LONG),
    KEY_READING_SPACE to (readingSpace ?: NO_VALUE),
    KEY_ENTRY_POINT to (entryPoint ?: NO_VALUE),
    KEY_CHAPTER_SEQUENCE to (chapterSequence ?: EMPTY_INT),
    KEY_PASSAGE_SEQUENCE to (passageSequence ?: EMPTY_INT),
    KEY_ITEM_COUNT to (itemCount ?: EMPTY_INT),
    KEY_ERROR_TYPE to errorType,
    KEY_GROUP_ID to (groupId ?: EMPTY_LONG),
    KEY_PROGRESS_REVISION to (progressRevision ?: EMPTY_LONG),
    KEY_SAVE_ATTEMPT_ID to (saveAttemptId ?: NO_VALUE),
    KEY_RESUME_SOURCE to (resumeSource ?: NO_VALUE),
    KEY_EXPECTED_PASSAGE_SEQUENCE to (expectedPassageSequence ?: EMPTY_INT),
    KEY_INITIAL_PASSAGE_SEQUENCE to (initialPassageSequence ?: EMPTY_INT),
)

internal fun CrashContext.toLogLine(
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
    publicRoomId?.let { append(" public_room_id=").append(it) }
    readingSpace?.let { append(" reading_space=").append(it) }
    entryPoint?.let { append(" entry_point=").append(it) }
    chapterSequence?.let { append(" chapter_sequence=").append(it) }
    passageSequence?.let { append(" passage_sequence=").append(it) }
    itemCount?.let { append(" item_count=").append(it) }
    errorType?.let { append(" error_type=").append(it) }
    groupId?.let { append(" group_id=").append(it) }
    progressRevision?.let { append(" progress_revision=").append(it) }
    saveAttemptId?.let { append(" save_attempt_id=").append(it) }
    resumeSource?.let { append(" resume_source=").append(it) }
    expectedPassageSequence?.let { append(" expected_passage_sequence=").append(it) }
    initialPassageSequence?.let { append(" initial_passage_sequence=").append(it) }
}

private const val KEY_SCREEN = "screen"
private const val KEY_OPERATION = "operation"
private const val KEY_BOOK_ID = "book_id"
private const val KEY_PUBLIC_ROOM_ID = "public_room_id"
private const val KEY_READING_SPACE = "reading_space"
private const val KEY_ENTRY_POINT = "entry_point"
private const val KEY_CHAPTER_SEQUENCE = "chapter_sequence"
private const val KEY_PASSAGE_SEQUENCE = "passage_sequence"
private const val KEY_ITEM_COUNT = "item_count"
private const val KEY_ERROR_TYPE = "error_type"
private const val KEY_GROUP_ID = "group_id"
private const val KEY_PROGRESS_REVISION = "progress_revision"
private const val KEY_SAVE_ATTEMPT_ID = "save_attempt_id"
private const val KEY_RESUME_SOURCE = "resume_source"
private const val KEY_EXPECTED_PASSAGE_SEQUENCE = "expected_passage_sequence"
private const val KEY_INITIAL_PASSAGE_SEQUENCE = "initial_passage_sequence"
private const val NO_ERROR = "none"
private const val NO_VALUE = "none"
private const val UNKNOWN_ERROR = "unknown"
private const val EMPTY_INT = -1
private const val EMPTY_LONG = -1L
