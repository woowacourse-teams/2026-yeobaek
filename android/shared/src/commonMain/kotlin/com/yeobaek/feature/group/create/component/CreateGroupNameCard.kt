package com.yeobaek.feature.group.create.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun CreateGroupNameCard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        CreateStepTitleCard(
            step = "01",
            title = "모임의 이름을 정해주세요",
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("예: 일요일 아침, 함께 읽기")
            },
            singleLine = true,
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Text(
            "${value.length}/20",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.outline,
            ),
            textAlign = TextAlign.End,
        )
    }
}

@Preview(showBackground = true, name = "그룹 이름 카드")
@Composable
private fun CreateGroupNameCardPreview() {
    YeobaekTheme {
        CreateGroupNameCard(
            value = "",
            onValueChange = {},
        )
    }
}
