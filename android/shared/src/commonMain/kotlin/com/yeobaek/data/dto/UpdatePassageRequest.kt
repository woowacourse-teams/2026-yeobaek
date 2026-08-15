package com.yeobaek.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePassageRequest(
    val passageId: Int,
)
