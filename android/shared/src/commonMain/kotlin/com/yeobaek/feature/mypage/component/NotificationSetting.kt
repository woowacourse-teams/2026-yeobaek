package com.yeobaek.feature.mypage.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_notification
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun NotificationSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .toggleable(
                value = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onEnabledChange,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_notification),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "알림 받기",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "모임 참여와 새 댓글 소식을 알려드려요.",
                style = MaterialTheme.typography.bodyMedium,
                color = YeobaekTextSecondary,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = enabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = YeobaekTextSecondary,
                uncheckedTrackColor = YeobaekNeutralContainer,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Preview(showBackground = true, name = "알림 켜짐", widthDp = 390)
@Composable
private fun NotificationSettingEnabledPreview() {
    YeobaekTheme {
        NotificationSetting(
            enabled = true,
            onEnabledChange = {},
        )
    }
}

@Preview(showBackground = true, name = "알림 꺼짐", widthDp = 390)
@Composable
private fun NotificationSettingDisabledPreview() {
    YeobaekTheme {
        NotificationSetting(
            enabled = false,
            onEnabledChange = {},
        )
    }
}
