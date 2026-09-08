package com.yeobaek.feature.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekButton
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.guide.component.GroupDetailGuideCard
import com.yeobaek.feature.guide.component.HomeGuideCard
import com.yeobaek.feature.guide.component.ReaderGuideCard

@Composable
fun GuideScreen(
    modifier: Modifier = Modifier,
) {
    var currentPage by remember { mutableStateOf(1) }
    var previousEnabled by remember { mutableStateOf(false) }
    var nextEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("여백 사용법")
                },
                actions = {
                    TextButton(
                        onClick = {},
                    ) {
                        Text("건너뛰기")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (previousEnabled) {
                    YeobaekButton(
                        onClick = {
                            if (currentPage > 1) currentPage -= 1
                            if (currentPage == 1) previousEnabled = false
                            if (currentPage < 3) nextEnabled = true
                        },
                        text = "이전",
                        modifier = Modifier.weight(1f),
                        enabled = previousEnabled,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f))
                }
                YeobaekButton(
                    onClick = {
                        currentPage += 1
                        if (currentPage > 1) previousEnabled = true
                        if (currentPage == 3) nextEnabled = false
                    },
                    text = "다음",
                    modifier = Modifier.weight(1f),
                    enabled = nextEnabled,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize(),
        ) {
            when(currentPage) {
                1 -> HomeGuideCard()
                2 -> GroupDetailGuideCard()
                3 -> ReaderGuideCard()
            }
        }
    }
}

@Preview(showBackground = true, name = "가이드화면")
@Composable
private fun GuideScreenPreview() {
    YeobaekTheme {
        GuideScreen()
    }
}
