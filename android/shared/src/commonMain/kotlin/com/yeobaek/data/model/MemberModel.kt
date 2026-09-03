package com.yeobaek.data.model

data class MemberModel(
    val id: Int,
    val mine: Boolean,
    val nickname: String,
    val blocked: Boolean,
)
