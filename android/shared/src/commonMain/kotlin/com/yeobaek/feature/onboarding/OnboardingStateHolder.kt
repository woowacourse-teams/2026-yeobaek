package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository

class OnboardingStateHolder(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
) {
    var codeValue by mutableStateOf("")
    var codeState by mutableStateOf(false)

    var nicknameValue by mutableStateOf("")
    var nicknameState by mutableStateOf(false)

    fun onCodeValueChange(inputValue: String) {
        codeState = false
        codeValue = inputValue
    }

    fun onNicknameValueChange(inputValue: String) {
        nicknameState = false
        nicknameValue = inputValue
    }

    fun setNickname() {
        userRepository.setUserData(nicknameValue)
    }

    fun joinGroup() {
        if (!codeState) {
            groupRepository.joinGroup(
                code = codeValue,
                userData = userRepository.userData,
            )
        } else {
            throw IllegalArgumentException("코드 입력이 잘못되었다.")
        }
    }

    fun codeValueCheck() {
        codeState = groupRepository.checkCode(codeValue)
    }

    fun nicknameValueCheck() {
        nicknameState = userRepository.checkNickname(nicknameValue)
    }
}
