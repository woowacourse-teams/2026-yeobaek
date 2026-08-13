package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.UserApi
import com.yeobaek.data.dto.UserRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.UserRepository

class RemoteUserRepositoryImpl(
    private val userApi: UserApi,
) : UserRepository {
    override suspend fun setUserData(userData: UserModel): UserModel {
        return userApi.createUser(UserRequest(nickname = userData.name)).toModel()
    }
}
