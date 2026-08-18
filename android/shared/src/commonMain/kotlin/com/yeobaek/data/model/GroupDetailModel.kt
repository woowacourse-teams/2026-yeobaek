package com.yeobaek.data.model

data class GroupDetailModel(
    val book: BookSummaryModel,
    val groupId: Long,
    val joinCode: String,
    val members: List<MemberModel>,
    val myProgress: MyProgressModel?,
    val name: String,
)
