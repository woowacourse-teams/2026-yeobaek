package com.yeobaek

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

internal fun Context.showSystemNotification(message: RemoteMessage) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val manager = getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return
    val body = message.notification?.body ?: message.data["body"] ?: return
    val notificationId = message.messageId ?: UUID.randomUUID().toString()
    val contentIntent = createNotificationContentIntent(message, notificationId) ?: return
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, getString(R.string.notification_channel_id))
    } else {
        Notification.Builder(this)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
    }
    val notification = builder
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name))
        .setContentText(body)
        .setStyle(Notification.BigTextStyle().bigText(body))
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .build()
    manager.notify(notificationId, 0, notification)
}

private fun Context.createNotificationContentIntent(
    message: RemoteMessage,
    notificationId: String,
): PendingIntent? {
    val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    intent.data = Uri.Builder()
        .scheme(packageName)
        .authority("notification")
        .appendPath(notificationId)
        .build()
    message.data.forEach { (key, value) -> intent.putExtra(key, value) }
    return PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
