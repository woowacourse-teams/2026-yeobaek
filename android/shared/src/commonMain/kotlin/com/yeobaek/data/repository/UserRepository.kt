package com.yeobaek.data.repository

import com.yeobaek.data.model.UserModel

interface UserRepository {
    suspend fun setUserData(nickname: String): UserModel
}
