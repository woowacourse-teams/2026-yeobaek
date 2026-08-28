package com.yeobaek.core.designsystem.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.book_image_not_found
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun BookCoverImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "책 표지",
            modifier = modifier
                .clip(shape = MaterialTheme.shapes.extraSmall)
                .aspectRatio(9f / 16f),
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(Res.drawable.book_image_not_found),
            contentDescription = "책 표지 불러오기 실패",
            modifier = modifier
                .clip(shape = MaterialTheme.shapes.extraSmall)
                .aspectRatio(9f / 16f),
            contentScale = ContentScale.Crop,
        )
    }
}

@Preview(showBackground = true, name = "책 표지 불러오기 실패")
@Composable
private fun BookCoverImagePreview() {
    YeobaekTheme {
        BookCoverImage(imageUrl = null)
    }
}
