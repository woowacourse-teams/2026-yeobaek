package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderTextSettingMenu(
    expanded: Boolean,
    fontSize: Int,
    onDismissRequest: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(200.dp),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        ) {
            Text(
                text = "글자 크기",
                style = MaterialTheme.typography.labelMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            FontSizeController(
                fontSize = fontSize,
                onFontSizeChange = onFontSizeChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
