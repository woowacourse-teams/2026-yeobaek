package com.yeobaek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTextPrimary

@Composable
fun BookListCover(
    imageUrl: String?,
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    val genre = genres.firstOrNull(String::isNotBlank)?.trim()
    Box(modifier = modifier) {
        BookCoverImage(imageUrl = imageUrl, modifier = Modifier.fillMaxWidth())
        if (genre != null) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Text(
                    text = genre,
                    modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = YeobaekTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
