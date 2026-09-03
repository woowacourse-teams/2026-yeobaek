package com.yeobaek.data.dto

import com.yeobaek.data.model.MemberModel
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val memberId: Int,
    val mine: Boolean,
    val nickname: String,
    val blocked: Boolean,
)

fun Member.toModel(): MemberModel =
    MemberModel(
        id = memberId,
        mine = mine,
        nickname = nickname,
        blocked = blocked,
    )
