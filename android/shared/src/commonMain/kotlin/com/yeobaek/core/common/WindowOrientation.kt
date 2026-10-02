package com.yeobaek.core.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
fun isLandscape(): Boolean {
    val windowSize = LocalWindowInfo.current.containerSize
    return windowSize.width > windowSize.height
}

@Composable
fun getGridCount(): Int = if (isLandscape()) LANDSCAPE_GRID_COUNT else PORTRAIT_GRID_COUNT

const val LANDSCAPE_GRID_COUNT = 8
const val PORTRAIT_GRID_COUNT = 3
