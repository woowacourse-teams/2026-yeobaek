package com.yeobaek.core.invitation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberInviteShareSheet(): InviteShareSheet {
    val controller = LocalUIViewController.current
    return remember(controller) {
        InviteShareSheet { text ->
            val view = controller.view
            if (text.isBlank() || view.window == null || controller.presentedViewController != null) {
                false
            } else {
                val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
                sheet.popoverPresentationController?.apply {
                    sourceView = view
                    sourceRect = view.bounds.useContents {
                        CGRectMake(size.width / 2, size.height / 2, 1.0, 1.0)
                    }
                    permittedArrowDirections = 0uL
                }
                controller.presentViewController(sheet, animated = true, completion = null)
                true
            }
        }
    }
}
