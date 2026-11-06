package com.yeobaek

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.yeobaek.core.invitation.AppInvitations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InvitationIntentTest {
    @Test
    fun implicitViewResolvesToDebugEntryPoint() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("yeobaek://invite?code=ABC123"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setPackage(context.packageName)
        val result = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        assertEquals(listOf("com.yeobaek.DebugMainActivity"), result.map { it.activityInfo.name })
    }

    @Test
    fun recreatedActivityDoesNotReplayConsumedIntentButNewClickDoes() {
        val invitations = AppInvitations()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("yeobaek://invite?code=ABC123"))
        val id = intent.deliverInvitation(invitations)
        assertNotNull(id)
        assertEquals("ABC123", invitations.pendingOpen.value?.code)
        invitations.consume(requireNotNull(invitations.pendingOpen.value))
        intent.deliverInvitation(invitations, id)
        assertNull(invitations.pendingOpen.value)
        intent.deliverInvitation(invitations)
        assertNotNull(invitations.pendingOpen.value)
    }

    @Test
    fun launcherIntentDoesNotBecomeInvitationAndMalformedLinkHasNoCode() {
        val invitations = AppInvitations()
        assertNull(Intent(Intent.ACTION_MAIN).deliverInvitation(invitations))
        val malformed = Intent(Intent.ACTION_VIEW, Uri.parse("yeobaek://invite?code=A&code=B"))
        assertNotNull(malformed.deliverInvitation(invitations))
        assertNull(invitations.pendingOpen.value?.code)
    }
}
