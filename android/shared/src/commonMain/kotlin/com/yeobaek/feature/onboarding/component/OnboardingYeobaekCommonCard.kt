package com.yeobaek.feature.onboarding.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_bookcase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingYeobaekCommonCard(
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
        Spacer(modifier = Modifier.height(12.dp))
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

@Preview(showBackground = true, name = "온보딩 화면 중앙 카드")
@Composable
private fun OnboardingYeobaekCommonCardPreview() {
    YeobaekTheme {
        OnboardingYeobaekCommonCard()
    }
}
