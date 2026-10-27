package com.yeobaek.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationRegistrationRequest(
    val fid: String,
)
