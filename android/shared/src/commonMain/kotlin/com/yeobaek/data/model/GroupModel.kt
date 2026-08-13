package com.yeobaek.data.model

import com.yeobaek.data.dto.Book

data class GroupModel(
    val book: Book,
    val clubId: Int,
    val memberCount: Int,
    val myProgress: MyProgress?,
    val name: String
)
