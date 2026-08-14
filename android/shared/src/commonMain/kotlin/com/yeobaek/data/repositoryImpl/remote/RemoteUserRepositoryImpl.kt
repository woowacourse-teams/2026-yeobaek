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
        val setUser = userApi.createUser(UserRequest(nickname = nickname)).toModel()
        userModel =userModel.copy(
            id = setUser.id,
            name = setUser.name
        )
        println("레파지토리에서 유저 정보 : ${userModel.id}, ${userModel.name}")
        return userModel
    }
}
