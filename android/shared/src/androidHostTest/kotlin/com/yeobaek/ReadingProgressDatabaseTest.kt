package com.yeobaek

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.yeobaek.core.readingprogress.ReadingProgressStore
import com.yeobaek.data.local.readingprogress.ReadingProgressDatabase
import com.yeobaek.data.local.readingprogress.RoomReadingProgressStore
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ReadingProgressDatabaseTest : ReadingProgressStoreContract() {
    override suspend fun withDatabase(block: suspend (() -> ReadingProgressStore) -> Unit) {
        val directory = Files.createTempDirectory("reading-progress-test").toFile()
        try {
            block {
                RoomReadingProgressStore(
                    Room.databaseBuilder<ReadingProgressDatabase>(
                        context = RuntimeEnvironment.getApplication(),
                        name = directory.resolve("progress.db").absolutePath,
                    ).setDriver(AndroidSQLiteDriver())
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .build(),
                )
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
