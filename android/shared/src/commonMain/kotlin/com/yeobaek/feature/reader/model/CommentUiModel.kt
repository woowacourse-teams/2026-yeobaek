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
    val isEdited: Boolean
        get() = updatedAt != null

    // 화면에 보여줄 작성 시각. 예: "2026.08.05  14:30"
    val displayCreatedAt: String
        get() = createdAt.toDisplayDateTime()
}

// 서버가 보내는 "2026-08-05T14:30:00" 형식에서 날짜와 시·분을 뽑는다. 시각이 없으면 날짜만 쓴다.
private val DATE_TIME_PATTERN = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?""")

// 형식이 예상과 다르면 받은 문자열을 그대로 보여준다.
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
