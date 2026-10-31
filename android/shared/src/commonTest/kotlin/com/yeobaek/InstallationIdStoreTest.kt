package com.yeobaek

import com.yeobaek.data.local.InstallationIdStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class InstallationIdStoreTest {
    @Test
    fun firstAccessPersistsV4UuidAndNewStoreReusesIt() {
        var saved: String? = null
        var writes = 0
        val store = InstallationIdStore({ saved }, {
            saved = it
            writes++
        })
        val first = store.getOrCreate()

        assertNotNull(Uuid.parseOrNull(first))
        assertEquals('4', first[14])
        assertEquals(first, saved)
        assertEquals(first, store.getOrCreate())
        assertEquals(
            first,
            InstallationIdStore({ saved }, {
                saved = it
                writes++
            }).getOrCreate(),
        )
        assertEquals(1, writes)
    }

    @Test
    fun malformedOrEmptyStorageIsReplacedWithValidUuid() {
        for (initial in listOf("", "not-a-uuid")) {
            var saved = initial
            val store = InstallationIdStore({ saved }, { saved = it })
            val id = store.getOrCreate()
            assertNotNull(Uuid.parseOrNull(id))
            assertEquals(id, saved)
            assertEquals(id, store.getOrCreate())
        }
    }

    @Test
    fun independentInstallationsReceiveDifferentIds() {
        var firstSaved: String? = null
        var secondSaved: String? = null
        val first = InstallationIdStore({ firstSaved }, { firstSaved = it }).getOrCreate()
        val second = InstallationIdStore({ secondSaved }, { secondSaved = it }).getOrCreate()
        assertNotEquals(first, second)
    }

    @Test
    fun failedPersistenceDoesNotReturnAnIdAsIfItWereSaved() {
        val store = InstallationIdStore({ null }, { error("storage unavailable") })
        assertFailsWith<IllegalStateException> { store.getOrCreate() }
    }
}
