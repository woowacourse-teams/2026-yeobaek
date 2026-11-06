package com.yeobaek

import com.yeobaek.data.api.createSurveyApi
import com.yeobaek.data.repositoryImpl.remote.SurveyRepositoryImpl
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class SurveyRepositoryTest {
    @Test
    fun returnsEligibilityForCurrentMember() = runTest {
        for (required in listOf(true, false)) {
            withRepository("""{"surveyRequired":$required}""") { repository ->
                assertEquals(required, repository.isSurveyRequired())
            }
        }
    }

    @Test
    fun failedRequestDoesNotBecomeAnEligibilityResult() = runTest {
        withRepository("""{"code":"MEMBER_NOT_FOUND"}""", HttpStatusCode.BadRequest) { repository ->
            assertFailsWith<IllegalStateException> { repository.isSurveyRequired() }
        }
    }

    @Test
    fun missingResponseBodyDoesNotBecomeAnEligibilityResult() = runTest {
        withRepository("", HttpStatusCode.NoContent) { repository ->
            assertFailsWith<IllegalStateException> { repository.isSurveyRequired() }
        }
    }

    @Test
    fun missingRequiredFieldIsNotTreatedAsFalse() = runTest {
        withRepository("{}") { repository ->
            assertFailsWith<JsonConvertException> { repository.isSurveyRequired() }
        }
    }

    private suspend fun withRepository(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        block: suspend (SurveyRepositoryImpl) -> Unit,
    ) {
        val client = HttpClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/api/members/me/survey-requirement", request.url.encodedPath)
                assertEquals("7", request.headers["X-Member-Id"])
                respond(body, status, headersOf("Content-Type", "application/json"))
            },
        ) {
            defaultRequest { header("X-Member-Id", 7) }
            install(ContentNegotiation) { json() }
        }
        try {
            val api = Ktorfit.Builder().baseUrl("https://example.test/").httpClient(client)
                .converterFactories(ResponseConverterFactory()).build().createSurveyApi()
            block(SurveyRepositoryImpl(api))
        } finally {
            client.close()
        }
    }
}
