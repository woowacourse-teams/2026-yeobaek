package com.yeobaek.data.repository

import com.yeobaek.data.model.UserModel

interface UserRepository {
    var userModel: UserModel
    suspend fun setUserData(nickname: String): UserModel
}
