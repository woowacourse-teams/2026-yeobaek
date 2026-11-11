package com.yeobaek.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun YeobaekTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    transformInput: (String) -> String = { it },
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        letterSpacing = 1.2.sp,
    ),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: Shape = MaterialTheme.shapes.small,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(transformInput(input))
        },
        textStyle = textStyle,
        placeholder = {
            Text(
                text = placeholder,
                style = textStyle.copy(fontWeight = FontWeight.Normal),
            )
        },
        enabled = enabled,
        leadingIcon = leadingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = colors,
        singleLine = true,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Preview(showBackground = true, name = "텍스트 입력 필드")
@Composable
private fun YeobaekTextFieldPreview() {
    YeobaekTheme {
        YeobaekTextField(
            value = "",
            onValueChange = {},
            placeholder = "예: Book42",
        )
    }
}
