package com.yeobaek.data.repository

import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassagesModel

interface ReaderRepository {
    suspend fun getPassages(
        groupId: Int,
        from: Int,
        to: Int,
    ): PassagesModel

    suspend fun updatePassage(
        clubId: Int,
        passageId: Int,
    ): MyProgressModel
}
