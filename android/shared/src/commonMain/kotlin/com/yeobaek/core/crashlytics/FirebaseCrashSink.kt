package com.yeobaek.core.crashlytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

interface CrashSink {
    fun log(message: String)

    fun setCustomKeys(values: Map<String, Any>)

    fun recordException(throwable: Throwable)
}

object FirebaseCrashSink : CrashSink {
    override fun log(message: String) {
        Firebase.crashlytics.log(message)
    }

    override fun setCustomKeys(values: Map<String, Any>) {
        Firebase.crashlytics.setCustomKeys(values)
    }

    override fun recordException(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }
}
