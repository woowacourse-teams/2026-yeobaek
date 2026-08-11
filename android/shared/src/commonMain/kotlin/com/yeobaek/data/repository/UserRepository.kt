package com.yeobaek.data.repository

interface UserRepository {
    fun checkNickname(nickname: String): Boolean
}
