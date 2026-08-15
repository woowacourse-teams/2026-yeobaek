package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.GroupRepository

class GroupRepositoryImpl(
    private val clubApi: ClubApi,
) : GroupRepository {
    override fun getGroups(): List<GroupModel> {
        TODO("Not yet implemented")
    }

    override fun createGroup(
        groupName: String,
        userData: UserModel,
        book: BookModel,
    ) {
        TODO("Not yet implemented")
    }

    override fun joinGroup(code: String, userData: UserModel) {
        TODO("Not yet implemented")
    }

    override fun checkCode(code: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getGroupDetail(groupId: Int): GroupDetailModel = clubApi
        .getClubDetail(clubId = groupId)
        .toModel()
}
