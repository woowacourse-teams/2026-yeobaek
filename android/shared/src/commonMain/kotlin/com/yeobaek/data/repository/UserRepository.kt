package com.yeobaek.data.repository

import com.yeobaek.data.model.LastReadingModel
import com.yeobaek.data.model.UserModel

interface UserRepository {
    suspend fun setUserData(nickname: String): UserModel
    suspend fun getLastReading(): LastReadingModel?
    fun getUserId(): Long
    fun getUsername(): String
}
