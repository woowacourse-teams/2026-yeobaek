package com.yeobaek.feature.group.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository

class JoinStateHolder(
    private val groupRepository: GroupRepository,
) {
    var codeValue by mutableStateOf("")
        private set

    var codeState by mutableStateOf(false)
        private set

    fun onCodeValueChange(value: String) {
        codeState = false
        codeValue = value
    }

    fun joinGroup() {
        if (!codeState) groupRepository.joinGroup(codeValue) else throw IllegalArgumentException("코드 입력이 잘못되었다.")
    }

    fun checkValue() {
        codeState = groupRepository.checkCode(codeValue)
    }
}
