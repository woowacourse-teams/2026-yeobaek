package com.yeobaek.core.analytics

sealed interface AnalyticsEvent {
    val name: String
    val properties: Map<String, String>

    data object UserCreated : AnalyticsEvent {
        override val name = "user_created"
        override val properties = emptyMap<String, String>()
    }
}
