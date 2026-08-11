package com.yeobaek.data.repository

interface UserRepository {
    val nickname: String
    fun checkNickname(nickname: String): Boolean
    fun setNickname(nickname: String)
}
