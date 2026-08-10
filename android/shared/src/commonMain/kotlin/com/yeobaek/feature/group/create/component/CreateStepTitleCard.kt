package com.yeobaek.feature.group.create.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun CreateStepTitleCard(
    step: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            "STEP $step",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 2.sp,
            ),
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 24.sp,
            ),
        )
    }
}

@Preview(showBackground = true, name = "스텝 타이틀 카드")
@Composable
private fun CreateStepTitleCardPreview() {
    YeobaekTheme {
        CreateStepTitleCard(
            step = "01",
            title = "모임의 이름을 정해주세요"
        )
    }
}
