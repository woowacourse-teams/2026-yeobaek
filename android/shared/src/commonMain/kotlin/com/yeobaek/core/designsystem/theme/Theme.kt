package com.yeobaek.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val YeobaekLightColorScheme = lightColorScheme(
    // 주요 버튼과 강한 강조
    primary = YeobaekInk,
    onPrimary = Color.White,
    primaryContainer = YeobaekSoft,
    onPrimaryContainer = YeobaekInk,

    // 진행률, 선택 상태, 보조 액션
    secondary = YeobaekAccent,
    onSecondary = Color.White,
    secondaryContainer = YeobaekHighlight,
    onSecondaryContainer = YeobaekInk,

    // 부드러운 보조 UI
    tertiary = YeobaekTextSecondary,
    onTertiary = Color.White,
    tertiaryContainer = YeobaekSoft,
    onTertiaryContainer = YeobaekInk,

    // 전체 화면
    background = YeobaekPaper,
    onBackground = YeobaekTextPrimary,

    // 카드, 입력창, 바텀 시트
    surface = YeobaekSurface,
    onSurface = YeobaekTextPrimary,
    surfaceVariant = YeobaekSoft,
    onSurfaceVariant = YeobaekTextMuted,

    // 테두리와 구분선
    outline = YeobaekOutline,
    outlineVariant = YeobaekLine,

    // 오류 상태
    error = YeobaekError,
    onError = YeobaekOnError,
)

@Composable
fun YeobaekTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = YeobaekLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YeobaekTypography,
        content = content
    )
}