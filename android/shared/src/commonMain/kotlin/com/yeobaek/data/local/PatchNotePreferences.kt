package com.yeobaek.data.local

import com.russhwolf.settings.Settings
import com.yeobaek.core.config.PatchNote
import com.yeobaek.core.config.decodePatchNote
import com.yeobaek.core.update.UpdatePolicyPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class PatchNotePreferences(private val settings: Settings) {
    private val mutableAcknowledgedIds = MutableStateFlow(readAcknowledgedIds())
    val acknowledgedIds = mutableAcknowledgedIds.asStateFlow()

    fun readFor(platform: UpdatePolicyPlatform, appVersion: String): PatchNote? =
        settings.getStringOrNull(NEXT_PATCH_NOTE)?.let { decodePatchNote(it, platform, appVersion) }

    fun saveForNextLaunch(raw: String) {
        settings.putString(NEXT_PATCH_NOTE, raw)
    }

    fun acknowledge(id: String) {
        val ids = mutableAcknowledgedIds.value
        if (id in ids) return
        val updated = ids + id
        settings.putString(ACKNOWLEDGED_PATCH_NOTES, Json.encodeToString(updated))
        mutableAcknowledgedIds.value = updated
    }

    private fun readAcknowledgedIds(): Set<String> {
        val raw = settings.getStringOrNull(ACKNOWLEDGED_PATCH_NOTES) ?: return emptySet()
        return try {
            Json.decodeFromString<Set<String>>(raw)
        } catch (_: SerializationException) {
            emptySet()
        }
    }

    private companion object {
        const val NEXT_PATCH_NOTE = "nextPatchNote"
        const val ACKNOWLEDGED_PATCH_NOTES = "acknowledgedPatchNotes"
    }
}
