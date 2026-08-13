package com.yeobaek.data.dto

import com.yeobaek.data.model.UserModel

data class UserResponse(
    val memberId: Int,
    val nickname: String
)

fun UserResponse.toModel(): UserModel {
    return UserModel(
        id = memberId,
        name = nickname,
    )
}
