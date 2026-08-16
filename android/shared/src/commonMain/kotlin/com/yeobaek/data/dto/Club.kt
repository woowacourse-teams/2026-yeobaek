package com.yeobaek.data.dto

data class Club(
    val book: Book,
    val clubId: Int,
    val memberCount: Int,
    val myProgress: Any,
    val name: String
)
