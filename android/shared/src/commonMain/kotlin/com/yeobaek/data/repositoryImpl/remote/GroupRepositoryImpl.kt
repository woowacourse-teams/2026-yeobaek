package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.dto.ClubRequest
import com.yeobaek.data.dto.JoinRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.GroupDetailModel
import com.yeobaek.data.model.GroupModel
import com.yeobaek.data.repository.GroupRepository

class GroupRepositoryImpl(
    private val clubApi: ClubApi,
) : GroupRepository {
    override suspend fun getGroups(): List<GroupModel> {
        val response = clubApi.getUserClubs()

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("그룹 정보가 없네요")
        } else {
            throw IllegalArgumentException("그룹 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun createGroup(
        groupName: String,
        bookId: Long,
    ) {
        val response = clubApi.createClub(
            request = ClubRequest(bookId = bookId, name = groupName),
        )

        if (!response.isSuccessful) {
            throw IllegalArgumentException("그룹을 생성하는데 실패했습니다.")
        }
    }

    override suspend fun joinGroup(joinCode: String) {
        val response = clubApi.joinClub(
            request = JoinRequest(joinCode = joinCode),
        )

        if (!response.isSuccessful) {
            throw IllegalArgumentException("그룹 가입에 실패했습니다 ${response.status}")
        }
    }

    override suspend fun getGroupDetail(groupId: Long): GroupDetailModel {
        val response = clubApi.getClubDetail(
            clubId = groupId,
        )

        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("그룹 정보가 없네요")
        } else {
            throw IllegalArgumentException("그룹 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun exitGroup(groupId: Long) {
        clubApi.exitClub(clubId = groupId)
    }
}
