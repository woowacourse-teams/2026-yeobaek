package com.yeobaek.data.dto

import com.yeobaek.data.model.MyProgressModel
import kotlinx.serialization.Serializable

@Serializable
data class MyProgress(
    val lastReadAt: String,
    val lastReadPassageSequence: Int,
    val progressRate: Int,
)

fun MyProgress.toModel(): MyProgressModel =
    MyProgressModel(
        lastReadAt = lastReadAt,
        lastReadPassageSequence = lastReadPassageSequence,
        progressRate = progressRate,
    )
