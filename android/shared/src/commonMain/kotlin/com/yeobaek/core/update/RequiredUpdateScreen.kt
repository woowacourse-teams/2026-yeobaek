package com.yeobaek.core.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekInk
import com.yeobaek.core.designsystem.theme.YeobaekPaper
import com.yeobaek.core.designsystem.theme.YeobaekShadow
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler

@Composable
fun RequiredUpdateGate(
    policy: UpdatePolicy,
    currentVersionCode: Int,
    onUpdate: () -> Unit,
    isUpdating: Boolean = false,
    errorMessage: String? = null,
    content: @Composable () -> Unit,
) {
    if (policy.isUpdateRequired(currentVersionCode)) {
        RequiredUpdateScreen(
            message = policy.forceUpdateMessage,
            onUpdate = onUpdate,
            isUpdating = isUpdating,
            errorMessage = errorMessage,
        )
    } else {
        content()
    }
}

@Composable
fun RequiredUpdateScreen(
    message: String,
    onUpdate: () -> Unit,
    isUpdating: Boolean = false,
    errorMessage: String? = null,
) {
    YeobaekTheme {
        PlatformBackHandler {}
        val safePadding = WindowInsets.safeDrawing.asPaddingValues()
        val verticalPadding = maxOf(safePadding.calculateTopPadding(), safePadding.calculateBottomPadding()) + 16.dp
        Box(
            modifier = Modifier.fillMaxSize().background(YeobaekPaper).background(YeobaekInk.copy(alpha = 0.36f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = verticalPadding),
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()
                        .dropShadow(
                            shape = RoundedCornerShape(15.dp),
                            shadow = Shadow(
                                radius = 24.dp,
                                spread = (-4).dp,
                                color = YeobaekShadow.copy(alpha = 0.14f),
                                offset = DpOffset(0.dp, 8.dp),
                            ),
                        )
                        .dropShadow(
                            shape = RoundedCornerShape(15.dp),
                            shadow = Shadow(
                                radius = 6.dp,
                                color = YeobaekShadow.copy(alpha = 0.08f),
                                offset = DpOffset(0.dp, 2.dp),
                            ),
                        )
                        .semantics { paneTitle = "업데이트가 필요해요" },
                    shape = RoundedCornerShape(15.dp),
                    color = YeobaekSurfaceElevated,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "업데이트가 필요해요",
                            modifier = Modifier.heightIn(min = 25.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = YeobaekTextStrong,
                        )
                        Text(
                            text = remember(message) {
                                message.replace(Regex("[가-힣]+")) { word ->
                                    word.value.toList().joinToString("\u2060")
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp).clearAndSetSemantics {
                                text = AnnotatedString(message)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineBreak = LineBreak.Heading,
                                localeList = LocaleList("ko-KR"),
                            ),
                            color = YeobaekTextSecondary,
                        )
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            Button(
                                onClick = onUpdate,
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    if (isUpdating) "업데이트 확인 중" else "업데이트",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "기본", group = "필수 업데이트", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RequiredUpdateScreenPreview() {
    RequiredUpdateScreen(
        message = UpdatePolicy.DEFAULT_FORCE_UPDATE_MESSAGE,
        onUpdate = {},
    )
}

@Preview(name = "확인 중", group = "필수 업데이트", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RequiredUpdateScreenUpdatingPreview() {
    RequiredUpdateScreen(
        message = UpdatePolicy.DEFAULT_FORCE_UPDATE_MESSAGE,
        onUpdate = {},
        isUpdating = true,
    )
}

@Preview(name = "오류", group = "필수 업데이트", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RequiredUpdateScreenErrorPreview() {
    RequiredUpdateScreen(
        message = UpdatePolicy.DEFAULT_FORCE_UPDATE_MESSAGE,
        onUpdate = {},
        errorMessage = "업데이트를 완료해야 계속 이용할 수 있어요.",
    )
}
