package com.yeobaek.data.repository

import com.yeobaek.data.model.MyProgressModel
import com.yeobaek.data.model.PassagesModel
import com.yeobaek.data.model.PublicRoomDetailModel
import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.PublicRoomSort
import com.yeobaek.data.model.VisitedPublicRoomModel

interface PublicRoomRepository {
    suspend fun getPublicRooms(sort: PublicRoomSort? = null): List<PublicRoomModel>

    suspend fun getVisitedPublicRooms(): List<VisitedPublicRoomModel>

    suspend fun getPublicRoomDetail(publicRoomId: Long): PublicRoomDetailModel

    suspend fun visitPublicRoom(publicRoomId: Long)

    suspend fun getPassages(
        publicRoomId: Long,
        from: Int,
        to: Int,
    ): PassagesModel

    suspend fun updatePassage(
        publicRoomId: Long,
        passageId: Long,
    ): MyProgressModel
}
