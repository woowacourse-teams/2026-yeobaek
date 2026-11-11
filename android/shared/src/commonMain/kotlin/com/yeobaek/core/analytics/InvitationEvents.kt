package com.yeobaek.core.analytics

import com.yeobaek.core.invitation.InviteAttribution

data class InviteShareSubmitted(
    val groupId: Long,
    val result: EventResult,
    val inviteShareId: String? = null,
) : AnalyticsEvent {
    override val name = "invite_share_submitted"
    override val properties = buildMap {
        put("group_id", groupId)
        put("result", result.value)
        inviteShareId?.let { put("invite_share_id", it) }
    }
}

data class InviteLinkOpened(
    val invitationRequestId: String,
    val linkValid: Boolean,
    val attribution: InviteAttribution? = null,
) : AnalyticsEvent {
    override val name = "invite_link_opened"
    override val properties = mapOf("invitation_request_id" to invitationRequestId, "link_valid" to linkValid) +
        attribution?.properties.orEmpty()
}
