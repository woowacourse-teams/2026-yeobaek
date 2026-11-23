package com.yeobaek.data.local.readingprogress

import android.content.Context
import androidx.room.Room

fun createReadingProgressStore(context: Context): RoomReadingProgressStore {
    val application = context.applicationContext
    return Room.databaseBuilder<ReadingProgressDatabase>(
        context = application,
        name = application.getDatabasePath("reading-progress.db").absolutePath,
    ).buildProgressStore()
}
