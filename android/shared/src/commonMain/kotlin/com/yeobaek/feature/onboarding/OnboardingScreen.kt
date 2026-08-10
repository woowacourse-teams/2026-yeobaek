package com.yeobaek.feature.onboarding

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_bookcase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.component.YeobaekTextField
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingScreen(
    stateHolder: OnboardingStateHolder = OnboardingStateHolder(),
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("여백", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth())
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize().verticalScroll(
                    state = rememberScrollState(),
                ),
            ) {
                OnboardingYeobaekCommonCard()
                OnboardingYeobaekTextField(
                    title = "참여 코드 입력",
                    value = stateHolder.codeValue,
                    onValueChange = {
                        stateHolder.onCodeValueChange(it)
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                YeobaekButton(
                    text = "모임 참여하기",
                    onClick = {},
                )
                OnboardingHorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                OnboardingGroupCreateCard(
                    text = "새 모임 만들기",
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun OnboardingYeobaekCommonCard(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            "READ TOGETHER",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            ),
        )
        Text(
            "책 한 권의 여백을 \n친구와 나눠보세요",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp,
                letterSpacing = (2).sp,
                fontSize = 24.sp,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("같은 책을 각자의 속도로 읽고, \n마음에 남은 문장과 생각을 교환해요.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                    .size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_bookcase),
                    contentDescription = "책장 아이콘",
                    modifier = Modifier.padding(60.dp).fillMaxSize(),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

    }
}

@Composable
private fun OnboardingYeobaekTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        YeobaekTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "예: BOOK42",
        )

    }
}

@Composable
private fun OnboardingHorizontalDivider(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "또는",
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.5f),
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OnboardingGroupCreateCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("아직 초대받은 코드가 없으신가요?", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onClick,
        ) {
            Text(text)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("둘러보기", style = MaterialTheme.typography.bodySmall)

    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    YeobaekTheme {
        OnboardingScreen()
    }
}
