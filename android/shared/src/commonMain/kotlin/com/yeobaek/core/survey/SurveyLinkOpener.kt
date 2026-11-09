package com.yeobaek.core.survey

import androidx.compose.runtime.Composable

fun interface SurveyLinkOpener {
    suspend fun open(url: String): Boolean
}

@Composable
expect fun rememberSurveyLinkOpener(): SurveyLinkOpener
