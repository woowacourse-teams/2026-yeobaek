package com.yeobaek.core.common

import androidx.compose.ui.platform.ClipEntry

expect fun String.toClipEntry(): ClipEntry

expect fun shouldCopySnackbar(): Boolean
