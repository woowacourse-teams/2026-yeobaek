package com.yeobaek

import com.yeobaek.data.api.createNotificationApi
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import com.yeobaek.data.repository.NotificationPreferencesException
import com.yeobaek.data.repositoryImpl.remote.NotificationRepositoryImpl
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NotificationRepositoryTest {
    @Test
    fun loadsPreferencesForCurrentMemberAndInstallation() = runTest {
        withRepository({ request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/notifications/preferences", request.url.encodedPath)
            assertEquals("fid-10", request.url.parameters["fid"])
            assertEquals("7", request.headers["X-Member-Id"])
            respondPreferences()
        }) { repository ->
            assertEquals(NotificationPreferences(true, true, false, true), repository.getPreferences(7, "fid-10"))
        }
    }

    @Test
    fun patchChangesOnlySelectedFieldAndAcceptsEmpty204() = runTest {
        for ((setting, field) in listOf(
            NotificationSetting.ALL to "enabled",
            NotificationSetting.CLUB_JOIN to "clubJoin",
            NotificationSetting.CLUB_COMMENT to "clubComment",
            NotificationSetting.PUBLIC_ROOM_COMMENT to "publicRoomComment",
        )) {
            withRepository({ request ->
                assertEquals(HttpMethod.Patch, request.method)
                assertEquals("fid-10", request.url.parameters["fid"])
                assertEquals("7", request.headers["X-Member-Id"])
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertTrue(body.filterKeys { it != field }.values.all { it == JsonNull })
                assertEquals("false", body.getValue(field).jsonPrimitive.content)
                respond("", HttpStatusCode.NoContent)
            }) { repository ->
                repository.updatePreferences(7, "fid-10", setting, false)
            }
        }
    }

    @Test
    fun missingRegistrationIsRegisteredAndRetriedOnce() = runTest {
        var count = 0
        withRepository({ request ->
            when (++count) {
                1 -> respondError("NOTIFICATION_REGISTRATION_NOT_FOUND")

                2 -> {
                    assertEquals("/api/notifications/registrations", request.url.encodedPath)
                    assertEquals(HttpMethod.Post, request.method)
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals("fid-10", body.getValue("fid").jsonPrimitive.content)
                    respond("", HttpStatusCode.OK)
                }

                else -> respondPreferences()
            }
        }) { repository ->
            repository.getPreferences(7, "fid-10")
            assertEquals(3, count)
        }
    }

    @Test
    fun recoveryDoesNotLoopWhenRegistrationIsStillMissing() = runTest {
        var count = 0
        withRepository({ request ->
            count++
            if (request.method == HttpMethod.Post) {
                respond("", HttpStatusCode.OK)
            } else {
                respondError("NOTIFICATION_REGISTRATION_NOT_FOUND")
            }
        }) { repository ->
            assertFailsWith<NotificationPreferencesException> { repository.getPreferences(7, "fid-10") }
            assertEquals(3, count)
        }
    }

    @Test
    fun unrelatedBadRequestDoesNotRegisterDevice() = runTest {
        var count = 0
        withRepository({
            count++
            respondError("INVALID_FID")
        }) { repository ->
            val error = assertFailsWith<NotificationPreferencesException> {
                repository.updatePreferences(7, "fid-10", NotificationSetting.ALL, false)
            }
            assertEquals("INVALID_FID", error.code)
            assertEquals(400, error.statusCode)
            assertEquals(1, count)
        }
    }

    @Test
    fun globalTogglePreservesTypeSelections() {
        val original = NotificationPreferences(true, true, false, true)
        val disabled = original.updated(NotificationSetting.ALL, false)
        assertFalse(disabled.enabled)
        assertTrue(disabled.clubJoin)
        assertFalse(disabled.clubComment)
        assertTrue(disabled.publicRoomComment)
        assertEquals(original, disabled.updated(NotificationSetting.ALL, true))
    }

    private suspend fun withRepository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        block: suspend (NotificationRepositoryImpl) -> Unit,
    ) {
        val client = HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) { json() }
        }
        try {
            val api = Ktorfit.Builder().baseUrl("https://example.test/").httpClient(client)
                .converterFactories(ResponseConverterFactory()).build().createNotificationApi()
            block(NotificationRepositoryImpl(api))
        } finally {
            client.close()
        }
    }

    private fun MockRequestHandleScope.respondPreferences(): HttpResponseData = respond(
        """{"enabled":true,"clubJoin":true,"clubComment":false,"publicRoomComment":true}""",
        HttpStatusCode.OK,
        headersOf("Content-Type", "application/json"),
    )

    private fun MockRequestHandleScope.respondError(code: String): HttpResponseData = respond(
        """{"code":"$code","message":"request failed"}""",
        HttpStatusCode.BadRequest,
        headersOf("Content-Type", "application/json"),
    )
}
