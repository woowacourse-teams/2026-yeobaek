package com.yeobaek.core.invitation

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberInviteShareSheet(): InviteShareSheet {
    val context = LocalContext.current
    return remember(context) {
        InviteShareSheet { text ->
            if (text.isBlank()) {
                false
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                try {
                    context.startActivity(Intent.createChooser(intent, "모임 초대 공유"))
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            }
        }
    }
}
