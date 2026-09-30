package com.yeobaek.feature.onboarding.create.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.component.BookCoverImage
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
fun SelectBookCard(
    title: String,
    authors: String,
    coverUrl: String?,
    selectOtherBook: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text("선택한 책", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors().copy(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary),
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth().height(100.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                BookCoverImage(
                    imageUrl = coverUrl,
                    modifier = Modifier.fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(authors, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(
                    onClick = selectOtherBook,
                    enabled = enabled,
                ) {
                    Text("다른 책 고르기", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "선택된 책 카드")
@Composable
private fun SelectBookCardPreview() {
    YeobaekTheme {
        SelectBookCard(
            title = "The Great Gatsby",
            authors = "F. Scott Fitzgerald",
            coverUrl = null,
            selectOtherBook = {},
        )
    }
}
