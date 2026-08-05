package com.yeobaek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun YeobaekButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YeobaekButtonPreview() {
    YeobaekTheme {
        YeobaekButton(
            text = "모임 참여하기",
            onClick = {},
        )
    }
}