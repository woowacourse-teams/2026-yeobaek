package com.yeobaek

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.yeobaek.analytics.AndroidPostHogAnalyticsClient
import com.yeobaek.core.app.AppContainer
import com.yeobaek.update.FirebaseUpdatePolicySource

class YeobaekApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val isDebug = BuildConfig.DEBUG
        appContainer = AppContainer(
            isDebug = isDebug,
            appVersion = BuildConfig.VERSION_NAME,
            updatePolicySource = FirebaseUpdatePolicySource(isDebug = isDebug),
            analyticsClient = AndroidPostHogAnalyticsClient(
                application = this,
            ),
        )

        FirebaseMessaging.getInstance().register()
            .addOnFailureListener { error ->
                Log.e("FCM", "Registration failed", error)
            }
    }
}
