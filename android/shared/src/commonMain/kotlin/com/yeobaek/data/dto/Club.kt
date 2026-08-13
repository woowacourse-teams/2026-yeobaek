package com.yeobaek.data.dto

import com.yeobaek.data.model.GroupModel
import kotlinx.serialization.Serializable

@Serializable
data class Club(
    val book: Book,
    val clubId: Int,
    val memberCount: Int,
    val myProgress: MyProgress?,
    val name: String
)

fun Club.toModel(): GroupModel {
    return GroupModel(
        book = book,
        clubId = clubId,
        memberCount = memberCount,
        myProgress = null,
        name = name,
    )
}
