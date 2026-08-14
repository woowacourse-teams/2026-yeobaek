package com.yeobaek.core.network

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NetworkProvider {
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
            level = LogLevel.BODY
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
