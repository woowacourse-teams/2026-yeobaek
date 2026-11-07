package com.yeobaek.core.survey

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSurveyLinkOpener(): SurveyLinkOpener {
    val context = LocalContext.current
    return remember(context) {
        SurveyLinkOpener { value ->
            val uri = Uri.parse(value)
            if (uri.scheme != "https" || uri.host.isNullOrBlank()) return@SurveyLinkOpener false
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
