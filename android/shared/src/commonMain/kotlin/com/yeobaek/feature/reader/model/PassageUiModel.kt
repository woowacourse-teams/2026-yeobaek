package com.yeobaek.feature.reader.model

data class PassageUiModel(
    val passageId: Int,
    val sequence: Int,
    val chapterId: Int,
    val content: String,
    val commentCount: Int,
) {
    val hasComment: Boolean
        get() = commentCount > 0
}
