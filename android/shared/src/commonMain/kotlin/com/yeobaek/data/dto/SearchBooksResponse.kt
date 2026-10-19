package com.yeobaek.data.dto

import com.yeobaek.data.model.SearchBooksModel

data class SearchBooksResponse(
    val searchBooks: List<SearchBook>
)

fun SearchBooksResponse.toModel(): SearchBooksModel = SearchBooksModel(
    searchBooks = searchBooks.map(SearchBook::toModel)
)
