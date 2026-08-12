package com.yeobaek.data.repository

import com.yeobaek.data.model.UserModel

interface UserRepository {
    val userData: UserModel
    fun checkNickname(nickname: String): Boolean
    fun setUserData(nickname: String)
}
