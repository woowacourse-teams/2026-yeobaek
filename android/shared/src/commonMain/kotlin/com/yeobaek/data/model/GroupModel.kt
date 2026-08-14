package com.yeobaek.data.model

data class GroupModel(
    val groupId: Int = 0,
    val groupCode: String = "",
    val groupName: String = "",
    val book: BookModel = BookModel(),
    val users: List<UserModel> = emptyList(),
)
