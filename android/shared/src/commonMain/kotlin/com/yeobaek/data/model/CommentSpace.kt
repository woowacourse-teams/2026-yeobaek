package com.yeobaek.data.model

sealed interface CommentSpace {
    data class Group(
        val groupId: Long,
    ) : CommentSpace

    data class PublicRoom(
        val publicRoomId: Long,
    ) : CommentSpace
}
