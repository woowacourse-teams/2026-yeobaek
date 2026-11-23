package com.yeobaek.core.network

import com.yeobaek.core.readingprogress.ReadingProgress
import com.yeobaek.core.readingprogress.ReadingProgressSender
import com.yeobaek.core.readingprogress.ReadingProgressSyncException
import com.yeobaek.core.readingprogress.ReadingSpace
import com.yeobaek.data.dto.MyProgress
import com.yeobaek.data.dto.UpdatePassageRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

internal val ReadingProgressMemberId = AttributeKey<Int>("ReadingProgressMemberId")

internal fun HttpRequestBuilder.bindReadingProgressMember(currentMemberId: Int?) {
    val member = attributes.getOrNull(ReadingProgressMemberId) ?: return
    if (currentMemberId != member) throw CancellationException("Reading progress account changed before send")
    headers.remove("X-Member-Id")
    headers.append("X-Member-Id", member.toString())
}

internal class HttpReadingProgressSender(
    private val client: HttpClient,
    private val baseUrl: String,
) : ReadingProgressSender {
    override suspend fun save(progress: ReadingProgress): Int {
        val resource = when (progress.key.space) {
            ReadingSpace.GROUP -> "clubs"
            ReadingSpace.PUBLIC_ROOM -> "public-rooms"
        }
        try {
            val response = client.put("${baseUrl.trimEnd('/')}/api/$resource/${progress.key.spaceId}/progress") {
                attributes.put(ReadingProgressMemberId, progress.key.memberId)
                contentType(ContentType.Application.Json)
                setBody(UpdatePassageRequest(progress.passageId))
            }
            if (!response.status.isSuccess()) {
                val status = response.status.value
                throw ReadingProgressSyncException(
                    retryable = status == 408 || status == 429 || status >= 500,
                    message = "Reading progress HTTP $status",
                )
            }
            return response.body<MyProgress>().lastReadPassageSequence
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            throw ReadingProgressSyncException(true, "Reading progress transport failed", exception)
        }
    }
}
