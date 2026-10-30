package com.yeobaek

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.yeobaek.analytics.AndroidPostHogAnalyticsClient
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.notification.ACTIVITY_NOTIFICATION_CHANNEL_ID
import com.yeobaek.update.FirebaseUpdatePolicySource
import com.yeobaek.update.RecommendedUpdatePreferences
import com.yeobaek.update.RecommendedUpdateSession

class YeobaekApplication : Application() {
    lateinit var appContainer: AppContainer
        private set
    lateinit var recommendedUpdateSession: RecommendedUpdateSession
        private set

    override fun onCreate() {
        super.onCreate()
        recommendedUpdateSession = RecommendedUpdateSession(
            RecommendedUpdatePreferences(getSharedPreferences("recommendedUpdates", MODE_PRIVATE)),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    ACTIVITY_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val isDebug = BuildConfig.DEBUG
        appContainer = AppContainer(
            isDebug = isDebug,
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            updatePolicySource = FirebaseUpdatePolicySource(isDebug = isDebug),
            analyticsClient = AndroidPostHogAnalyticsClient(
                application = this,
            ),
        )
        appContainer.startUpdatePolicyUpdates()

        FirebaseMessaging.getInstance().register()
            .addOnFailureListener { error ->
                Log.e("FCM", "Registration failed", error)
            }
    }
}
