package com.yeobaek.data.repository

import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel

interface GroupRepository {
    fun getGroups(): List<GroupModel>
    fun createGroup(groupName: String, userData: UserModel, book: BookModel)
    fun joinGroup(code: String, userData: UserModel)
    fun checkCode(code: String): Boolean
    suspend fun getGroupDetail(groupId: Int): GroupDetailModel
}
