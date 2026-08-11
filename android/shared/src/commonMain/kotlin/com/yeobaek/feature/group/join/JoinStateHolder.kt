package com.yeobaek.feature.group.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.GroupRepository

class JoinStateHolder(
    private val groupRepository: GroupRepository
) {
    var codeValue by mutableStateOf("")
        private set

    fun onCodeValueChange(value: String) {
        codeValue = value
    }

    fun joinGroup() {
        groupRepository.joinGroup(codeValue) // 코드로 참여하려고 함
    }
}
