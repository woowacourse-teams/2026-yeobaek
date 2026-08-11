package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yeobaek.feature.reader.model.ReaderFontSize
import kotlin.math.roundToInt

@Composable
fun ReaderTextSettingMenu(
    expanded: Boolean,
    fontSize: Int,
    onDismissRequest: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSizes: List<Int> = ReaderFontSize.options,
) {
    val selectedIndex = fontSizes
        .indexOf(fontSize)
        .takeIf { it >= 0 }
        ?: fontSizes.indexOf(ReaderFontSize.DEFAULT)

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FontSizeController(
                    value = selectedIndex.toFloat(),
                    onValueChange = { value ->
                        val index = value
                            .roundToInt()
                            .coerceIn(fontSizes.indices)
                        onFontSizeChange(fontSizes[index])
                    },
                    modifier = Modifier.fillMaxWidth(),
                    fontSizes = fontSizes,
                )
            }
        }
    }
}
