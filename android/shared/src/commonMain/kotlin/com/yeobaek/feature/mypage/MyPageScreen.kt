package com.yeobaek.feature.mypage

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_user_delete
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.mypage.component.MyPageDeleteDialog
import org.jetbrains.compose.resources.painterResource

@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    appVersion: String,
    deleteAccount: () -> Unit,
    navigateToNickname: () -> Unit,
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
                    Text("마이페이지")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Card(
                modifier = Modifier.padding(20.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onPrimary),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.clip(shape = CircleShape).size(50.dp)
                            .border(width = 1.dp, color = Color.Black, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(uiState.name.first().toString())
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(uiState.name)
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable {
                    showDeleteDialog = true
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_user_delete),
                    contentDescription = "사용자 계정 삭제 아이콘",
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("회원 탈퇴")
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                "v$appVersion",
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                textAlign = TextAlign.End,
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
            deleteAccount = {},
            navigateToNickname = {},
        )
    }
}
