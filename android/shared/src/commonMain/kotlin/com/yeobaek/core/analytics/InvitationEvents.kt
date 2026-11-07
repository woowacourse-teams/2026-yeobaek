package com.yeobaek.core.analytics

data class InviteShareSubmitted(
    val groupId: Long,
    val result: EventResult,
) : AnalyticsEvent {
    override val name = "invite_share_submitted"
    override val properties = mapOf("group_id" to groupId, "result" to result.value)
}

data class InviteLinkOpened(
    val invitationRequestId: String,
    val linkValid: Boolean,
) : AnalyticsEvent {
    override val name = "invite_link_opened"
    override val properties = mapOf("invitation_request_id" to invitationRequestId, "link_valid" to linkValid)
}
