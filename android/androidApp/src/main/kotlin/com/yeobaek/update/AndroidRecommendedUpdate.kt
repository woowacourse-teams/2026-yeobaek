package com.yeobaek.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.play.core.appupdate.AppUpdateManager
import com.yeobaek.core.designsystem.theme.YeobaekTheme

private object RecommendedUpdateBannerTokens {
    val HorizontalInset = 16.dp
    val VerticalInset = 4.dp
    val ContentSpacing = 8.dp
    val MinHeight = 56.dp
    val ButtonMinWidth = 96.dp
    val DividerWidth = 1.dp
    val MessageLineHeight = 20.sp
    val ActionFontSize = 13.sp
    const val STACKED_FONT_SCALE = 1.3f
}

@Composable
internal fun AndroidRecommendedUpdate(
    manager: AppUpdateManager,
    currentVersionCode: Int,
    session: RecommendedUpdateSession,
    content: @Composable () -> Unit,
) {
    val controller = remember(manager, currentVersionCode, session) {
        RecommendedUpdateController(
            manager,
            currentVersionCode,
            session,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        controller.onResult(result.resultCode)
    }
    val state by controller.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.resume(launcher) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { controller.pause() }
    LaunchedEffect(controller, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            controller.state.collect { update ->
                val message = update.errorMessage ?: return@collect
                controller.consumeError()
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }
    YeobaekTheme {
        val readyToInstall = state.stage == RecommendedUpdateStage.READY_TO_INSTALL
        Surface(
            color = if (readyToInstall) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.background
            },
        ) {
            Column(
                Modifier.fillMaxSize().then(
                    if (readyToInstall) Modifier.navigationBarsPadding() else Modifier,
                ),
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(horizontal = RecommendedUpdateBannerTokens.HorizontalInset).then(
                        if (snackbarHostState.currentSnackbarData != null && !readyToInstall) {
                            Modifier.navigationBarsPadding()
                        } else {
                            Modifier
                        },
                    ),
                )
                if (readyToInstall) {
                    RecommendedUpdateBanner(
                        onInstall = controller::install,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecommendedUpdateBanner(
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column {
            HorizontalDivider(
                thickness = RecommendedUpdateBannerTokens.DividerWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            val contentModifier = Modifier.padding(
                horizontal = RecommendedUpdateBannerTokens.HorizontalInset,
                vertical = RecommendedUpdateBannerTokens.VerticalInset,
            )
            if (LocalDensity.current.fontScale > RecommendedUpdateBannerTokens.STACKED_FONT_SCALE) {
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(RecommendedUpdateBannerTokens.ContentSpacing),
                ) {
                    RecommendedUpdateMessage()
                    RecommendedUpdateRestartButton(onInstall = onInstall, modifier = Modifier.align(Alignment.End))
                }
            } else {
                Row(
                    modifier = Modifier.heightIn(min = RecommendedUpdateBannerTokens.MinHeight).then(contentModifier),
                    horizontalArrangement = Arrangement.spacedBy(RecommendedUpdateBannerTokens.ContentSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecommendedUpdateMessage(modifier = Modifier.weight(1f))
                    RecommendedUpdateRestartButton(onInstall = onInstall)
                }
            }
        }
    }
}

@Composable
private fun RecommendedUpdateMessage(modifier: Modifier = Modifier) {
    Text(
        text = "새 버전 설치 준비를 마쳤어요",
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = RecommendedUpdateBannerTokens.MessageLineHeight,
            lineBreak = LineBreak.Heading,
            localeList = LocaleList("ko-KR"),
        ),
    )
}

@Composable
private fun RecommendedUpdateRestartButton(onInstall: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onInstall,
        modifier = modifier.widthIn(min = RecommendedUpdateBannerTokens.ButtonMinWidth),
        contentPadding = PaddingValues(horizontal = RecommendedUpdateBannerTokens.ContentSpacing),
    ) {
        Text(
            text = "지금 재시작",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = RecommendedUpdateBannerTokens.ActionFontSize),
        )
    }
}

@Preview(name = "재시작 안내", group = "권장 업데이트", widthDp = 390, showBackground = true)
@Composable
private fun RecommendedUpdateReadyPreview() {
    YeobaekTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RecommendedUpdateBanner(onInstall = {})
        }
    }
}

@Preview(name = "실패 알림", group = "권장 업데이트", widthDp = 390, showBackground = true)
@Composable
private fun RecommendedUpdateFailedPreview() {
    YeobaekTheme {
        Box(Modifier.padding(RecommendedUpdateBannerTokens.HorizontalInset)) {
            Snackbar {
                Text("업데이트를 다운로드하지 못했어요.")
            }
        }
    }
}
