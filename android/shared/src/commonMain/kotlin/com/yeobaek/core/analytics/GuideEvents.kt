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

enum class GuideDirection(
    val value: String,
) {
    NEXT("next"),
    PREVIOUS("previous"),
}

data class GuidePageMoved(
    val page: Int,
    val direction: GuideDirection,
) : AnalyticsEvent {
    override val name = "guide_page_moved"
    override val properties = mapOf(
        KEY_PAGE to page,
        KEY_DIRECTION to direction.value,
    )
}

data class GuideSentenceTapped(
    val hasComment: Boolean,
) : AnalyticsEvent {
    override val name = "guide_sentence_tapped"
    override val properties = mapOf(
        KEY_HAS_COMMENT to hasComment,
    )
}

private const val KEY_ENTRY_POINT = "entry_point"
private const val KEY_PAGE = "page"
private const val KEY_DIRECTION = "direction"
private const val KEY_HAS_COMMENT = "has_comment"
