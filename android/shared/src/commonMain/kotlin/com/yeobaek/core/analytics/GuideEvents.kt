package com.yeobaek.core.analytics

enum class GuideEntryPoint(
    val value: String,
) {
    ONBOARDING("onboarding"),
    MY_PAGE("my_page"),
}

data class GuideStarted(
    val entryPoint: GuideEntryPoint,
) : AnalyticsEvent {
    override val name = "guide_started"
    override val properties = mapOf(
        KEY_ENTRY_POINT to entryPoint.value,
    )
}

data class GuideCompleted(
    val entryPoint: GuideEntryPoint,
) : AnalyticsEvent {
    override val name = "guide_completed"
    override val properties = mapOf(
        KEY_ENTRY_POINT to entryPoint.value,
    )
}

data class GuideDismissed(
    val entryPoint: GuideEntryPoint,
    val page: Int,
) : AnalyticsEvent {
    override val name = "guide_dismissed"
    override val properties = mapOf(
        KEY_ENTRY_POINT to entryPoint.value,
        KEY_PAGE to page,
    )
}

private const val KEY_ENTRY_POINT = "entry_point"
private const val KEY_PAGE = "page"
