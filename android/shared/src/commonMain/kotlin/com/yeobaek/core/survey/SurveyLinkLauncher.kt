package com.yeobaek.core.survey

import com.yeobaek.data.local.SurveyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class SurveyLinkLauncher(
    private val preferences: SurveyPreferences,
    private val scope: CoroutineScope,
) {
    // UI가 재생성돼도 진행 중인 링크 열기 요청을 이어서 관찰합니다. UI 스레드에서 접근합니다.
    private val pendingRequests = mutableMapOf<Pair<Int, String>, Deferred<Boolean>>()

    fun pendingRequest(userId: Int, surveyId: String): Deferred<Boolean>? = pendingRequests[userId to surveyId]

    fun open(
        userId: Int,
        surveyId: String,
        url: String,
        opener: SurveyLinkOpener,
    ): Deferred<Boolean> {
        val key = userId to surveyId
        pendingRequests[key]?.let { return it }
        val request = scope.async(Dispatchers.Main.immediate, start = CoroutineStart.LAZY) {
            try {
                val opened = opener.open(url)
                currentCoroutineContext().ensureActive()
                if (opened) preferences.markLinkOpened(userId, surveyId)
                opened
            } finally {
                pendingRequests.remove(key)
            }
        }
        pendingRequests[key] = request
        request.start()
        return request
    }
}
