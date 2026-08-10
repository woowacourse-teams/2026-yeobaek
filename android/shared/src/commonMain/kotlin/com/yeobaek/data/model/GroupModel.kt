package com.yeobaek.data.model

data class GroupModel(
    val groupCode: String = "",
    val groupName: String = "",
    val book: BookModel = BookModel(),
    val users: List<UserModel> = emptyList(),
)
