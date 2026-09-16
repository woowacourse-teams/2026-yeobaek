package com.yeobaek.core.analytics

data class GroupDetailOpened(
    val groupId: Long,
) : AnalyticsEvent {
    override val name = "group_detail_opened"
    override val properties = mapOf(
        KEY_GROUP_ID to groupId,
    )
}

data object GroupCreateInitiated : AnalyticsEvent {
    override val name = "group_create_initiated"
    override val properties = emptyMap<String, Any>()
}

data object GroupJoinInitiated : AnalyticsEvent {
    override val name = "group_join_initiated"
    override val properties = emptyMap<String, Any>()
}

private const val KEY_GROUP_ID = "group_id"
