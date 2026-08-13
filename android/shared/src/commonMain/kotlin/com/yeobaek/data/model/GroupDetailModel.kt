package com.yeobaek.data.model

import com.yeobaek.data.dto.BookSummary
import com.yeobaek.data.dto.Member
import com.yeobaek.data.dto.MyProgress

data class GroupDetailModel(
    val book: BookSummary,
    val groupId: Int,
    val joinCode: String,
    val members: List<Member>,
    val myProgress: MyProgress,
    val name: String,
)
