package com.yeobaek.data.repository

import com.yeobaek.data.model.RecentReadingModel
import com.yeobaek.data.model.UserModel

interface UserRepository {
    suspend fun setUserData(nickname: String): UserModel
    suspend fun getRecentReading(): RecentReadingModel?
    suspend fun getUserId(): Int
    suspend fun getUsername(): String
    suspend fun deleteAccount()
    suspend fun blockUser(userId: Int)
    suspend fun unBlockUser(userId: Int)
}
