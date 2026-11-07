package com.yeobaek.feature.group.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.yeobaek.core.invitation.InviteLink

internal class JoinInputState(private val savedState: SavedStateHandle) {
    var value by mutableStateOf(JoinUiState(codeValue = savedState[CODE] ?: ""))
        private set

    fun initialize(initialCode: String?, requestId: String?) {
        if (value.isJoining) return
        if (savedState.get<Boolean>(INITIALIZED) == true && savedState.get<String>(REQUEST_ID) == requestId) return
        savedState[INITIALIZED] = true
        savedState[REQUEST_ID] = requestId
        updateCode(initialCode?.takeIf(InviteLink::isValidCode).orEmpty())
    }

    fun edit(code: String) {
        if (!value.isJoining) updateCode(code.take(6))
    }

    fun beginJoin(): String? {
        if (value.isJoining || value.successJoin) return null
        if (!InviteLink.isValidCode(value.codeValue)) {
            value = value.copy(codeState = true)
            return null
        }
        value = value.copy(isJoining = true, codeState = false)
        return value.codeValue
    }

    fun finishJoin(success: Boolean) {
        value = value.copy(isJoining = false, successJoin = success, codeState = !success)
    }

    fun cancelJoin() {
        value = value.copy(isJoining = false)
    }

    private fun updateCode(code: String) {
        savedState[CODE] = code
        value = value.copy(codeValue = code, codeState = false, successJoin = false)
    }

    private companion object {
        const val CODE = "join.input"
        const val INITIALIZED = "join.initialized"
        const val REQUEST_ID = "join.requestId"
    }
}
