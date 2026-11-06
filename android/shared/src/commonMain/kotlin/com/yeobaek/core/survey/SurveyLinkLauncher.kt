package com.yeobaek.core.survey

import com.yeobaek.data.local.SurveyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class SurveyLinkLauncher(
    private val preferences: SurveyPreferences,
    private val scope: CoroutineScope,
) {
    fun open(
        userId: Int,
        surveyId: String,
        url: String,
        opener: SurveyLinkOpener,
    ): Deferred<Boolean> = scope.async(Dispatchers.Main.immediate) {
        val opened = opener.open(url)
        currentCoroutineContext().ensureActive()
        if (opened) preferences.markLinkOpened(userId, surveyId)
        opened
    }
}
