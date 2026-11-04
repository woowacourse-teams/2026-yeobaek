package com.yeobaek.core.invitation

import androidx.compose.runtime.Composable

fun interface InviteShareSheet {
    // Success means the system sheet was opened, not that an invitation was sent.
    fun share(text: String): Boolean
}

@Composable
expect fun rememberInviteShareSheet(): InviteShareSheet
