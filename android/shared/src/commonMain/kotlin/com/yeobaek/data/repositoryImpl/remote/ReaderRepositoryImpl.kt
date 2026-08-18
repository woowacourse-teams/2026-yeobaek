package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.ReaderApi
import com.yeobaek.data.dto.UpdatePassageRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassagesModel
import com.yeobaek.data.repository.ReaderRepository

class ReaderRepositoryImpl(
    private val readerApi: ReaderApi,
) : ReaderRepository {
    override suspend fun getPassages(
        groupId: Long,
        from: Int,
        to: Int,
    ): PassagesModel = readerApi
        .getPassages(
            clubId = groupId,
            from = from,
            to = to,
        )
        .toModel()

    override suspend fun updatePassage(
        clubId: Long,
        passageId: Long,
    ): MyProgressModel = readerApi
        .updatePassage(
            clubId = clubId,
            request = UpdatePassageRequest(
                passageId = passageId,
            ),
        )
        .toModel()
}
