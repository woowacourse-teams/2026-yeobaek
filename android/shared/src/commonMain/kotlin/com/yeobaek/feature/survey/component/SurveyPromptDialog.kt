package com.yeobaek.feature.survey.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.survey_decline
import android.shared.generated.resources.survey_open_failed
import android.shared.generated.resources.survey_opening
import android.shared.generated.resources.survey_participate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.core.designsystem.theme.YeobaekShadow
import com.yeobaek.core.designsystem.theme.YeobaekSoft
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun SurveyPromptDialog(
    config: SurveyConfig,
    onShown: () -> Unit,
    onDismissRequest: () -> Unit,
    onDecline: () -> Unit,
    onParticipate: () -> Unit,
    enabled: Boolean = true,
    isOpeningLink: Boolean = false,
    openFailed: Boolean = false,
) {
    val currentOnShown by rememberUpdatedState(onShown)
    val density = LocalDensity.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isOpeningLink,
            dismissOnClickOutside = !isOpeningLink,
        ),
    ) {
        CompositionLocalProvider(LocalDensity provides density) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                SurveyPromptContent(config, onDecline, onParticipate, enabled, isOpeningLink, openFailed)
                LaunchedEffect(config.id) {
                    withFrameNanos { }
                    currentOnShown()
                }
            }
        }
    }
}

@Composable
private fun SurveyPromptContent(
    config: SurveyConfig,
    onDecline: () -> Unit,
    onParticipate: () -> Unit,
    enabled: Boolean,
    isOpeningLink: Boolean,
    openFailed: Boolean,
) {
    val shape = RoundedCornerShape(15.dp)
    val lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    Surface(
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 24.dp,
                    spread = (-4).dp,
                    color = YeobaekShadow.copy(alpha = 0.14f),
                    offset = DpOffset(0.dp, 8.dp),
                ),
            )
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 6.dp,
                    color = YeobaekShadow.copy(alpha = 0.08f),
                    offset = DpOffset(0.dp, 2.dp),
                ),
            )
            .testTag("survey-dialog")
            .semantics { paneTitle = config.title },
        shape = shape,
        color = YeobaekSurfaceElevated,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = config.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(
                    lineHeight = 30.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeightStyle = lineHeightStyle,
                ),
                color = YeobaekTextStrong,
            )
            Text(
                text = config.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    lineHeightStyle = lineHeightStyle,
                ),
                color = YeobaekTextSecondary,
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        enabled = enabled && !isOpeningLink,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YeobaekSoft,
                            contentColor = YeobaekTextSecondary,
                        ),
                    ) {
                        Text(
                            stringResource(Res.string.survey_decline),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Button(
                        onClick = onParticipate,
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        enabled = enabled && !isOpeningLink,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Text(
                            stringResource(
                                if (isOpeningLink) Res.string.survey_opening else Res.string.survey_participate,
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (openFailed) {
                Text(
                    text = stringResource(Res.string.survey_open_failed),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(name = "PMF 설문 팝업", widthDp = 360, showBackground = true)
@Composable
private fun SurveyPromptPreview() {
    YeobaekTheme {
        Box(Modifier.padding(20.dp)) {
            SurveyPromptContent(SurveyConfig.DEFAULT, {}, {}, true, false, false)
        }
    }
}
