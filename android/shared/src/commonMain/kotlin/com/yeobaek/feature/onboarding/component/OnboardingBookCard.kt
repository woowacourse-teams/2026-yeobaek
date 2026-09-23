package com.yeobaek.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun OnboardingBookCard(
    title: String,
    authors: String,
    coverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable {
            onClick()
        },
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors().copy(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier.clip(shape = MaterialTheme.shapes.extraSmall).background(color = Color.Red)
                .fillMaxWidth(),
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "책 표지 이미지",
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            authors,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, name = "온보딩 책 카드")
@Composable
private fun OverviewBookCardPreview() {
    YeobaekTheme {
        OnboardingBookCard(
            title = "테스트",
            authors = "테스트",
            coverUrl = "",
            onClick = {},
        )
    }
}
