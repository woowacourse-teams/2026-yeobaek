package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.GroupRepository

class MockGroupRepositoryImpl : GroupRepository {
    private val totalGroup = MockData.mockGroups.toMutableList()
    private val groups: MutableList<GroupModel> = mutableListOf()

    override fun createGroup(
        groupName: String,
        userData: UserModel,
        book: BookModel,
    ) {
        val groupData = GroupModel(
            groupId = (totalGroup.maxOfOrNull { it.groupId } ?: 0) + 1,
            groupCode = "EXAM${totalGroup.size}",
            groupName = groupName,
            book = book,
            users = listOf(
                UserModel(
                    id = userData.id,
                    name = userData.name,
                ),
            ),
        )
        totalGroup.add(groupData)
        groups.add(groupData)
    }

    override fun getGroups(): List<GroupModel> = groups.toList()
    override fun joinGroup(code: String, userData: UserModel) {
        if (checkCode(code)) throw IllegalArgumentException("존재하지 않는 모임입니다.")
        val joinGroup = totalGroup.find { it.groupCode == code }!!
        val addUserGroup = joinGroup.copy(
            users = joinGroup.users +
                UserModel(
                    id = userData.id,
                    name = userData.name,
                ),
        )
        groups.add(addUserGroup)
    }

    override fun checkCode(code: String): Boolean {
        val totalCheck = totalGroup.find { it.groupCode == code } == null
        val myGroupCheck = groups.find { it.groupCode == code } == null
        return totalCheck || !myGroupCheck
    }

    override suspend fun getGroupDetail(groupId: Int): GroupDetailModel {
        TODO("Not yet implemented")
    }
}
