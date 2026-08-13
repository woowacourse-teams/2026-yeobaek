package com.yeobaek.data.dto

import com.yeobaek.data.model.UserModel
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val memberId: Int,
    val mine: Boolean,
    val nickname: String,
)

fun Member.toModel(): MemberModel =
    MemberModel(
        memberId = memberId,
        mine = mine,
        nickname = nickname,
    )
