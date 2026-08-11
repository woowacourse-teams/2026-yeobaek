package com.yeobaek.feature.reader.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun ReaderProgressBar(
    progress: Float,
    onProgressChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderProgress by remember(progress) {
        mutableFloatStateOf(progress.coerceIn(0f, 100f))
    }
    val interactionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.secondary,
        activeTrackColor = MaterialTheme.colorScheme.secondary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 4.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = sliderProgress,
                onValueChange = { newProgress ->
                    sliderProgress = newProgress
                },
                onValueChangeFinished = {
                    onProgressChangeFinished(sliderProgress)
                },
                valueRange = 0f..100f,
                colors = sliderColors,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .size(16.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                            ),
                        colors = sliderColors,
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(4.dp),
                        colors = sliderColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                    )
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${sliderProgress.toInt()}%",
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.4.sp,
                ),
                color = YeobaekTextSecondary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Preview(showBackground = true, name = "읽기 진행률")
@Composable
private fun ReaderProgressBarPreview() {
    YeobaekTheme {
        ReaderProgressBar(
            progress = 12f,
            onProgressChangeFinished = {},
        )
    }
}
