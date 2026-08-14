package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.UserApi
import com.yeobaek.data.dto.UserRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.UserRepository

class RemoteUserRepositoryImpl(
    private val userApi: UserApi,
) : UserRepository {
    override var userModel = UserModel()

    override suspend fun setUserData(nickname: String): UserModel {
        val response = userApi.createUser(UserRequest(nickname = nickname))

        return if (response.isSuccessful) {
            val setUser = response.body()?.toModel() ?: throw IllegalArgumentException("유저 정보가 없네요")
            userModel = userModel.copy(
                id = setUser.id,
                name = setUser.name,
            )
            userModel
        } else {
            throw IllegalArgumentException("회원 생성 실패 ${response.status}")
        }
    }
}
