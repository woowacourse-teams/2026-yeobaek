package com.yeobaek.feature.group.join

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTextField
import com.yeobaek.core.designsystem.component.YeobaekTopAppBar
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun JoinScreen(
    modifier: Modifier = Modifier,
) {
    var codeValue by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize(),
        topBar = {
            YeobaekTopAppBar(
                title = "모임 참여하기",
                onBackClick = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.secondary, shape = CircleShape)
                    .size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "6",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 40.sp,
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                    Text(
                        "자리",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "JOIN A READING CLUB",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "친구에게 받은 \n참여 코드를 입력해주세요",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "코드를 확인하면 함께 읽는 책과 \n친구들의 모임에 바로 참여할 수 있어요.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Spacer(modifier = Modifier.height(36.dp))
            YeobaekTextField(
                value = codeValue,
                onValueChange = {
                    codeValue = it
                },
                placeholder = "예: BOOK42",
            )
            Spacer(modifier = Modifier.height(12.dp))
            YeobaekButton(
                text = "모임 참여하기",
                onClick = {},
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "참여 코드는 모임에 참여중인 친구에게 받을 수 있어요.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    }
}

@Preview(showBackground = true, name = "그룹 참여 화면")
@Composable
private fun JoinScreenPreview() {
    YeobaekTheme {
        JoinScreen()
    }
}
