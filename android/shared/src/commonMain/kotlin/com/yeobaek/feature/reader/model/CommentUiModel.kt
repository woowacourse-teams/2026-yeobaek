package com.yeobaek.feature.reader.model

import com.yeobaek.data.model.CommentModel

data class CommentUiModel(
    val commentId: Long,
    val memberId: Int,
    val nickname: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val isMine: Boolean,
) {
    val displayCreatedAt: String
        get() = createdAt.toDisplayDateTime()
}

private val DATE_TIME_PATTERN = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?""")

private fun String.toDisplayDateTime(): String {
    val match = DATE_TIME_PATTERN.find(this) ?: return this
    val (year, month, day, hour, minute) = match.destructured
    val date = "$year.$month.$day"
    return if (hour.isEmpty()) date else "$date  $hour:$minute"
}

fun CommentModel.toUiModel(): CommentUiModel =
    CommentUiModel(
        commentId = commentId,
        memberId = memberId,
        nickname = nickname,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isMine = mine,
    )
