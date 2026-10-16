package com.yeobaek.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter

@Composable
fun TrackScreen(
    crashReporter: CrashReporter,
    analyticsTracker: AnalyticsTracker,
    screen: TrackedScreen,
) {
    LaunchedEffect(screen) {
        analyticsTracker.trackScreen(screen.value)
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = CrashContext(
                screen = screen,
                operation = CrashOperation.SCREEN_VIEWED,
            ),
        )
    }
}
