package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.PublicRoomApi
import com.yeobaek.data.dto.UpdatePassageRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassagesModel
import com.yeobaek.data.model.PublicRoomDetailModel
import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.PublicRoomSort
import com.yeobaek.data.model.VisitedPublicRoomModel
import com.yeobaek.data.repository.PublicRoomRepository

class PublicRoomRepositoryImpl(
    private val publicRoomApi: PublicRoomApi,
) : PublicRoomRepository {
    override suspend fun getPublicRooms(sort: PublicRoomSort?): List<PublicRoomModel> {
        val response = publicRoomApi.getPublicRooms()

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("공개방 정보가 없네요")
        } else {
            throw IllegalArgumentException("공개방 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun getVisitedPublicRooms(): List<VisitedPublicRoomModel> {
        val response = publicRoomApi.getVisitedPublicRooms()

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("방문한 공개방 정보가 없네요")
        } else {
            throw IllegalArgumentException("방문한 공개방 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun getPublicRoomDetail(publicRoomId: Long): PublicRoomDetailModel {
        val response = publicRoomApi.getPublicRoomDetail(publicRoomId)

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("공개방 정보가 없네요")
        } else {
            throw IllegalArgumentException("공개방 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun visitPublicRoom(publicRoomId: Long) {
        val response = publicRoomApi.visitPublicRoom(publicRoomId)

        if (!response.isSuccessful) {
            throw IllegalArgumentException("공개방 방문에 실패했습니다 ${response.status}")
        }
    }

    override suspend fun getPassages(
        publicRoomId: Long,
        from: Int,
        to: Int,
    ): PassagesModel {
        val response = publicRoomApi.getPassages(publicRoomId, from, to)

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("문단 정보가 없네요")
        } else {
            throw IllegalArgumentException("문단 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun updatePassage(
        publicRoomId: Long,
        passageId: Long,
    ): MyProgressModel {
        val response = publicRoomApi.updatePassage(
            publicRoomId = publicRoomId,
            request = UpdatePassageRequest(passageId = passageId),
        )

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("문단 정보가 없네요")
        } else {
            throw IllegalArgumentException("문단 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }
}
