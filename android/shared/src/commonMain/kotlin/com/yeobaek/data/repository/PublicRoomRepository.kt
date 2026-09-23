package com.yeobaek.data.repository

import com.yeobaek.data.model.PublicRoomModel
import com.yeobaek.data.model.PublicRoomSort
import com.yeobaek.data.model.VisitedPublicRoomModel

interface PublicRoomRepository {
    suspend fun getPublicRooms(sort: PublicRoomSort? = null): List<PublicRoomModel>

    suspend fun getVisitedPublicRooms(): List<VisitedPublicRoomModel>
}
