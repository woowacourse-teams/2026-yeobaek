package com.yeobaek.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClubRequest(
    val bookId: Long,
    val name: String,
)
