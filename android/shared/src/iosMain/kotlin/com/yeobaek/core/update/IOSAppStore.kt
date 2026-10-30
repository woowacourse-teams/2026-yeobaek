package com.yeobaek.core.update

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal object IOSAppStore {
    const val APP_ID = "6807303389"
    const val BUNDLE_ID = "com.yeobaek.haro"

    suspend fun open(): Boolean = openUrl("itms-apps://apps.apple.com/app/id$APP_ID") ||
        openUrl("https://apps.apple.com/app/id$APP_ID")

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun openUrl(value: String): Boolean {
        val url = NSURL.URLWithString(value) ?: return false
        return suspendCancellableCoroutine { continuation ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { opened ->
                if (continuation.isActive) continuation.resume(opened)
            }
        }
    }
}
