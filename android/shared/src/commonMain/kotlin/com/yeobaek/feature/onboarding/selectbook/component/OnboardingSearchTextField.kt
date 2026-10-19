package com.yeobaek.feature.onboarding.selectbook.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yeobaek.core.designsystem.component.YeobaekTextField
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingSearchTextField(
    searchTextValue: String,
    updateSearchTextValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    YeobaekTextField(
        value = searchTextValue,
        onValueChange = { value ->
            updateSearchTextValue(value)
        },
        placeholder = "책 제목을 검색해 주세요",
        modifier = modifier,
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_search),
                contentDescription = "검색 아이콘",
            )
        },
    )
}
