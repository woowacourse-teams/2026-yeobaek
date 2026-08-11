package com.yeobaek.feature.group.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository

class JoinStateHolder(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
) {
    var codeValue by mutableStateOf("")
        private set

    var codeState by mutableStateOf(false)
        private set

    fun initInputValue() {
        codeValue = ""
        codeState = false
    }

    fun onCodeValueChange(value: String) {
        codeState = false
        codeValue = value
    }

    fun joinGroup() {
        if (!codeState) groupRepository.joinGroup(
            code = codeValue,
            username = userRepository.nickname,
        ) else throw IllegalArgumentException("코드 입력이 잘못되었다.")
    }

    fun checkValue() {
        codeState = groupRepository.checkCode(codeValue)
    }
}
