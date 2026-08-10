package com.yeobaek.feature.reader.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.component.noRippleClickable
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.reader.model.PassageUiModel
import com.yeobaek.feature.reader.passages

@Composable
fun PassageItem(
    passage: PassageUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = passage.content,
        modifier = modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick),
        style = MaterialTheme.typography.bodyLarge.copy(
            letterSpacing = 1.sp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun PassageItemPreview() {
    YeobaekTheme {
        PassageItem(
            passage = passages[0],
            onClick = {},
        )
    }
}
