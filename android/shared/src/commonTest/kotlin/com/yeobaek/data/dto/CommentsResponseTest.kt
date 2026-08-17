package com.yeobaek.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class CommentsResponseTest {

    @Test
    fun commentsResponseIsDecodedAndMappedToModel() {
        val response = Json.decodeFromString<CommentsResponse>(
            """
            {
              "comments": [
                {
                  "commentId": 7,
                  "memberId": 3,
                  "nickname": "독자",
                  "content": "좋은 문장이에요.",
                  "createdAt": "2026-08-17T08:23:42.928Z",
                  "updatedAt": "2026-08-17T08:23:42.928Z",
                  "mine": true
                }
              ]
            }
            """.trimIndent(),
        )

        val comment = response.toModel().comments.single()

        assertEquals(7L, comment.commentId)
        assertEquals(3L, comment.memberId)
        assertEquals("독자", comment.nickname)
        assertEquals("좋은 문장이에요.", comment.content)
        assertEquals("2026-08-17T08:23:42.928Z", comment.createdAt)
        assertEquals("2026-08-17T08:23:42.928Z", comment.updatedAt)
        assertTrue(comment.mine)
    }
}
