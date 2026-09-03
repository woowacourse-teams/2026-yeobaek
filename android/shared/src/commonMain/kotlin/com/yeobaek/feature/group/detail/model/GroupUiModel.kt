package com.yeobaek.feature.group.detail.model

data class GroupUiModel(
    val id: Long = 0,
    val name: String = "",
    val groupCode: String = "XXXXXX",
    val users: List<UserUiModel> = emptyList(),
)
