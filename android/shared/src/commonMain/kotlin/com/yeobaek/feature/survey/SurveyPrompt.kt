package com.yeobaek.feature.survey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.survey.SurveyLinkLauncher
import com.yeobaek.core.survey.SurveyLinkOpener
import com.yeobaek.core.survey.rememberSurveyLinkOpener
import com.yeobaek.data.local.SurveyPreferences
import com.yeobaek.feature.survey.component.SurveyPromptDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Composable
fun SurveyPrompt(
    userId: Int,
    config: SurveyConfig,
    preferences: SurveyPreferences,
    linkLauncher: SurveyLinkLauncher,
    onClose: () -> Unit,
    linkOpener: SurveyLinkOpener = rememberSurveyLinkOpener(),
    visible: Boolean = true,
) {
    if (userId <= 0 || config.id.isBlank()) return

    key(userId, config.id) {
        val currentOnClose by rememberUpdatedState(onClose)
        var shown by remember { mutableStateOf(false) }
        var request by remember { mutableStateOf(linkLauncher.pendingRequest(userId, config.id)) }
        val opening = request != null
        var closed by remember { mutableStateOf(false) }
        var openFailed by remember { mutableStateOf(false) }

        LaunchedEffect(request) {
            val pending = request ?: return@LaunchedEffect
            try {
                val opened = pending.await()
                currentCoroutineContext().ensureActive()
                if (opened) {
                    closed = true
                    currentOnClose()
                } else {
                    openFailed = true
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                openFailed = true
            } finally {
                request = null
            }
        }

        if (visible && !closed) {
            SurveyPromptDialog(
                config = config,
                enabled = shown && !closed,
                isOpeningLink = opening,
                openFailed = openFailed,
                onShown = {
                    if (!closed) {
                        preferences.markShown(userId, config.id)
                        shown = true
                    }
                },
                onDismissRequest = {
                    if (!opening && !closed) {
                        closed = true
                        currentOnClose()
                    }
                },
                onDecline = {
                    if (shown && !opening && !closed) {
                        preferences.markDismissed(userId, config.id)
                        closed = true
                        currentOnClose()
                    }
                },
                onParticipate = {
                    if (shown && !opening && !closed) {
                        openFailed = false
                        // 팝업이 제거돼도 링크 열기 결과는 앱 범위에서 저장합니다.
                        request = linkLauncher.open(userId, config.id, config.url, linkOpener)
                    }
                },
            )
        }
    }
}
