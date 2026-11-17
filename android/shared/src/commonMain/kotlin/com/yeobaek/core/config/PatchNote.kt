package com.yeobaek.core.config

import com.yeobaek.core.update.UpdatePolicyPlatform
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

data class PatchNote(
    val id: String,
    val title: String,
    val changes: List<PatchNoteChange>,
)

@Serializable
data class PatchNoteChange(
    val title: String,
    val body: String,
)

@Serializable
private data class PatchNoteConfig(
    val enabled: Boolean = false,
    val id: String = "",
    val targets: Map<String, String> = emptyMap(),
    val title: String = "",
    val changes: List<PatchNoteChange> = emptyList(),
)

private val patchNoteJson = Json { ignoreUnknownKeys = true }

internal fun decodePatchNote(
    raw: String,
    platform: UpdatePolicyPlatform,
    appVersion: String,
): PatchNote? {
    val config = try {
        patchNoteJson.decodeFromString<PatchNoteConfig>(raw)
    } catch (_: SerializationException) {
        return null
    }
    if (!config.enabled || config.id.isBlank() || config.title.isBlank() || appVersion.isBlank()) return null
    val platformKey = when (platform) {
        UpdatePolicyPlatform.ANDROID -> "android"
        UpdatePolicyPlatform.IOS -> "ios"
    }
    if (config.targets[platformKey] != appVersion) return null
    if (config.changes.isEmpty() || config.changes.any { it.title.isBlank() || it.body.isBlank() }) return null

    return PatchNote(
        id = config.id.trim(),
        title = config.title.trim(),
        changes = config.changes,
    )
}
