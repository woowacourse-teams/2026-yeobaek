package com.yeobaek

import com.yeobaek.core.invitation.AppInvitations
import com.yeobaek.core.invitation.InvitationRequest
import com.yeobaek.core.invitation.InviteAttribution
import com.yeobaek.core.invitation.InviteLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppInvitationsTest {
    @Test
    fun restoredRequestCanResumeWithoutAnotherOpenEvent() {
        val received = mutableListOf<Pair<String, Boolean>>()
        val original = AppInvitations { received.add(it.id to (it.code != null)) }
        original.open("yeobaek://invite?code=ABC123", "original")

        val restored = AppInvitations { received.add(it.id to (it.code != null)) }
        restored.open("yeobaek://invite?code=ABC123", "original", isRestored = true)

        assertEquals(listOf("original" to true), received)
        assertEquals("original", restored.pendingOpen.value?.id)
    }

    @Test
    fun trackedLinkRetainsAttributionAcrossRecreation() {
        val attribution = InviteAttribution(
            shareId = "79e37893-cd98-4db9-83db-ff90c3b80595",
            webVisitId = "24a97a25-77e2-4b65-8432-d73ded1d5b47",
        )
        val invitations = AppInvitations().apply { open(InviteLink.create("ABC123", attribution), "request") }

        val restored = AppInvitations().apply { restore(invitations.snapshot()) }

        assertEquals(InvitationRequest("request", "ABC123", attribution), restored.pendingOpen.value)
    }

    @Test
    fun restoresPendingRequestFromLegacySnapshot() {
        val restored = AppInvitations().apply { restore(listOf("request", "ABC123", "handled")) }

        assertEquals(InvitationRequest("request", "ABC123"), restored.pendingOpen.value)
    }

    @Test
    fun holdsRequestUntilConsumptionAndOldCompletionDoesNotClearNewRequest() {
        val invitations = AppInvitations()
        invitations.open("yeobaek://invite?code=ABC123", "first")
        val first = requireNotNull(invitations.pendingOpen.value)
        invitations.open("yeobaek://invite?code=DEF456", "second")
        invitations.consume(first)
        assertEquals(InvitationRequest("second", "DEF456"), invitations.pendingOpen.value)
    }

    @Test
    fun recreationDoesNotReplayHandledRequestButNewClickOfSameCodeIsAllowed() {
        val invitations = AppInvitations()
        invitations.open("yeobaek://invite?code=ABC123", "one")
        invitations.consume(requireNotNull(invitations.pendingOpen.value))
        val restored = AppInvitations().apply { restore(invitations.snapshot()) }
        restored.open("yeobaek://invite?code=ABC123", "one")
        assertNull(restored.pendingOpen.value)
        restored.open("yeobaek://invite?code=ABC123", "two")
        assertEquals("two", restored.pendingOpen.value?.id)
    }

    @Test
    fun pendingRegistrationRequestRestoresWithoutOverwritingNewIncomingLink() {
        val old = AppInvitations().apply { open("yeobaek://invite?code=ABC123", "old") }
        val restored = AppInvitations().apply { restore(old.snapshot()) }
        assertEquals(old.pendingOpen.value, restored.pendingOpen.value)
        val fresh = AppInvitations().apply { open("yeobaek://invite?code=DEF456", "fresh") }
        fresh.restore(old.snapshot())
        assertEquals("fresh", fresh.pendingOpen.value?.id)
    }

    @Test
    fun invalidLinkProducesNoticeWithoutCodeAndOtherIntentsStayUnrelated() {
        val invitations = AppInvitations()
        assertFalse(invitations.open("other://link"))
        assertNull(invitations.pendingOpen.value)
        assertTrue(invitations.open("yeobaek://invite?code=BAD", "invalid"))
        assertEquals(InvitationRequest("invalid", null), invitations.pendingOpen.value)
        invitations.cancelPending()
        invitations.open("yeobaek://invite?code=BAD", "invalid")
        assertNull(invitations.pendingOpen.value)
    }

    @Test
    fun finishingAnOldOwnerDoesNotUnlockAnotherSubmission() {
        val invitations = AppInvitations()
        invitations.setJoining("old", true)
        invitations.setJoining("new", true)
        invitations.setJoining("old", false)
        assertEquals(setOf("new"), invitations.joiningOwners.value)
        invitations.setJoining("new", false)
        assertTrue(invitations.joiningOwners.value.isEmpty())
    }
}
