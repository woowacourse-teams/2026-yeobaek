package com.yeobaek.core.analytics

enum class EventResult(
    val value: String,
) {
    SUCCESS("success"),
    FAILURE("failure"),
    INVALID("invalid"),
}

enum class InvalidReason(
    val value: String,
) {
    NICKNAME_BLANK("nickname_blank"),
    NAME_BLANK("name_blank"),
    BOOK_NOT_SELECTED("book_not_selected"),
    CODE_BLANK("code_blank"),
    CODE_INVALID_FORMAT("code_invalid_format"),
    NICKNAME_TOO_LONG("nickname_too_long"),
}
