package com.yeobaek.feature.home.component

import android.shared.generated.resources.Res
import android.shared.generated.resources.ic_plus
import android.shared.generated.resources.ic_upper_right_arrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun GroupButtonSection(
    navigateToJoin: () -> Unit,
    navigateToCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        GroupActionButton(
            text = "모임 참여하기",
            iconResource = Res.drawable.ic_upper_right_arrow,
            navigateToRoute = navigateToJoin,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        GroupActionButton(
            text = "새 모임 만들기",
            iconResource = Res.drawable.ic_plus,
            navigateToRoute = navigateToCreate,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GroupActionButton(
    text: String,
    iconResource: DrawableResource,
    navigateToRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = navigateToRoute,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(vertical = 16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.outlineVariant,
            contentColor = MaterialTheme.colorScheme.secondary,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = "$text 아이콘",
            )
            Text(text)
        }
    }
}

@Preview(showBackground = true, name = "모임 버튼 영역")
@Composable
private fun GroupButtonSectionPreview() {
    YeobaekTheme {
        GroupButtonSection(
            navigateToJoin = {},
            navigateToCreate = {},
        )
    }
}
