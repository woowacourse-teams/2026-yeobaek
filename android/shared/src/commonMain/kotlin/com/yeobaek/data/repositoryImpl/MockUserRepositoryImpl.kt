package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.repository.UserRepository

class MockUserRepositoryImpl : UserRepository {
    override fun checkNickname(nickname: String): Boolean {
        return MockData.mockUsers.any{ it.name == nickname } || nickname.isBlank()
    }
}
