package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.UserRepository
import kotlin.random.Random

class MockUserRepositoryImpl : UserRepository {
    override var userData: UserModel = UserModel()
        private set

    override fun checkNickname(nickname: String): Boolean {
        return MockData.mockUsers.any{ it.name == nickname } || nickname.isBlank()
    }

    override fun setUserData(nickname: String) {
        this.userData = UserModel(
            id = Random.nextInt(100, 1000),
            name = nickname
        )
    }
}
