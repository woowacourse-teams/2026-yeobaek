package com.yeobaek.core.common

enum class TrackedScreen(
    val value: String,
) {
    NICKNAME("nickname"),
    GUIDE("guide"),
    ONBOARDING("onboarding"),
    ONBOARDING_CREATE("onboarding_create"),
    HOME("home"),
    GROUP_DETAIL("group_detail"),
    GROUP_JOIN("group_join"),
    GROUP_CREATE("group_create"),
    READER("reader"),
    PUBLIC_ROOM_READER("public_room_reader"),
    COMMENT_COLLECTION("comment_collection"),
    MY_PAGE("my_page"),
}
