package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.UserApi
import com.yeobaek.data.dto.UserRequest
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.local.UserPreferences
import com.yeobaek.data.model.LastReadingModel
import com.yeobaek.data.model.UserModel
import com.yeobaek.data.repository.UserRepository
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK

class UserRepositoryImpl(
    private val userApi: UserApi,
    private val userPreferences: UserPreferences,
) : UserRepository {
    override suspend fun setUserData(nickname: String): UserModel {
        val response = userApi.createUser(UserRequest(nickname = nickname))

        return if (response.isSuccessful) {
            val setUser = response.body()?.toModel() ?: throw IllegalArgumentException("유저 정보가 없네요")

            userPreferences.saveUser(
                setUser.id,
                setUser.name,
            )
            setUser
        } else {
            throw IllegalArgumentException("회원 생성 실패 ${response.status}")
        }
    }

    override suspend fun getLastReading(): LastReadingModel? {
        val response = userApi.getLastReading()

        return when (response.status) {
            OK -> {
                response.body()?.toModel() ?: throw IllegalArgumentException("마지막으로 읽은 책 정보가 없네요")
            }

            NoContent -> {
                null
            }

            else -> {
                throw IllegalArgumentException("마지막으로 읽은 책 정보를 가져오는데 실패했습니다. ${response.status}")
            }
        }
    }

    override fun getUserId(): Int = userPreferences.getUserId() ?: throw IllegalArgumentException("유저 아이디 정보가 없네요")

    override fun getUsername(): String = userPreferences.getUsername() ?: throw IllegalArgumentException(
        "유저 이름 정보가 없네요",
    )
}
