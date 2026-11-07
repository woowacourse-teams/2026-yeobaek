package com.yeobaek.feature.survey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yeobaek.core.common.LocalAppPromptBlocked
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.survey.SurveyLinkLauncher
import com.yeobaek.core.survey.SurveyLinkOpener
import com.yeobaek.core.survey.SurveyPromptPolicy
import com.yeobaek.core.survey.rememberSurveyLinkOpener
import com.yeobaek.data.local.SurveyPreferences
import com.yeobaek.data.repository.SurveyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Composable
fun HomeSurveyPrompt(
    userId: Int?,
    config: SurveyConfig,
    repository: SurveyRepository,
    preferences: SurveyPreferences,
    linkLauncher: SurveyLinkLauncher,
    homeReady: Boolean,
    otherPromptPending: Boolean = false,
    linkOpener: SurveyLinkOpener = rememberSurveyLinkOpener(),
) {
    if (userId == null || userId <= 0 || config.id.isBlank()) return
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val blocked = LocalAppPromptBlocked.current || otherPromptPending
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val currentConfig by rememberUpdatedState(config)

    key(userId, config.id) {
        var offered by rememberSaveable { mutableStateOf(false) }
        val active = homeReady && lifecycleState == Lifecycle.State.RESUMED && !blocked && config.enabled
        // 자신의 다이얼로그가 홈 윈도우의 포커스를 가져가도 팝업은 유지합니다.
        val canCheck = active && (windowFocused || offered)

        LaunchedEffect(canCheck, repository, preferences) {
            if (!canCheck) return@LaunchedEffect
            val record = preferences.getState(userId, config.id)
            if (record.dismissed || record.linkOpened) {
                offered = false
                return@LaunchedEffect
            }
            // 이미 띄운 팝업의 복원은 새로운 자동 노출과 구분합니다.
            if (offered || record.shown) return@LaunchedEffect
            try {
                val required = repository.isSurveyRequired()
                currentCoroutineContext().ensureActive()
                offered = SurveyPromptPolicy.shouldShow(
                    currentConfig,
                    required,
                    preferences.getState(userId, config.id),
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                offered = false
            }
        }

        val record = preferences.getState(userId, config.id)
        if (offered && !record.dismissed && !record.linkOpened) {
            SurveyPrompt(
                userId = userId,
                config = config,
                preferences = preferences,
                linkLauncher = linkLauncher,
                onClose = { offered = false },
                linkOpener = linkOpener,
                visible = active,
            )
        }
    }
}
