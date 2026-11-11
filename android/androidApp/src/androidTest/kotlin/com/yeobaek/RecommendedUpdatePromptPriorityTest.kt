package com.yeobaek

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.yeobaek.update.RecommendedUpdateController
import com.yeobaek.update.RecommendedUpdatePreferences
import com.yeobaek.update.RecommendedUpdateSession
import java.lang.reflect.Proxy
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendedUpdatePromptPriorityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val launcher = object : ActivityResultLauncher<IntentSenderRequest>() {
        override val contract = ActivityResultContracts.StartIntentSenderForResult()
        override fun launch(input: IntentSenderRequest, options: ActivityOptionsCompat?) = Unit
        override fun unregister() = Unit
    }

    @Test
    fun pendingCheckBlocksOtherPromptsUntilFailureOrFlowResult() = withController { controller, requests ->
        assertTrue(controller.isPromptPending.value)
        instrumentation.runOnMainSync { controller.resume(launcher) }
        assertEquals(1, requests.size)
        assertTrue(controller.isPromptPending.value)
        requests[0].setException(IllegalStateException("조회 실패"))
        instrumentation.waitForIdleSync()
        assertFalse(controller.isPromptPending.value)

        instrumentation.runOnMainSync { controller.resume(launcher) }
        assertTrue(controller.isPromptPending.value)
        instrumentation.runOnMainSync { controller.onResult(Activity.RESULT_CANCELED) }
        assertFalse(controller.isPromptPending.value)
    }

    @Test
    fun staleCheckCannotReleaseNewChecksPromptBlock() = withController { controller, requests ->
        instrumentation.runOnMainSync {
            controller.resume(launcher)
            controller.pause()
            controller.resume(launcher)
        }
        assertEquals(2, requests.size)
        requests[0].setException(IllegalStateException("이전 조회 실패"))
        instrumentation.waitForIdleSync()
        assertTrue(controller.isPromptPending.value)
        requests[1].setException(IllegalStateException("새 조회 실패"))
        instrumentation.waitForIdleSync()
        assertFalse(controller.isPromptPending.value)
    }

    private fun withController(block: (RecommendedUpdateController, List<TaskCompletionSource<AppUpdateInfo>>) -> Unit) {
        val requests = mutableListOf<TaskCompletionSource<AppUpdateInfo>>()
        val manager = Proxy.newProxyInstance(
            AppUpdateManager::class.java.classLoader,
            arrayOf(AppUpdateManager::class.java),
        ) { _, method, _ ->
            if (method.name == "getAppUpdateInfo") {
                TaskCompletionSource<AppUpdateInfo>().also(requests::add).task
            } else {
                null
            }
        } as AppUpdateManager
        val name = "update-priority-test-${UUID.randomUUID()}"
        val context = instrumentation.targetContext
        lateinit var controller: RecommendedUpdateController
        instrumentation.runOnMainSync {
            controller = RecommendedUpdateController(
                manager,
                1,
                RecommendedUpdateSession(
                    RecommendedUpdatePreferences(context.getSharedPreferences(name, Context.MODE_PRIVATE)),
                ),
            )
        }
        try {
            block(controller, requests)
        } finally {
            instrumentation.runOnMainSync { controller.close() }
            context.deleteSharedPreferences(name)
        }
    }
}
