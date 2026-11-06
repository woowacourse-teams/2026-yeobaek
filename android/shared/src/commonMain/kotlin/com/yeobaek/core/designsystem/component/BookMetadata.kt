package com.yeobaek.core.designsystem.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.book_genre_label
import android.shared.generated.resources.book_keywords_title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekHighlight
import com.yeobaek.core.designsystem.theme.YeobaekSoft
import com.yeobaek.core.designsystem.theme.YeobaekTextMuted
import com.yeobaek.core.designsystem.theme.YeobaekTextPrimary
import com.yeobaek.core.designsystem.theme.YeobaekTextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookMetadataRow(
    genres: List<String>,
    hashtags: List<String>,
    modifier: Modifier = Modifier,
) {
    val genre = genres.firstOrNull { it.isNotBlank() }?.trim()
    val hashtag = hashtags.displayHashtags().firstOrNull()
    if (genre == null && hashtag == null) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (genre != null) {
            BookGenreBadge(
                genre = genre,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (hashtag != null) {
            Text(
                text = hashtag,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = YeobaekTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BookHashtags(
    hashtags: List<String>,
    modifier: Modifier = Modifier,
) {
    val labels = hashtags.displayHashtags()
    if (labels.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { hashtag ->
            Text(
                text = hashtag,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = YeobaekTextSecondary,
            )
        }
    }
}

@Composable
fun BookGenres(
    genres: List<String>,
    modifier: Modifier = Modifier,
) {
    val labels = genres.map(String::trim).filter(String::isNotEmpty)
    if (labels.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.book_genre_label),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = YeobaekTextMuted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEach { genre -> BookGenreBadge(genre, minWidth = 82.dp) }
        }
    }
}

@Composable
fun BookKeywords(
    hashtags: List<String>,
    modifier: Modifier = Modifier,
) {
    val labels = hashtags.displayHashtags()
    if (labels.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth()
            .background(YeobaekSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.book_keywords_title),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = YeobaekTextPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { hashtag ->
                Box(
                    modifier = Modifier.widthIn(min = 60.dp).heightIn(min = 26.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = hashtag,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = YeobaekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookGenreBadge(
    genre: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = 50.dp,
) {
    Box(
        modifier = modifier.widthIn(min = minWidth).heightIn(min = 24.dp)
            .background(YeobaekHighlight, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = YeobaekTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun List<String>.displayHashtags(): List<String> = mapNotNull { value ->
    value.trim().trimStart('#').trim().takeIf(String::isNotEmpty)?.let { "#$it" }
}
