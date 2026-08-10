package com.yeobaek.feature.group.detail.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_back_arrow
import android.shared.generated.resources.ic_menu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun DetailTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier.size(16.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back_arrow),
                    contentDescription = "뒤로가기 아이콘",
                )
            }
        },
        actions = {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_menu),
                contentDescription = "메뉴 아이콘",
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Preview(showBackground = true, name = "상세 화면 앱 상단바")
@Composable
private fun DetailTopAppBarPreview() {
    YeobaekTheme {
        DetailTopAppBar(
            title = "어른이들을 위한 동화 읽기",
        )
    }
}
