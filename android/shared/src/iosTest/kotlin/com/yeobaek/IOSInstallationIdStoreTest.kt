package com.yeobaek

import com.yeobaek.data.local.IOSInstallationIdStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSInstallationIdStoreTest {
    @Test
    fun persistsOneIdentityAndMarksDirectoryAndFileExcludedFromBackup() {
        withDirectory { directory ->
            val first = IOSInstallationIdStore.getOrCreate(directory)
            assertEquals(first, IOSInstallationIdStore.getOrCreate(directory))
            val file = requireNotNull(directory.URLByAppendingPathComponent("installation_id", false))
            assertEquals(
                first,
                NSString.create(contentsOfURL = file, encoding = NSUTF8StringEncoding, error = null)?.toString(),
            )
            assertExcludedFromBackup(directory)
            assertExcludedFromBackup(file)
        }
    }

    @Test
    fun replacesCorruptFileAndRestoresBackupExclusionAfterAtomicWrite() {
        withDirectory { directory ->
            check(NSFileManager.defaultManager.createDirectoryAtURL(directory, true, null, null))
            val file = requireNotNull(directory.URLByAppendingPathComponent("installation_id", false))
            check(NSString.create(string = "invalid").writeToURL(file, true, NSUTF8StringEncoding, null))
            val id = IOSInstallationIdStore.getOrCreate(directory)
            assertEquals('4', id[14])
            assertEquals(id, IOSInstallationIdStore.getOrCreate(directory))
            assertExcludedFromBackup(file)
        }
    }

    private fun assertExcludedFromBackup(url: NSURL) {
        val values = requireNotNull(url.resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), null))
        assertTrue((values[NSURLIsExcludedFromBackupKey] as NSNumber).boolValue)
    }

    private fun withDirectory(block: (NSURL) -> Unit) {
        val temporary = NSURL.fileURLWithPath(NSTemporaryDirectory())
        val directory =
            requireNotNull(temporary.URLByAppendingPathComponent("installation-id-test-${Uuid.random()}", true))
        try {
            block(directory)
        } finally {
            NSFileManager.defaultManager.removeItemAtURL(directory, null)
        }
    }
}
