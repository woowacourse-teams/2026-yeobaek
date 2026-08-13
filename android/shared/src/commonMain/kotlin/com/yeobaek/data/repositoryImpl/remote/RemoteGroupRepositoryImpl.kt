package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.dto.ClubRequest
import com.yeobaek.data.dto.JoinRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.GroupRepository

class RemoteGroupRepositoryImpl(
    private val clubApi: ClubApi,
) : GroupRepository {
    override suspend fun getGroups(
        userId: Int,
    ): List<GroupModel> {
        return clubApi.getUserClubs(userId).toModel()
    }

    override suspend fun createGroup(
        groupName: String,
        userId: Int,
        book: BookModel,
    ) {
        clubApi.createClub(
            userId = userId,
            request = ClubRequest(bookId = book.id, name = groupName),
        )
    }

    override suspend fun joinGroup(joinCode: String, userId: Int) {
        clubApi.joinClub(
            userId = userId,
            request = JoinRequest(joinCode = joinCode),
        )
    }

    override suspend fun getGroupDetail(userId: Int, groupId: Int): GroupDetailModel {
        return clubApi.getClubDetail(
            userId = userId,
            clubId = groupId,
        ).toModel()
    }
}
