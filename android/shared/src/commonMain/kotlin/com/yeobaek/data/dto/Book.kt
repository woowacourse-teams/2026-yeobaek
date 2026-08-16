package com.yeobaek.data.dto

data class Book(
    val authors: List<String>,
    val bookId: Int,
    val passageCount: Int,
    val title: String
)
