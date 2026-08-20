package com.yeobaek

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DoubleBackToExit(
    context: Context,
    onExit: () -> Unit,
) {
    var lastBackProcessedTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = SystemClock.elapsedRealtime()

        if (currentTime - lastBackProcessedTime <= 2000) {
            onExit()
        } else {
            lastBackProcessedTime = currentTime

            Toast.makeText(context, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
