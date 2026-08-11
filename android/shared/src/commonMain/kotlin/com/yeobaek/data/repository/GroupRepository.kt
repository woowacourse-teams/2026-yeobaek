package com.yeobaek.data.repository

import com.yeobaek.data.model.GroupModel

interface GroupRepository {
    fun getGroups(): List<GroupModel>
    fun createGroup(groupName: String)
    fun joinGroup(code: String): GroupModel
}
