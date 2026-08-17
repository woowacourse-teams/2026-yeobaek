package com.yeobaek.core.network

import com.yeobaek.data.local.UserPreferences
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NetworkProvider(
    private val userPreferences: UserPreferences,
    private val isDebug: Boolean,
) {
    private val httpClient: HttpClient =
        createHttpClient()

    val ktorfit: Ktorfit =
        createKtorfit(
            httpClient = httpClient,
        )

    fun close() {
        httpClient.close()
    }

    private fun createHttpClient(): HttpClient = HttpClient {
        defaultRequest {
            userPreferences.getUserId()?.let { userId ->
                header(
                    key = "X-Member-Id",
                    value = userId
                )
            }
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = if (isDebug) LogLevel.BODY else LogLevel.NONE
            sanitizeHeader { header ->
                header.equals(
                    "X-Member-Id",
                    ignoreCase = true,
                )
            }
        }
    }

    private fun createKtorfit(
        httpClient: HttpClient,
    ): Ktorfit =
        Ktorfit.Builder()
            .baseUrl(BASE_URL)
            .httpClient(httpClient)
            .converterFactories(
                ResponseConverterFactory(),
            )
            .build()

    companion object {
        const val BASE_URL = "https://yeobaek.duckdns.org/"
    }
}
