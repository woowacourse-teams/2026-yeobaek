package com.yeobaek

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class YeobaekMessagingService : FirebaseMessagingService() {

    override fun onRegistered(installationId: String) {
        (application as YeobaekApplication).appContainer.updateFirebaseInstallationId(installationId)
        if (BuildConfig.DEBUG) Log.d("FCM", "Registered installation ID: $installationId")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (BuildConfig.DEBUG) Log.d("FCM", "Message received: ${message.messageId}")
    }
}
