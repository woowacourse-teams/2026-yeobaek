package com.yeobaek.core.network

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NetworkProvider(
    private val memberId: Int,
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
        defaultRequest {
            header(
                "X-Member-Id",
                memberId.toString(),
            )
        }
    }

    private fun createKtorfit(
        httpClient: HttpClient,
    ): Ktorfit =
        Ktorfit.Builder()
            .baseUrl(BASE_URL)
            .httpClient(httpClient)
            .build()

    companion object {
        const val BASE_URL = "https://yeobaek.duckdns.org/"
    }
}
