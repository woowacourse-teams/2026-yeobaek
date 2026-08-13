package com.yeobaek.data.repository

import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel

interface GroupRepository {
    suspend fun getGroups(userId: Int): List<GroupModel>
    suspend fun createGroup(groupName: String, userId: Int, book: BookModel)
    suspend fun joinGroup(joinCode: String, userId: Int)
    suspend fun getGroupDetail(userId: Int, groupId: Int): GroupDetailModel
}
