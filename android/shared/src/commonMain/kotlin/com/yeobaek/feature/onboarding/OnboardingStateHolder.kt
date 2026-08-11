package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository

class OnboardingStateHolder(
    private val groupRepository: GroupRepository
) {
    var codeValue by mutableStateOf("")

    fun onCodeValueChange(inputValue: String) {
        codeValue = inputValue
    }

    fun joinGroup() {
        groupRepository.joinGroup(codeValue)
    }
}
