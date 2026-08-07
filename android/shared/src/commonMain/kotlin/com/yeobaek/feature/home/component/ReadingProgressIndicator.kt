package com.yeobaek.feature.home.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekAccent
import com.yeobaek.core.designsystem.theme.YeobaekSoft
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun ReadingProgressIndicator(
    progressRate: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = { progressRate / 100f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = YeobaekAccent,
            trackColor = YeobaekSoft,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            drawStopIndicator = {},
        )
        Text(
            text = "$progressRate%",
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
            ),
            color = YeobaekTextSecondary,
        )
    }
}

@Preview(showBackground = true, name = "독서 진행률")
@Composable
private fun ReadingProgressIndicatorPreview() {
    YeobaekTheme {
        ReadingProgressIndicator(progressRate = 12)
    }
}
