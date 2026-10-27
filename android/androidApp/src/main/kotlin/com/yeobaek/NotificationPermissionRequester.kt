package com.yeobaek

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

internal class NotificationPermissionRequester(
    private val activity: ComponentActivity,
) {
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun requestOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val preferences = activity.getSharedPreferences(
            "notification_permission",
            Context.MODE_PRIVATE,
        )
        if (preferences.getBoolean("request_attempted", false)) return

        preferences.edit().putBoolean("request_attempted", true).apply()

        if (
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
