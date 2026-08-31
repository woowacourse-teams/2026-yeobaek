package com.yeobaek.data.repository

import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel

interface GroupRepository {
    suspend fun getGroups(): List<GroupModel>
    suspend fun createGroup(groupName: String, bookId: Long)
    suspend fun joinGroup(joinCode: String)
    suspend fun getGroupDetail(groupId: Long): GroupDetailModel
    suspend fun exitGroup(groupId: Long)
}
