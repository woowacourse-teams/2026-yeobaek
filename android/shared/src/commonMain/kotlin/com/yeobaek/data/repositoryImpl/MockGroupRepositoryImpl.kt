package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.GroupRepository

class MockGroupRepositoryImpl : GroupRepository {
    private val totalGroup = MockData.mockGroups.toMutableList()
    private val groups: MutableList<GroupModel> = mutableListOf()

    override fun createGroup(
        groupName: String,
        book: BookModel,
    ) {
        totalGroup.add(
            GroupModel(
                groupCode = "EXAM${totalGroup.size}",
                groupName = groupName,
                book = book,
                users = listOf(
                    UserModel(
                        name = "나",
                    ),
                ),
            ),
        )
        groups.add(
            GroupModel(
                groupCode = "EXAM${totalGroup.size}",
                groupName = groupName,
                book = book,
                users = listOf(
                    UserModel(
                        name = "나",
                    ),
                ),
            ),
        )
    }
    override fun getGroups(): List<GroupModel> = groups
    override fun joinGroup(code: String) {
        if (checkCode(code)) throw IllegalArgumentException("존재하지 않는 모임입니다.")
        val joinGroup = totalGroup.find { it.groupCode == code }!!
        groups.add(joinGroup)
    }

    override fun checkCode(code: String): Boolean = totalGroup.find { it.groupCode == code } == null
}
