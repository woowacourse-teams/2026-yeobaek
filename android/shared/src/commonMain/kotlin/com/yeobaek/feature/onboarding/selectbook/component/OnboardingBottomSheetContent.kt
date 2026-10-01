package com.yeobaek.feature.onboarding.selectbook.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingBottomSheetContent(
    title: String,
    onClickPublicRoom: () -> Unit,
    onClickCreateRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("\"$title\"을 어떻게 읽을까요?", style = MaterialTheme.typography.titleLarge)
        OnboardingActionCard(
            title = "공개방에 참여하기",
            content = "같은 책을 읽는 사람들과 바로 시작해요.",
            onClick = onClickPublicRoom,
        )
        OnboardingActionCard(
            title = "새 모임 만들기",
            content = "친구와 읽을 모임을 만들고 코드를 공유해요.",
            onClick = onClickCreateRoom,
        )
    }
}

@Composable
private fun OnboardingActionCard(
    title: String,
    content: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
