package com.yeobaek

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DevToolsScreen(
    appVersion: String,
    buildType: String,
    serverEnv: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("개발자 도구")
        Text("앱 버전: $appVersion")
        Text("빌드 타입: $buildType")
        Text("연결된 서버 환경: $serverEnv")
    }
}
