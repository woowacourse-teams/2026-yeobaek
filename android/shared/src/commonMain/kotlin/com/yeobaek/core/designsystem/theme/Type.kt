package com.yeobaek.core.designsystem.theme

import android.shared.generated.resources.Res
import android.shared.generated.resources.ridibatang
import android.shared.generated.resources.wantedsans_black
import android.shared.generated.resources.wantedsans_bold
import android.shared.generated.resources.wantedsans_extrabold
import android.shared.generated.resources.wantedsans_medium
import android.shared.generated.resources.wantedsans_regular
import android.shared.generated.resources.wantedsans_semibold
import android.shared.generated.resources.yuhan_kimberly_bold
import android.shared.generated.resources.yuhan_kimberly_light
import android.shared.generated.resources.yuhan_kimberly_medium
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

val YeobaekSerif: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.yuhan_kimberly_light, FontWeight.Light),
        Font(Res.font.yuhan_kimberly_medium, FontWeight.Medium),
        Font(Res.font.yuhan_kimberly_bold, FontWeight.Bold),
    )

val YeobaekBatang: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.ridibatang, FontWeight.Normal),
    )

private val YeobaekSans: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.wantedsans_regular, FontWeight.Normal),
        Font(Res.font.wantedsans_medium, FontWeight.Medium),
        Font(Res.font.wantedsans_semibold, FontWeight.SemiBold),
        Font(Res.font.wantedsans_bold, FontWeight.Bold),
        Font(Res.font.wantedsans_extrabold, FontWeight.ExtraBold),
        Font(Res.font.wantedsans_black, FontWeight.Black),
    )

val YeobaekTypography: Typography
    @Composable get() = Typography(
        // 화면의 큰 제목
        // 예: 책 한 권의 여백을 친구와 나눠보세요
        headlineLarge = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            letterSpacing = (-1.1).sp,
        ),

        // 섹션 제목
        // 예: 마지막으로 읽은 책, 내 모임
        headlineMedium = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.7).sp,
        ),

        // 카드·다이얼로그 제목
        // 예: 데미안, 어린 왕자
        titleLarge = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.9).sp,
        ),

        // 작은 제목
        // 예: 모임 이름, 참여한 사용자
        titleMedium = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 25.sp,
            letterSpacing = (-0.5).sp,
        ),

        // 주요 본문
        // 예: 책 뷰어의 독서 문단
        bodyLarge = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.4).sp,
        ),

        // 일반 본문
        // 예: 화면 안내, 모임 설명
        bodyMedium = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        ),

        // 부가 설명
        // 예: 작가명, 페이지 정보, 도움말
        bodySmall = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
        ),

        // 버튼 텍스트
        // 예: 모임 참여하기, 책 이어 읽기
        labelLarge = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        ),

        // 칩·탭·라벨
        // 예: STEP 01, 참여 코드, 독서율
        labelMedium = TextStyle(
            fontFamily = YeobaekSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
    )
