package com.yeobaek.data.dto

import com.yeobaek.data.model.GroupModel
import kotlinx.serialization.Serializable

@Serializable
data class ClubsResponse(
    val clubs: List<Club>,
)

fun ClubsResponse.toModel(): List<GroupModel> = clubs.map { club ->
    club.toModel()
}
