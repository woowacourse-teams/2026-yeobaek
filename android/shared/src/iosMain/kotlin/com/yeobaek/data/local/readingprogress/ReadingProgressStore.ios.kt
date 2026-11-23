package com.yeobaek.data.local.readingprogress

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createReadingProgressStore(): RoomReadingProgressStore {
    val manager = NSFileManager.defaultManager
    val directory = manager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask).first() as NSURL
    check(manager.createDirectoryAtURL(directory, true, null, null))
    return createReadingProgressStore(requireNotNull(directory.path) + "/reading-progress.db")
}

internal fun createReadingProgressStore(path: String): RoomReadingProgressStore =
    Room.databaseBuilder<ReadingProgressDatabase>(name = path).buildProgressStore()
