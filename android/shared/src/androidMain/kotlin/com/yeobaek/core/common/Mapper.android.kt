package com.yeobaek.core.common

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun String.toClipEntry(): ClipEntry {
    val clipData = ClipData.newPlainText(this, this)
    return ClipEntry(clipData)
}
