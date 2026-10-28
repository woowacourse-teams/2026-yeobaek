package com.yeobaek.core.update

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

class AppStoreUpdateSession(private val settings: Settings) {
    private val availableAtLaunch = settings.getStringOrNull(AVAILABLE_VERSION_KEY)
    var hasCheckedStore: Boolean = false
        private set

    fun recordAvailableVersion(version: String?) {
        hasCheckedStore = true
        if (version == null) {
            settings.remove(AVAILABLE_VERSION_KEY)
        } else {
            settings.putString(AVAILABLE_VERSION_KEY, version)
        }
    }

    fun canPrompt(version: String, currentVersion: String): Boolean = version == availableAtLaunch &&
        compareStoreVersions(version, currentVersion)?.let { it > 0 } == true &&
        version !in promptedVersions()

    fun markPrompted(version: String) {
        settings.putString(PROMPTED_VERSIONS_KEY, Json.encodeToString(promptedVersions() + version))
    }

    private fun promptedVersions(): Set<String> = try {
        settings.getStringOrNull(PROMPTED_VERSIONS_KEY)?.let { Json.decodeFromString<Set<String>>(it) } ?: emptySet()
    } catch (_: IllegalArgumentException) {
        emptySet()
    }

    private companion object {
        const val AVAILABLE_VERSION_KEY = "iosRecommendedUpdateAvailableVersion"
        const val PROMPTED_VERSIONS_KEY = "iosRecommendedUpdatePromptedVersions"
    }
}

internal fun compareStoreVersions(first: String, second: String): Int? {
    fun parts(value: String): List<Int>? {
        if (!value.matches(Regex("[0-9]+(\\.[0-9]+)*"))) return null
        return value.split('.').map { it.toIntOrNull() ?: return null }
    }
    val firstParts = parts(first) ?: return null
    val secondParts = parts(second) ?: return null
    for (index in 0 until maxOf(firstParts.size, secondParts.size)) {
        val comparison = firstParts.getOrElse(index) { 0 }.compareTo(secondParts.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}
