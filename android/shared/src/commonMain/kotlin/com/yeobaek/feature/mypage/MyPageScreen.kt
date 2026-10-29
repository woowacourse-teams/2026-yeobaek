package com.yeobaek.feature.mypage

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_guide
import android.shared.generated.resources.ic_notification
import android.shared.generated.resources.ic_user_delete
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.mypage.component.MyPageDeleteDialog
import com.yeobaek.feature.mypage.component.MyPageFeatureCard

@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    appVersion: String,
    onDeleteAccountClick: () -> Unit,
    deleteAccount: () -> Unit,
    navigateToNickname: () -> Unit,
    navigateToGuide: () -> Unit,
    navigateToNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.deleteState) {
        when (uiState.deleteState) {
            is DeleteState.Success -> {
                navigateToNickname()
            }

            is DeleteState.Failure -> {
                snackbarHostState.showSnackbar(
                    message = "회원 탈퇴에 실패했습니다.",
                    duration = SnackbarDuration.Short,
                )
            }

            is DeleteState.Idle, DeleteState.Loading -> {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("마이페이지", style = MaterialTheme.typography.titleLarge, color = YeobaekTextStrong)
                },
                expandedHeight = 56.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 24.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onPrimary),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.clip(shape = CircleShape).size(50.dp)
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(uiState.name.take(1), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(uiState.name, style = MaterialTheme.typography.titleMedium)
                }
            }
            MyPageFeatureCard(
                text = "알림 설정",
                icon = Res.drawable.ic_notification,
                onClick = navigateToNotificationSettings,
            )
            Spacer(modifier = Modifier.height(24.dp))
            MyPageFeatureCard(
                text = "가이드",
                icon = Res.drawable.ic_guide,
                onClick = {
                    navigateToGuide()
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            MyPageFeatureCard(
                text = "회원 탈퇴",
                contentColor = MaterialTheme.colorScheme.error,
                icon = Res.drawable.ic_user_delete,
                onClick = {
                    onDeleteAccountClick()
                    showDeleteDialog = true
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "v$appVersion",
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDeleteDialog) {
        MyPageDeleteDialog(
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                deleteAccount()
            },
        )
    }
}

@Preview(showBackground = true, name = "마이페이지")
@Composable
private fun MyPageScreenPreview() {
    YeobaekTheme {
        MyPageScreen(
            uiState = MyPageUiState(
                id = 0,
                name = "하로",
            ),
            appVersion = "1.0.0",
            onDeleteAccountClick = {},
            deleteAccount = {},
            navigateToNickname = {},
            navigateToGuide = {},
            navigateToNotificationSettings = {},
        )
    }
}
