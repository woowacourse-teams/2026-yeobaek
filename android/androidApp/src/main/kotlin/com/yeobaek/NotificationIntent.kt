package com.yeobaek

import android.content.Intent
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.notification.CommentNotification

internal fun Intent.deliverNotificationOpen(appContainer: AppContainer) {
    val keys = listOf("type", "clubId", "publicRoomId", "passageSequence", "sentenceId")
    val data = keys.mapNotNull { key -> getStringExtra(key)?.let { key to it } }.toMap()
    if (CommentNotification.from(data) == null) return

    appContainer.onNotificationOpened(data)
    keys.forEach(::removeExtra)
}
