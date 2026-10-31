package com.yeobaek.data.local

import kotlin.uuid.Uuid

class InstallationIdStore(
    private val read: () -> String?,
    private val write: (String) -> Unit,
) {
    fun getOrCreate(): String {
        val stored = read()
        if (stored != null && Uuid.parseOrNull(stored) != null) return stored

        return Uuid.random().toString().also(write)
    }
}
