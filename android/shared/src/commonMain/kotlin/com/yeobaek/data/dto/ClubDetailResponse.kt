package com.yeobaek.data.dto

import com.yeobaek.data.model.GroupDetailModel
import kotlinx.serialization.Serializable

@Serializable
data class ClubDetailResponse(
    val book: BookSummary,
    val clubId: Int,
    val joinCode: String,
    val members: List<Member>,
    val myProgress: MyProgress,
    val name: String,
)

fun ClubDetailResponse.toModel(): GroupDetailModel =
    GroupDetailModel(
        book = book,
        groupId = clubId,
        joinCode = joinCode,
        members = members.map { it.toModel() },
        myProgress = myProgress,
        name = name,
    )
