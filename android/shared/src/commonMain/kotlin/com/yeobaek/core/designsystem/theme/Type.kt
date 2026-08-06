package com.yeobaek.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val YeobaekSerif = FontFamily.Serif
private val YeobaekSans = FontFamily.SansSerif

val YeobaekTypography = Typography(
    // 화면의 큰 제목
    // 예: 책 한 권의 여백을 친구와 나눠보세요
    headlineLarge = TextStyle(
        fontFamily = YeobaekSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.1).sp,
    ),

    // 섹션 제목
    // 예: 마지막으로 읽은 책, 내 모임
    headlineMedium = TextStyle(
        fontFamily = YeobaekSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.7).sp,
    ),

    // 카드·다이얼로그 제목
    // 예: 데미안, 어린 왕자
    titleLarge = TextStyle(
        fontFamily = YeobaekSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.9).sp,
    ),

    // 작은 제목
    // 예: 모임 이름, 참여한 사용자
    titleMedium = TextStyle(
        fontFamily = YeobaekSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.5).sp,
    ),

    // 주요 본문
    // 예: 책 뷰어의 독서 문단
    bodyLarge = TextStyle(
        fontFamily = YeobaekSerif,
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
