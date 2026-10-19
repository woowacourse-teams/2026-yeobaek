package com.yeobaek.data.dto

import com.yeobaek.data.model.SearchBookModel

data class SearchBook(
    val authors: List<String>,
    val bookId: Int,
    val coverImageUrl: String,
    val passageCount: Int,
    val publishedYear: Int,
    val publisher: String,
    val title: String
)

fun SearchBook.toModel(): SearchBookModel = SearchBookModel(
    authors = authors,
    bookId = bookId.toLong(),
    coverImageUrl = coverImageUrl,
    passageCount = passageCount,
    publishedYear = publishedYear,
    publisher = publisher,
    title = title
)
