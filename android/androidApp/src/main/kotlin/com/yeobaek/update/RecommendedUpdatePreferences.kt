package com.yeobaek.update

import android.content.SharedPreferences

class RecommendedUpdatePreferences(private val preferences: SharedPreferences) {
    fun availableVersionCode(): Int? = preferences.getInt(AVAILABLE_VERSION, 0).takeIf { it > 0 }

    fun recordAvailableVersion(versionCode: Int) {
        preferences.edit().putInt(AVAILABLE_VERSION, versionCode).apply()
    }

    fun wasPrompted(versionCode: Int): Boolean =
        preferences.getStringSet(PROMPTED_VERSIONS, emptySet()).orEmpty().contains(versionCode.toString())

    fun markPrompted(versionCode: Int) {
        val versions = preferences.getStringSet(PROMPTED_VERSIONS, emptySet()).orEmpty().toMutableSet()
        versions.add(versionCode.toString())
        preferences.edit().putStringSet(PROMPTED_VERSIONS, versions).apply()
    }

    private companion object {
        const val AVAILABLE_VERSION = "availableVersionCode"
        const val PROMPTED_VERSIONS = "promptedVersionCodes"
    }
}
