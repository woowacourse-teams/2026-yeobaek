package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.GroupRepository

class MockGroupRepositoryImpl : GroupRepository {
    private val groups = MockData.mockGroups.toMutableList()

    override fun createGroup(
        groupName: String,
        book: BookModel
    ) {
        groups.add(
            GroupModel(
                groupCode = "EXAM00",
                groupName = groupName,
                book = book,
                users = listOf(
                    UserModel(
                        name = "나"
                    )
                )
            )
        )
    }
    override fun getGroups(): List<GroupModel> {
        return groups
    }
    override fun joinGroup(code: String): GroupModel {
        val group = groups.find { it.groupCode == code }
        return group ?: throw IllegalArgumentException("존재하지 않는 모임입니다.")
    }
}
