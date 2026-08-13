package com.yeobaek.data.dto

import com.yeobaek.data.model.GroupModel

data class Club(
    val book: Book,
    val clubId: Int,
    val memberCount: Int,
    val myProgress: Any,
    val name: String
)

fun Club.toModel(): GroupModel {
    return GroupModel(
        book = book,
        clubId = clubId,
        memberCount = memberCount,
        myProgress = myProgress,
        name = name,
    )
}
