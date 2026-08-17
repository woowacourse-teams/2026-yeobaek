package com.yeobaek.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val nickname: String,
)
