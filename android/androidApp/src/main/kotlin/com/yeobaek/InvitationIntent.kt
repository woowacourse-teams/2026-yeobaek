package com.yeobaek

import android.content.Intent
import com.yeobaek.core.invitation.AppInvitations
import java.util.UUID

internal const val INVITATION_REQUEST_ID = "yeobaek.invitation.requestId"

internal fun Intent.deliverInvitation(invitations: AppInvitations, restoredRequestId: String? = null): String? {
    if (action != Intent.ACTION_VIEW || data?.scheme != "yeobaek") return null
    val requestId = restoredRequestId ?: UUID.randomUUID().toString()
    invitations.open(dataString, requestId)
    return requestId
}
