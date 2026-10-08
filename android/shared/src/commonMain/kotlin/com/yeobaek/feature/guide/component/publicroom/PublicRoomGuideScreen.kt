package com.yeobaek.feature.guide.component.publicroom

import android.shared.generated.resources.Res
import android.shared.generated.resources.img_public_room_guid
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun PublicRoomGuideScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            "공개방에서 같은 책을 읽는\n사람들을 만나보세요",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 30.sp,
                lineHeight = 40.sp,
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "공개방은 같은 책을 고른 누구나 참여해\n문장과 생각을  나누는 열린 독서 공간이에요.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = Color(0xFF7D746B),
            ),
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.img_public_room_guid),
                contentDescription = "공개방 가이드 사진",
            )
        }
    }
}

@Preview(showBackground = true, name = "공개방 가이드 화면")
@Composable
private fun PublicRoomGuideScreenPreview() {
    YeobaekTheme {
        PublicRoomGuideScreen()
    }
}
