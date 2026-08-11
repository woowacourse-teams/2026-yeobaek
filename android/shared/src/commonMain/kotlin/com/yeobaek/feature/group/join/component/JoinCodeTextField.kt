package com.yeobaek.feature.group.join.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.YeobaekTextField
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun JoinCodeTextField(
    codeValue: String,
    codeState: Boolean,
    onCodeValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            if (codeState) "잘못된 코드입니다. 다시 입력해주세요." else "참여 코드",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (codeState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        YeobaekTextField(
            value = codeValue,
            onValueChange = {
                onCodeValueChange(it)
            },
            placeholder = "예: BOOK42",
            isError = codeState,
        )
    }
}

@Preview(showBackground = true, name = "그룹 참여 화면 참여 코드 입력")
@Composable
private fun JoinCodeTextFieldPreview() {
    YeobaekTheme {
        JoinCodeTextField(
            codeValue = "",
            codeState = false,
            onCodeValueChange = {},
        )
    }
}
