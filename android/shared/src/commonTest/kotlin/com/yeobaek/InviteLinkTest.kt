package com.yeobaek

import com.yeobaek.core.invitation.InviteLink
import com.yeobaek.core.invitation.InviteLinkResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InviteLinkTest {
    @Test
    fun generatedLinksRoundTripWithoutChangingCodes() {
        listOf("WYI97U", "ABC123", "000000").forEach { code ->
            assertEquals("yeobaek://invite?code=$code", InviteLink.create(code))
            assertEquals(InviteLinkResult.Valid(code), InviteLink.parse(InviteLink.create(code)))
        }
    }

    @Test
    fun rejectsMissingDuplicateAndUnexpectedUriParts() {
        listOf(
            "yeobaek://invite",
            "yeobaek://invite?code=",
            "yeobaek://invite?code=ABC123&code=DEF456",
            "yeobaek://invite?code=ABC123&memberId=1",
            "yeobaek://invite?code=ABC123#fragment",
            "yeobaek://invite/path?code=ABC123",
            "yeobaek:///invite?code=ABC123",
            "yeobaek://invite.evil?code=ABC123",
            "yeobaek://user@invite?code=ABC123",
            "yeobaek://invite:80?code=ABC123",
            "yeobaek://invite?code=%41BC123",
        ).forEach { assertEquals(InviteLinkResult.Invalid, InviteLink.parse(it), it) }
    }

    @Test
    fun rejectsInvalidCodesInsteadOfTruncatingOrNormalizingThem() {
        listOf("abc123", "ABCDE", "ABCDEFG", "ABC 12", "가나다123", "ABC12\n").forEach { code ->
            assertEquals(InviteLinkResult.Invalid, InviteLink.parse("yeobaek://invite?code=$code"))
            assertFailsWith<IllegalArgumentException> { InviteLink.create(code) }
        }
    }

    @Test
    fun unrelatedEntriesAreNotInvitationErrors() {
        listOf(null, "https://example.com", "other://invite?code=ABC123").forEach {
            assertEquals(InviteLinkResult.Unrelated, InviteLink.parse(it))
        }
    }

    @Test
    fun shareMessageContainsBothManualAndLinkEntry() {
        val text = InviteLink.shareText("데미안", "WYI97U")
        assertTrue(text.contains("데미안 모임"))
        assertTrue(text.contains("초대코드: WYI97U"))
        assertTrue(text.contains(InviteLink.create("WYI97U")))
        assertTrue(text.contains("여백 앱에서 초대코드를 입력"))
    }
}
