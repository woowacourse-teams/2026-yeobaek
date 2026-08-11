package com.yeobaek.feature.reader.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import kotlin.math.roundToInt

@Composable
fun FontSizeController(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    fontSizes: List<Int> = listOf(14, 16, 18, 20, 22, 24, 26),
) {
    Slider(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue.roundToInt().toFloat())
        },
        valueRange = 0f..fontSizes.lastIndex.toFloat(),
        steps = fontSizes.size - 2,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(4.dp),
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 2.dp,
            )
        },
    )
}

@Preview(showBackground = true, name = "글자 크기 조절 슬라이더")
@Composable
private fun FontSizeControllerPreview() {
    YeobaekTheme {
        FontSizeController(
            value = 0f,
            onValueChange = {},
        )
    }
}
