package com.yeobaek.feature.group.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class JoinStateHolder {
    var codeValue by mutableStateOf("")
        private set

    fun onCodeValueChange(value: String) {
        codeValue = value
    }
}
