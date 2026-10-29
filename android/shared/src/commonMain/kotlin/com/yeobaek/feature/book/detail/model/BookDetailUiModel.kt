package com.yeobaek.feature.book.detail.model

import com.yeobaek.data.model.BookDetailModel

data class BookDetailUiModel(
    val bookId: Long,
    val title: String,
    val authors: String,
    val coverImageUrl: String?,
    val description: String?,
    val pageCount: String,
    val passageCount: String,
    val chapters: List<BookDetailChapterUiModel>,
)

data class BookDetailChapterUiModel(
    val number: String,
    val title: String,
    val passageRange: String,
)

fun BookDetailModel.toUiModel(): BookDetailUiModel = BookDetailUiModel(
    bookId = bookId,
    title = title,
    authors = authors.joinToString(", "),
    coverImageUrl = coverImageUrl,
    description = description,
    pageCount = pageCount?.let { "${it}쪽" } ?: "정보 없음",
    passageCount = "${passageCount}문단",
    chapters = chapters.sortedBy { it.sequence }.map { chapter ->
        BookDetailChapterUiModel(
            number = chapter.sequence.toString().padStart(2, '0'),
            title = chapter.title,
            passageRange = "문단 ${chapter.startPassageSequence}–${chapter.endPassageSequence}",
        )
    },
)
