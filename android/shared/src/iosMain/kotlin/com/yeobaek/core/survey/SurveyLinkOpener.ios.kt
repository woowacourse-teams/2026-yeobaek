package com.yeobaek.core.survey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberSurveyLinkOpener(): SurveyLinkOpener = remember {
    SurveyLinkOpener { value ->
        val url = NSURL.URLWithString(value) ?: return@SurveyLinkOpener false
        if (url.scheme?.lowercase() != "https") return@SurveyLinkOpener false
        suspendCancellableCoroutine { continuation ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { opened ->
                if (continuation.isActive) continuation.resume(opened)
            }
        }
    }
}
