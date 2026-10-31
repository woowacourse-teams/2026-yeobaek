package com.yeobaek

import com.yeobaek.data.local.AndroidInstallationIdStore
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AndroidInstallationIdStoreTest {
    @Test
    fun recreatingStoreKeepsPersistedIdentityAndLeavesNoTemporaryFile() {
        val directory = Files.createTempDirectory("installation-id-test").toFile()
        try {
            val first = AndroidInstallationIdStore(directory).getOrCreate()
            val second = AndroidInstallationIdStore(directory).getOrCreate()
            assertEquals(first, second)
            assertEquals(first, directory.resolve("installation_id").readText())
            assertFalse(directory.resolve("installation_id.tmp").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun concurrentAccessAcrossStoreInstancesCreatesOneIdentity() {
        val directory = Files.createTempDirectory("installation-id-concurrent-test").toFile()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val identities = executor.invokeAll(
                List(20) { Callable { AndroidInstallationIdStore(directory).getOrCreate() } },
            ).map { it.get() }
            assertEquals(1, identities.toSet().size)
            assertEquals(identities.first(), directory.resolve("installation_id").readText())
        } finally {
            executor.shutdownNow()
            directory.deleteRecursively()
        }
    }
}
