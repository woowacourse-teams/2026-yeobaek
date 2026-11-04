package com.yeobaek.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
object IOSInstallationIdStore {
    private val lock = NSLock()

    fun getOrCreate(): String {
        val manager = NSFileManager.defaultManager
        val support = manager.URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask).first() as NSURL
        val directory = requireNotNull(support.URLByAppendingPathComponent("installation-identity", true))
        return getOrCreate(directory)
    }

    internal fun getOrCreate(directory: NSURL): String {
        lock.lock()
        try {
            val manager = NSFileManager.defaultManager
            check(manager.createDirectoryAtURL(directory, true, null, null))
            check(directory.setResourceValue(true, NSURLIsExcludedFromBackupKey, null))
            val file = requireNotNull(directory.URLByAppendingPathComponent("installation_id", false))
            val id = InstallationIdStore(
                read = {
                    NSString.create(contentsOfURL = file, encoding = NSUTF8StringEncoding, error = null)?.toString()
                },
                write = { value ->
                    check(
                        NSString.create(string = value).writeToURL(
                            file,
                            atomically = true,
                            encoding = NSUTF8StringEncoding,
                            error = null,
                        ),
                    ) { "Could not persist installation ID" }
                },
            ).getOrCreate()
            check(file.setResourceValue(true, NSURLIsExcludedFromBackupKey, null))
            return id
        } finally {
            lock.unlock()
        }
    }
}
