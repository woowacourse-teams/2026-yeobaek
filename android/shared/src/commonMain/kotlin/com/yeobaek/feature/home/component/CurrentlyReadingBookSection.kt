package com.yeobaek.feature.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.feature.home.model.CurrentlyReadingBookUiModel

@Composable
fun CurrentlyReadingBookSection(
    bookUiModel: CurrentlyReadingBookUiModel,
    navigateToReader: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().clickable {
            navigateToReader(bookUiModel.clubId)
        },
    ) {
        SectionTitle(title = "읽고 있는 책")
        Spacer(modifier = Modifier.height(20.dp))
        CurrentlyReadingBookItem(bookUiModel = bookUiModel)
    }
}

@Preview(showBackground = true, name = "읽고 있는 책 섹션")
@Composable
private fun CurrentlyReadingBookSectionPreview() {
    YeobaekTheme {
        CurrentlyReadingBookSection(
            bookUiModel = CurrentlyReadingBookUiModel(
                groupName = "고전 읽는 오후 모임",
                title = "데미안",
                coverImageUrl =
                    "https://minumsa.minumsa.com/wp-content/uploads/bookcover/" +
                        "044_%EB%8D%B0%EB%AF%B8%EC%95%88-500x840.jpg",
                authors = "헤르만 헤세",
                progressRate = 12,
                clubId = 0,
            ),
            navigateToReader = {},
        )
    }
}
