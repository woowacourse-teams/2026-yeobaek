package com.yeobaek.core.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.UIKit.UIDevice

internal class IOSAppStoreVersionSource {
    private val client = HttpClient(Darwin) {
        expectSuccess = true
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestVersion(): String? {
        val response = client.get("https://itunes.apple.com/lookup") {
            parameter("id", IOSAppStore.APP_ID)
            parameter("country", "kr")
        }
        val result = json.decodeFromString<LookupResponse>(response.bodyAsText()).results.singleOrNull {
            it.bundleId == IOSAppStore.BUNDLE_ID && it.trackId.toString() == IOSAppStore.APP_ID
        } ?: return null
        if (compareStoreVersions(UIDevice.currentDevice.systemVersion, result.minimumOsVersion)?.let { it >= 0 } !=
            true
        ) {
            return null
        }
        return result.version.takeIf { compareStoreVersions(it, "0")?.let { comparison -> comparison > 0 } == true }
    }

    fun close() {
        client.close()
    }

    @Serializable
    private data class LookupResponse(val results: List<StoreResult> = emptyList())

    @Serializable
    private data class StoreResult(
        val bundleId: String,
        val trackId: Long,
        val version: String,
        val minimumOsVersion: String,
    )
}
