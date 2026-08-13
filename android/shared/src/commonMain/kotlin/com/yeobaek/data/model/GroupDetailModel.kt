package com.yeobaek.data.model

data class GroupDetailModel(
    val book: BookSummaryModel,
    val groupId: Int,
    val joinCode: String,
    val members: List<MemberModel>,
    val myProgress: MyProgressModel,
    val name: String,
)
