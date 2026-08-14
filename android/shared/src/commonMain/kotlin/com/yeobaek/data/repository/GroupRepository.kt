package com.yeobaek.data.repository

import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel

interface GroupRepository {
    suspend fun getGroups(userId: Int): List<GroupModel>
    suspend fun createGroup(groupName: String, userId: Int, bookId: Int)
    suspend fun joinGroup(joinCode: String, userId: Int)
    suspend fun getGroupDetail(userId: Int, groupId: Int): GroupDetailModel
}
