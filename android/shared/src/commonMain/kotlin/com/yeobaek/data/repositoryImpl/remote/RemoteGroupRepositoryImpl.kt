package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.dto.ClubRequest
import com.yeobaek.data.dto.JoinRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.repository.GroupRepository

class RemoteGroupRepositoryImpl(
    private val clubApi: ClubApi,
) : GroupRepository {
    override suspend fun getGroups(
        userId: Int,
    ): List<GroupModel> {
        val response = clubApi.getUserClubs(userId)

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("그룹 정보가 없네요")
        } else {
            throw IllegalArgumentException("그룹 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun createGroup(
        groupName: String,
        userId: Int,
        bookId: Int,
    ) {
        val response = clubApi.createClub(
            userId = userId,
            request = ClubRequest(bookId = bookId, name = groupName),
        )

        if (!response.isSuccessful) {
            throw IllegalArgumentException("그룹을 생성하는데 실패했습니다.")
        }
    }

    override suspend fun joinGroup(joinCode: String, userId: Int) {
        val response = clubApi.joinClub(
            userId = userId,
            request = JoinRequest(joinCode = joinCode),
        )

        if (!response.isSuccessful) {
            throw IllegalArgumentException("그룹 가입에 실패했습니다 ${response.status}")
        }
    }

    override suspend fun getGroupDetail(userId: Int, groupId: Int): GroupDetailModel {
        val response = clubApi.getClubDetail(
            userId = userId,
            clubId = groupId,
        )

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("그룹 정보가 없네요")
        } else {
            throw IllegalArgumentException("그룹 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }
}
