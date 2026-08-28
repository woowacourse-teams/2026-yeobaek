package com.yeobaek.core.network.crash

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

object FirebaseCrashSink {
    fun log(message: String) {
        Firebase.crashlytics.log(message)
    }

    fun setCustomKeys(values: Map<String, Any>) {
        Firebase.crashlytics.setCustomKeys(values)
    }

    fun recordException(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }
}
