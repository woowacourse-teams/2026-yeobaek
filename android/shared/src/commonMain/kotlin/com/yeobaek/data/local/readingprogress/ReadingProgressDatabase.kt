package com.yeobaek.data.local.readingprogress

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [ReadingProgressEntity::class], version = 1, exportSchema = true)
@ConstructedBy(ReadingProgressDatabaseConstructor::class)
abstract class ReadingProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ReadingProgressDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ReadingProgressDatabaseConstructor : RoomDatabaseConstructor<ReadingProgressDatabase> {
    override fun initialize(): ReadingProgressDatabase
}

internal fun RoomDatabase.Builder<ReadingProgressDatabase>.buildProgressStore(): RoomReadingProgressStore =
    RoomReadingProgressStore(
        setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build(),
    )
