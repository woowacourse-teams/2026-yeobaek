package com.yeobaek.core.common

import android.content.ClipData
import android.os.Build
import androidx.compose.ui.platform.ClipEntry

actual fun String.toClipEntry(): ClipEntry {
    val clipData = ClipData.newPlainText(this, this)
    return ClipEntry(clipData)
}

actual fun shouldCopySnackbar(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
