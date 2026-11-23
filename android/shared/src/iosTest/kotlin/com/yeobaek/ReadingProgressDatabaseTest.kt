package com.yeobaek

import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.data.local.readingprogress.createReadingProgressStore
import kotlin.uuid.Uuid
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

@OptIn(ExperimentalForeignApi::class)
class ReadingProgressDatabaseTest : ReadingProgressStoreContract() {
    override suspend fun withDatabase(block: suspend (() -> ReadingProgressStore) -> Unit) {
        val directory = NSTemporaryDirectory() + "reading-progress-test-${Uuid.random()}"
        val manager = NSFileManager.defaultManager
        check(manager.createDirectoryAtPath(directory, true, null, null))
        try {
            block { createReadingProgressStore("$directory/progress.db") }
        } finally {
            check(manager.removeItemAtPath(directory, null))
        }
    }
}
