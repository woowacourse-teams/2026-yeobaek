package com.yeobaek.feature.home.model

data class GroupUiModel(
    val groupId: Long = 0,
    val uri: String?,
    val title: String = "",
    val groupName: String = "",
    val groupCount: Int = 0,
)
