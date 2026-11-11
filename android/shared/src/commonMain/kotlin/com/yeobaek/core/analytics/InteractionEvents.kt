package com.yeobaek.core.analytics

enum class KeyboardField(val value: String) {
    PUBLIC_ROOM_SEARCH("public_room_search"),
    GROUP_CREATE_BOOK_SEARCH("group_create_book_search"),
    GROUP_NAME("group_name"),
}

enum class KeyboardDismissTrigger(val value: String) {
    OUTSIDE_TAP("outside_tap"),
    IME_SEARCH("ime_search"),
    IME_DONE("ime_done"),
}

data class InputKeyboardDismissed(val field: KeyboardField, val trigger: KeyboardDismissTrigger) : AnalyticsEvent {
    override val name = "input_keyboard_dismissed"
    override val properties = mapOf("field" to field.value, "trigger" to trigger.value)
}

data object NavigationBackIgnored : AnalyticsEvent {
    override val name = "navigation_back_ignored"
    override val properties = mapOf("reason" to "stale_entry")
}
