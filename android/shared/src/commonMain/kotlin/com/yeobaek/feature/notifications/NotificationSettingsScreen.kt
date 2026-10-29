package com.yeobaek.feature.notifications

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.designsystem.theme.SettingsTokens
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import org.jetbrains.compose.resources.painterResource

@Composable
fun NotificationSettingsScreen(
    uiState: NotificationSettingsUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (NotificationSetting, Boolean) -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    systemNotificationsEnabled: Boolean? = true,
    onOpenSystemSettings: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val canEdit = systemNotificationsEnabled == true && !uiState.isSaving
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbar.showSnackbar(it)
            onSaveErrorShown()
        }
    }
    LaunchedEffect(uiState.loadError) {
        if (uiState.preferences != null) uiState.loadError?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("알림 설정", style = MaterialTheme.typography.headlineMedium, color = YeobaekTextStrong)
                },
                expandedHeight = SettingsTokens.ToolbarHeight,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(Res.drawable.ic_back_arrow), contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { contentPadding ->
        val preferences = uiState.preferences
        when {
            uiState.isLoading -> Box(
                Modifier.padding(contentPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            preferences == null -> Column(
                Modifier.padding(contentPadding).fillMaxSize().padding(SettingsTokens.ContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(uiState.loadError ?: "알림 설정을 가져오지 못했습니다.")
                TextButton(onClick = onRetry) { Text("다시 시도") }
            }

            else -> Column(
                modifier = Modifier.padding(contentPadding).fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(SettingsTokens.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsTokens.SectionSpacing),
            ) {
                SettingsCard {
                    NotificationSettingRow(
                        label = "전체 알림",
                        checked = preferences.enabled,
                        enabled = canEdit,
                        onCheckedChange = { onToggle(NotificationSetting.ALL, it) },
                        modifier = Modifier.padding(SettingsTokens.CardPadding),
                        isGlobal = true,
                    )
                }
                SettingsCard {
                    Column(Modifier.padding(horizontal = SettingsTokens.CardPadding)) {
                        val items = listOf(
                            NotificationSetting.CLUB_JOIN to "모임 참여 알림",
                            NotificationSetting.CLUB_COMMENT to "모임 댓글 알림",
                            NotificationSetting.PUBLIC_ROOM_COMMENT to "공개방 댓글 알림",
                        )
                        items.forEachIndexed { index, (setting, label) ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            NotificationSettingRow(
                                label = label,
                                checked = preferences.isEnabled(setting),
                                enabled = canEdit && preferences.enabled,
                                onCheckedChange = { onToggle(setting, it) },
                            )
                        }
                    }
                }
                if (systemNotificationsEnabled == false) {
                    SettingsCard {
                        Column(
                            Modifier.padding(SettingsTokens.CardPadding),
                            verticalArrangement = Arrangement.spacedBy(SettingsTokens.InfoSpacing),
                        ) {
                            Text("기기에서 알림이 꺼져 있어요", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "알림을 받으려면 시스템 설정에서 허용해 주세요. 이 기기의 앱 알림 설정은 유지돼요.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = onOpenSystemSettings) { Text("시스템 설정 열기") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsTokens.CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(SettingsTokens.BorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.padding(vertical = SettingsTokens.BorderWidth)) { content() }
    }
}

@Composable
private fun NotificationSettingRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isGlobal: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(
            min = if (isGlobal) SettingsTokens.GlobalRowHeight else SettingsTokens.ItemRowHeight,
        )
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsTokens.CardPadding),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else YeobaekTextSecondary,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.size(width = SettingsTokens.SwitchWidth, height = SettingsTokens.GlobalRowHeight),
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                uncheckedBorderColor = MaterialTheme.colorScheme.tertiary,
                disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(
                    alpha = SettingsTokens.DISABLED_TRACK_ALPHA,
                ),
                disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun NotificationSettingsPreview() {
    YeobaekTheme {
        NotificationSettingsScreen(
            uiState = NotificationSettingsUiState(NotificationPreferences(true, true, true, true), isLoading = false),
            onBackClick = {},
            onRetry = {},
            onToggle = { _, _ -> },
            onSaveErrorShown = {},
        )
    }
}
