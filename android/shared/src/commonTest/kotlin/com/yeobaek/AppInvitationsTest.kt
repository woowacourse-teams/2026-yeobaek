package com.yeobaek

import com.yeobaek.core.invitation.AppInvitations
import com.yeobaek.core.invitation.InvitationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppInvitationsTest {
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
