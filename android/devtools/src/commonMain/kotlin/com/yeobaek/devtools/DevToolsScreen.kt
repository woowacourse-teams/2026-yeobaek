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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.config.ConfigFetchStatus
import com.yeobaek.core.config.ConfigValueSource
import com.yeobaek.core.config.RemoteConfigKeys
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.invitation.InviteLink
import com.yeobaek.core.invitation.rememberInviteShareSheet
import com.yeobaek.core.survey.rememberSurveyLinkOpener
import com.yeobaek.data.repository.AppConfigRepository
import com.yeobaek.feature.survey.component.SurveyPromptDialog
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

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
                InvitationTools()
                RemoteConfigTools(appContainer.appConfigRepository)
            }
        }
    }
}

@Composable
private fun InvitationTools() {
    var code by rememberSaveable { mutableStateOf("ABC123") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val shareSheet = rememberInviteShareSheet()

    fun open(uri: String) {
        try {
            uriHandler.openUri(uri)
            error = null
        } catch (_: IllegalArgumentException) {
            error = "링크를 열 수 없습니다. 앱의 URL 등록을 확인해주세요."
        }
    }

    ToolSection("모임 초대 테스트") {
        OutlinedTextField(
            value = code,
            onValueChange = { value -> code = value.filter(Char::isLetterOrDigit).uppercase().take(6) },
            label = { Text("테스트 초대코드") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("링크 실행은 코드를 자동 입력하며, 참여 버튼을 누르기 전에는 참여하지 않습니다.")
        TextButton(onClick = { open(InviteLink.create(code)) }, enabled = InviteLink.isValidCode(code)) {
            Text("초대 링크 실행")
        }
        TextButton(
            enabled = InviteLink.isValidCode(code),
            onClick = {
                error = if (shareSheet.share(InviteLink.shareText("테스트", code))) {
                    null
                } else {
                    "공유창을 열 수 없습니다."
                }
            },
        ) { Text("초대 문구 공유") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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

    SurveyTools(config.surveyConfig, config.valueSources[RemoteConfigKeys.PMF_SURVEY])
}

@Composable
private fun SurveyTools(config: SurveyConfig, source: ConfigValueSource?) {
    var previewShown by remember { mutableStateOf(false) }

    ToolSection("설문 팝업") {
        Info("설문 ID", config.id)
        Info("자동 노출 사용 여부", config.enabled.toString())
        Info("적용 값의 출처", sourceLabel(source))
        Text("현재 적용된 문구로 미리 봅니다. 대상 여부와 이전 안내 기록에 관계없이 열 수 있으며, 안내 기록은 변경하지 않습니다.")
        TextButton(onClick = { previewShown = true }) {
            Text("설문 팝업 미리보기")
        }
    }

    if (previewShown) {
        key(config.id) {
            SurveyPreviewDialog(config, onClose = { previewShown = false })
        }
    }
}

@Composable
private fun SurveyPreviewDialog(config: SurveyConfig, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val linkOpener = rememberSurveyLinkOpener()
    val currentOnClose by rememberUpdatedState(onClose)
    var opening by remember { mutableStateOf(false) }
    var openFailed by remember { mutableStateOf(false) }

    fun close() {
        if (!opening) currentOnClose()
    }

    SurveyPromptDialog(
        config = config,
        onShown = {},
        onDismissRequest = ::close,
        onDecline = ::close,
        onParticipate = {
            if (!opening) {
                opening = true
                openFailed = false
                scope.launch {
                    try {
                        val opened = linkOpener.open(config.url)
                        currentCoroutineContext().ensureActive()
                        if (opened) currentOnClose() else openFailed = true
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        openFailed = true
                    } finally {
                        opening = false
                    }
                }
            }
        },
        isOpeningLink = opening,
        openFailed = openFailed,
    )
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
