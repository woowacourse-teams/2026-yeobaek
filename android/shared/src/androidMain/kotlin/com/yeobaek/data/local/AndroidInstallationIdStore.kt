package com.yeobaek.data.local

import java.io.File

class AndroidInstallationIdStore(
    private val noBackupDirectory: File,
) {
    fun getOrCreate(): String = synchronized(lock) {
        check(noBackupDirectory.isDirectory || noBackupDirectory.mkdirs())
        val file = File(noBackupDirectory, "installation_id")
        InstallationIdStore(
            read = { file.takeIf { it.exists() }?.readText() },
            write = { id ->
                val temporary = File(noBackupDirectory, "installation_id.tmp")
                try {
                    temporary.writeText(id)
                    check(temporary.renameTo(file)) { "Could not persist installation ID" }
                } finally {
                    temporary.delete()
                }
            },
        ).getOrCreate()
    }

    private companion object {
        val lock = Any()
    }
}
