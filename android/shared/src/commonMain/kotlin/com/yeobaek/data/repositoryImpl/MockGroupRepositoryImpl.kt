package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.GroupRepository
import kotlin.code

class MockGroupRepositoryImpl : GroupRepository {
    private val totalGroup = MockData.mockGroups.toMutableList()
    private val groups: MutableList<GroupModel> = mutableListOf()

    override fun createGroup(
        groupName: String,
        username: String,
        book: BookModel,
    ) {
        val groupData = GroupModel(
            groupCode = "EXAM${totalGroup.size}",
            groupName = groupName,
            book = book,
            users = listOf(
                UserModel(
                    name = username,
                ),
            ),
        )
        totalGroup.add(groupData)
        groups.add(groupData)
    }

    override fun getGroups(): List<GroupModel> = groups.toList()
    override fun joinGroup(code: String, username: String) {
        if (checkCode(code)) throw IllegalArgumentException("존재하지 않는 모임입니다.")
        val joinGroup = totalGroup.find { it.groupCode == code }!!
        val addUserGroup = joinGroup.copy(
            users = joinGroup.users + UserModel(
                name = username,
            ),
        )
        groups.add(addUserGroup)
    }

    override fun checkCode(code: String): Boolean {
        val totalCheck = totalGroup.find { it.groupCode == code } == null
        val myGroupCheck = groups.find { it.groupCode == code } == null
        return totalCheck || !myGroupCheck
    }
}
