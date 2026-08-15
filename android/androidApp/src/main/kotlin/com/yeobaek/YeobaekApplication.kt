package com.yeobaek

import android.app.Application
import com.yeobaek.core.app.AppContainer

class YeobaekApplication : Application() {

    val appContainer: AppContainer by lazy {
        AppContainer(memberId = 7)
    }
}
