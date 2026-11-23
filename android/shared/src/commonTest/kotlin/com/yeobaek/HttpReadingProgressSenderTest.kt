package com.yeobaek

import com.yeobaek.core.network.HttpReadingProgressSender
import com.yeobaek.core.network.bindReadingProgressMember
import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressKey
import com.yeobaek.core.readingprogress.ReadingProgressSyncException
import com.yeobaek.core.readingprogress.ReadingSpace
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class HttpReadingProgressSenderTest {
    @Test
    fun sendsOnlyBoundAccountHeaderAndUsesCorrectResource() = runTest {
        val paths = mutableListOf<String>()
        val client = client(
            MockEngine { request ->
                assertEquals(listOf("7"), request.headers.getAll("X-Member-Id"))
                paths += request.url.encodedPath
                respond(PROGRESS_RESPONSE, headers = JSON_HEADERS)
            },
        ) { 7 }
        try {
            val sender = HttpReadingProgressSender(client, "https://example.test/")
            assertEquals(34, sender.save(progress()))
            sender.save(progress(ReadingSpace.PUBLIC_ROOM))
            assertEquals(listOf("/api/clubs/2/progress", "/api/public-rooms/2/progress"), paths)
        } finally {
            client.close()
        }
    }

    @Test
    fun changedAccountNeverReachesTransport() = runTest {
        var requests = 0
        val client = client(
            MockEngine {
                requests++
                respond(PROGRESS_RESPONSE, headers = JSON_HEADERS)
            },
        ) { 8 }
        try {
            assertFailsWith<CancellationException> {
                HttpReadingProgressSender(client, "https://example.test").save(progress())
            }
            assertEquals(0, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun onlyTransientHttpFailuresAreRetryable() = runTest {
        for (status in listOf(400, 401, 403, 404, 408, 429, 500, 503)) {
            val client = client(MockEngine { respond("", HttpStatusCode.fromValue(status)) }) { 7 }
            try {
                val failure = assertFailsWith<ReadingProgressSyncException> {
                    HttpReadingProgressSender(client, "https://example.test").save(progress())
                }
                assertEquals(status in listOf(408, 429, 500, 503), failure.retryable)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun transportFailureIsRetryableButMalformedSuccessIsNotAcknowledged() = runTest {
        val offline = client(MockEngine { throw IOException("offline") }) { 7 }
        try {
            assertTrue(
                assertFailsWith<ReadingProgressSyncException> {
                    HttpReadingProgressSender(offline, "https://example.test").save(progress())
                }.retryable,
            )
        } finally {
            offline.close()
        }
        val malformed = client(MockEngine { respond("{}", headers = JSON_HEADERS) }) { 7 }
        try {
            val failure = assertFailsWith<Exception> {
                HttpReadingProgressSender(malformed, "https://example.test").save(progress())
            }
            assertFalse(failure is ReadingProgressSyncException && failure.retryable)
        } finally {
            malformed.close()
        }
    }

    private fun client(engine: MockEngine, memberId: () -> Int?): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json() }
        defaultRequest { header("X-Member-Id", "stale-default") }
    }.apply {
        plugin(HttpSend).intercept { request ->
            request.bindReadingProgressMember(memberId())
            execute(request)
        }
    }

    private fun progress(space: ReadingSpace = ReadingSpace.GROUP) = ReadingProgress(
        ReadingProgressKey(7, space, 2, 11),
        100,
        34,
        2,
        1,
    )

    companion object {
        private val PROGRESS_RESPONSE = """
            {"lastReadAt":"2026-11-23","lastReadPassageSequence":34,"progressRate":2}
        """.trimIndent()
        private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
