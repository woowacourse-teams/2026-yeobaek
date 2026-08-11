package com.yeobaek.data.repository

import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupModel

interface GroupRepository {
    fun getGroups(): List<GroupModel>
    fun createGroup(groupName: String, book: BookModel)
    fun joinGroup(code: String)
    fun checkCode(code: String): Boolean
}
