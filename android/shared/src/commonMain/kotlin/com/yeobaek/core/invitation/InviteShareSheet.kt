package com.yeobaek.core.invitation

import androidx.compose.runtime.Composable

fun interface InviteShareSheet {
    fun share(text: String): Boolean
}

@Composable
expect fun rememberInviteShareSheet(): InviteShareSheet
