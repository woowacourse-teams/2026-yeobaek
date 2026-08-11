package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.repository.UserRepository

class MockUserRepositoryImpl : UserRepository {
    override var nickname: String = ""
        private set

    override fun checkNickname(nickname: String): Boolean {
        return MockData.mockUsers.any{ it.name == nickname } || nickname.isBlank()
    }

    override fun setNickname(nickname: String) {
        this.nickname = nickname
    }
}
