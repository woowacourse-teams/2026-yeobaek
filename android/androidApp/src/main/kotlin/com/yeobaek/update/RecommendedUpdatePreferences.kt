package com.yeobaek.update

import android.content.SharedPreferences

class RecommendedUpdatePreferences(private val preferences: SharedPreferences) {
    fun wasPrompted(versionCode: Int): Boolean =
        preferences.getStringSet(PROMPTED_VERSIONS, emptySet()).orEmpty().contains(versionCode.toString())

    fun markPrompted(versionCode: Int) {
        val versions = preferences.getStringSet(PROMPTED_VERSIONS, emptySet()).orEmpty().toMutableSet()
        versions.add(versionCode.toString())
        preferences.edit().putStringSet(PROMPTED_VERSIONS, versions).apply()
    }

    private companion object {
        const val PROMPTED_VERSIONS = "promptedVersionCodes"
    }
}
