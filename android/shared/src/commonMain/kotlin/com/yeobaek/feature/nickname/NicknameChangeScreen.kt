package com.yeobaek.feature.nickname

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import android.shared.generated.resources.nickname_change_back
import android.shared.generated.resources.nickname_change_button
import android.shared.generated.resources.nickname_change_description
import android.shared.generated.resources.nickname_change_editing_helper
import android.shared.generated.resources.nickname_change_failure_helper
import android.shared.generated.resources.nickname_change_heading
import android.shared.generated.resources.nickname_change_initial_helper
import android.shared.generated.resources.nickname_change_invalid_helper
import android.shared.generated.resources.nickname_change_label
import android.shared.generated.resources.nickname_change_saving
import android.shared.generated.resources.nickname_change_title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTextField
import com.yeobaek.core.designsystem.theme.SettingsTokens
import com.yeobaek.core.designsystem.theme.YeobaekNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekOnNeutralContainer
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.platform.PlatformBackHandler
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NicknameChangeScreen(
    uiState: NicknameUiState,
    initialNickname: String,
    onNicknameValueChange: (String) -> Unit,
    onNicknameChange: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUpdating = uiState.nicknameUpdateState is NicknameUpdateState.Loading
    val isSuccess = uiState.nicknameUpdateState is NicknameUpdateState.Success
    val isBackBlocked = isUpdating || isSuccess
    val isError = !uiState.nicknameState || uiState.nicknameUpdateState is NicknameUpdateState.Failure
    PlatformBackHandler(enabled = isBackBlocked, onBack = {})
    val focusManager = LocalFocusManager.current
    LaunchedEffect(uiState.nicknameUpdateState) {
        if (uiState.nicknameUpdateState is NicknameUpdateState.Failure) focusManager.clearFocus()
    }
    val helper = when {
        !uiState.nicknameState -> Res.string.nickname_change_invalid_helper
        uiState.nicknameUpdateState is NicknameUpdateState.Failure -> Res.string.nickname_change_failure_helper
        uiState.nicknameValue == initialNickname -> Res.string.nickname_change_initial_helper
        else -> Res.string.nickname_change_editing_helper
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.nickname_change_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = YeobaekTextStrong,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !isBackBlocked,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_back_arrow),
                            contentDescription = stringResource(Res.string.nickname_change_back),
                            modifier = Modifier.size(SettingsTokens.MenuIconSize),
                            tint = Color.Unspecified,
                        )
                    }
                },
                expandedHeight = SettingsTokens.ToolbarHeight,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            YeobaekButton(
                text = stringResource(
                    if (isUpdating) Res.string.nickname_change_saving else Res.string.nickname_change_button,
                ),
                onClick = onNicknameChange,
                enabled = uiState.isEnabled &&
                    uiState.nicknameState &&
                    uiState.nicknameValue.isNotBlank() &&
                    uiState.nicknameValue != initialNickname &&
                    !isUpdating &&
                    !isSuccess,
                modifier = Modifier.navigationBarsPadding().padding(16.dp).height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = YeobaekNeutralContainer,
                    disabledContentColor = YeobaekOnNeutralContainer,
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(contentPadding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            Text(
                stringResource(Res.string.nickname_change_heading),
                style = MaterialTheme.typography.headlineMedium.copy(
                    letterSpacing = 0.sp,
                    lineHeightStyle = NicknameLineHeight,
                ),
                color = YeobaekTextStrong,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.nickname_change_description),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeightStyle = NicknameLineHeight),
                color = YeobaekTextSecondary,
            )
            Spacer(Modifier.height(42.dp))
            Text(
                stringResource(Res.string.nickname_change_label),
                style = MaterialTheme.typography.labelLarge.copy(lineHeightStyle = NicknameLineHeight),
                color = YeobaekTextStrong,
            )
            Spacer(Modifier.height(10.dp))
            YeobaekTextField(
                value = uiState.nicknameValue,
                onValueChange = onNicknameValueChange,
                placeholder = "",
                enabled = !isUpdating && !isSuccess,
                isError = isError,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeightStyle = NicknameLineHeight),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    errorContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedTextColor = if (uiState.nicknameValue == initialNickname) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(helper),
                style = MaterialTheme.typography.bodySmall.copy(lineHeightStyle = NicknameLineHeight),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private val NicknameLineHeight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

@Preview(showBackground = true, name = "닉네임 변경 / 현재 이름")
@Composable
private fun NicknameChangeInitialPreview() {
    YeobaekTheme {
        NicknameChangeScreen(NicknameUiState(nicknameValue = "조밀"), "조밀", {}, {}, {})
    }
}

@Preview(showBackground = true, name = "닉네임 변경 / 입력 중")
@Composable
private fun NicknameChangeEditingPreview() {
    YeobaekTheme {
        NicknameChangeScreen(NicknameUiState(nicknameValue = "엘리"), "조밀", {}, {}, {})
    }
}

@Preview(showBackground = true, name = "닉네임 변경 / 오류")
@Composable
private fun NicknameChangeErrorPreview() {
    YeobaekTheme {
        NicknameChangeScreen(
            NicknameUiState(nicknameValue = "엘리", nicknameState = false, isEnabled = false),
            "조밀",
            {},
            {},
            {},
        )
    }
}
