package com.yeobaek.feature.group.detail.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import android.shared.generated.resources.ic_exit
import android.shared.generated.resources.ic_notification
import android.shared.generated.resources.ic_notification_off
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.SettingsTokens
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.group.detail.GroupNotificationUiState
import org.jetbrains.compose.resources.painterResource

private val ToolbarButtonSize = 44.dp

@Composable
fun DetailTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    notificationState: GroupNotificationUiState?,
    onNotificationClick: () -> Unit,
    onExitClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val titleInset = if (notificationState != null && onExitClick != null) ToolbarButtonSize * 2 else ToolbarButtonSize
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides ToolbarButtonSize) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .heightIn(min = SettingsTokens.ToolbarHeight)
                    .padding(horizontal = SettingsTokens.InfoSpacing),
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart).size(ToolbarButtonSize),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_back_arrow),
                        contentDescription = "뒤로가기 아이콘",
                        modifier = Modifier.size(SettingsTokens.MenuIconSize),
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.Center)
                        .padding(horizontal = titleInset + SettingsTokens.ToolbarTitleInset),
                    style = MaterialTheme.typography.headlineMedium,
                    color = YeobaekTextStrong,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (notificationState != null || onExitClick != null) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (notificationState != null) {
                            GroupNotificationButton(notificationState, onNotificationClick)
                        }
                        if (onExitClick != null) {
                            IconButton(onClick = onExitClick, modifier = Modifier.size(ToolbarButtonSize)) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_exit),
                                    contentDescription = "모임 나가기 아이콘",
                                    modifier = Modifier.size(SettingsTokens.MenuIconSize),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupNotificationButton(state: GroupNotificationUiState, onClick: () -> Unit) {
    val enabled = state.enabled
    if (enabled == null) {
        IconButton(onClick = onClick, enabled = !state.isLoading, modifier = Modifier.size(ToolbarButtonSize)) {
            Icon(
                painter = painterResource(Res.drawable.ic_notification),
                contentDescription = if (state.isLoading) "모임 알림 설정 불러오는 중" else "모임 알림 설정 다시 불러오기",
                modifier = Modifier.size(SettingsTokens.MenuIconSize).alpha(SettingsTokens.DISABLED_TRACK_ALPHA),
                tint = Color.Unspecified,
            )
        }
        return
    }
    IconToggleButton(
        checked = enabled,
        onCheckedChange = { onClick() },
        enabled = !state.isSaving,
        modifier = Modifier.size(ToolbarButtonSize),
    ) {
        Icon(
            painter = painterResource(if (enabled) Res.drawable.ic_notification else Res.drawable.ic_notification_off),
            contentDescription = "모임 알림",
            modifier = Modifier.size(SettingsTokens.MenuIconSize),
            tint = Color.Unspecified,
        )
    }
}

@Preview(showBackground = true, name = "상세 화면 앱 상단바")
@Composable
private fun DetailTopAppBarPreview() {
    YeobaekTheme {
        DetailTopAppBar(
            title = "심야북클럽",
            onBackClick = {},
            notificationState = GroupNotificationUiState(enabled = true, isLoading = false),
            onNotificationClick = {},
            onExitClick = {},
        )
    }
}
