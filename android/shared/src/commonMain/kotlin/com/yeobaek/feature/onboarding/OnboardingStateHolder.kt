package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class OnboardingStateHolder {
    var codeValue by mutableStateOf("")

    fun onCodeValueChange(inputValue: String) {
        codeValue = inputValue
    }
}
