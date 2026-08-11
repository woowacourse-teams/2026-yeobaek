package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository

class OnboardingStateHolder(
    private val groupRepository: GroupRepository,
) {
    var codeValue by mutableStateOf("")
    var codeState by mutableStateOf(false)

    fun onCodeValueChange(inputValue: String) {
        codeState = false
        codeValue = inputValue
    }

    fun joinGroup() {
        if (!codeState) groupRepository.joinGroup(codeValue) else throw IllegalArgumentException("코드 입력이 잘못되었다.")
    }

    fun checkValue() {
        codeState = groupRepository.checkCode(codeValue)
        if (codeState) codeValue = ""
    }
}
