package com.yeobaek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

private const val PARTICIPATION_CODE_LENGTH = 6

@Composable
fun YeobaekTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.take(PARTICIPATION_CODE_LENGTH))
        },
        textStyle = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            letterSpacing = 1.2.sp
        ),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp
                )
            )
        },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        singleLine = true,
    )
}

@Preview(showBackground = true, name = "텍스트 입력 필드")
@Composable
private fun YeobaekTextFieldPreview() {
    YeobaekTheme {
        YeobaekTextField(
            value = "",
            onValueChange = {},
            placeholder = "예: Book42"
        )
    }
}