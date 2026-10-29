package com.yeobaek.devtools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.config.ConfigFetchStatus
import com.yeobaek.core.config.ConfigValueSource
import com.yeobaek.core.config.RemoteConfigKeys
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.repository.AppConfigRepository
import kotlin.time.Instant

@Composable
fun DevToolsScreen(
    appContainer: AppContainer,
    appBuild: String,
    buildType: String,
    navigateBack: () -> Unit,
) {
    YeobaekTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("개발자 도구") },
                    navigationIcon = { TextButton(onClick = navigateBack) { Text("뒤로") } },
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ToolSection("앱 정보") {
                    Info("앱 버전", appContainer.appVersion)
                    Info("빌드 번호", appBuild)
                    Info("빌드 타입", buildType)
                    Info("연결된 서버", appContainer.apiBaseUrl)
                }
                RemoteConfigTools(appContainer.appConfigRepository)
            }
        }
    }
}

@Composable
private fun RemoteConfigTools(repository: AppConfigRepository) {
    val config by repository.state.collectAsState()

    ToolSection("원격 설정 수신 상태") {
        Info("초기화", if (config.isInitialized) "완료" else "준비되지 않음 · 기본값 사용")
        Info(
            "최근 가져오기 상태",
            when (config.lastFetchStatus) {
                ConfigFetchStatus.NOT_FETCHED -> "기록 없음"
                ConfigFetchStatus.SUCCESS -> "성공"
                ConfigFetchStatus.FAILURE -> "실패"
                ConfigFetchStatus.THROTTLED -> "요청 제한"
            },
        )
        Info("마지막 조회 시도 (UTC)", formatTime(config.lastFetchAttemptAtMillis))
        Info("마지막 성공 수신 (UTC)", formatTime(config.lastSuccessfulFetchAtMillis))
        Info("이번 실행의 마지막 활성화 (UTC)", formatTime(config.lastActivationAtMillis))
        config.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    ToolSection("댓글 좋아요") {
        Text(RemoteConfigKeys.COMMENT_LIKE_ENABLED, style = MaterialTheme.typography.labelMedium)
        Info("현재 적용 값", config.featureFlags.commentLikeEnabled.toString())
        Info(
            "적용 값의 출처",
            sourceLabel(config.valueSources[RemoteConfigKeys.COMMENT_LIKE_ENABLED]),
        )
    }

    ToolSection("온보딩 제목") {
        Text(RemoteConfigKeys.ONBOARDING_TITLE, style = MaterialTheme.typography.labelMedium)
        Info("현재 적용 문구", config.sessionMessages.onboardingTitle)
        Info(
            "적용 값의 출처",
            sourceLabel(config.valueSources[RemoteConfigKeys.ONBOARDING_TITLE]),
        )
        Text(
            "새 원격 문구는 다음 앱 실행부터 적용됩니다.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ToolSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun sourceLabel(source: ConfigValueSource?): String = when (source) {
    ConfigValueSource.REMOTE -> "Firebase 원격값"
    ConfigValueSource.DEFAULT, null -> "앱 기본값"
}

private fun formatTime(value: Long?): String = value?.let { Instant.fromEpochMilliseconds(it).toString() } ?: "기록 없음"
