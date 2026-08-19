package com.yeobaek.data.repository

import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassagesModel

interface ReaderRepository {
    suspend fun getPassages(
        groupId: Long,
        from: Int,
        to: Int,
    ): PassagesModel

    suspend fun updatePassage(
        clubId: Long,
        passageId: Long,
    ): MyProgressModel
}
