package com.yeobaek

import com.yeobaek.core.config.ConfigFetchInfo
import com.yeobaek.core.config.ConfigFetchStatus
import com.yeobaek.core.config.ConfigParameterValue
import com.yeobaek.core.config.ConfigUpdateEvent
import com.yeobaek.core.config.ConfigValueSource
import com.yeobaek.core.config.RemoteConfigDataSource
import com.yeobaek.core.config.RemoteConfigKeys
import com.yeobaek.core.config.SurveyConfig
import com.yeobaek.data.repositoryImpl.remote.AppConfigRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class SurveyConfigTest {
    @Test
    fun defaultsKeepSurveyDisabledAndPreserveRequestedCopy() = runTest {
        val source = FakeRemoteConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)

        assertEquals(SurveyConfig.DEFAULT, repository.state.value.surveyConfig)
        assertTrue(repository.initialize().isSuccess)
        assertFalse(repository.state.value.surveyConfig.enabled)
        val defaultJson = source.defaults.getValue(RemoteConfigKeys.PMF_SURVEY).toString()
        assertEquals(SurveyConfig.DEFAULT, Json.decodeFromString<SurveyConfig>(defaultJson))
        assertEquals("여백을 사용해 보니 어떠셨나요?", repository.state.value.surveyConfig.title)
        assertEquals(
            "좋았던 점도, 아쉬웠던 점도 들려주세요. 여러분의 소중한 의견을 바탕으로 앞으로 더 좋은 서비스를 만들어 가겠습니다 💛",
            repository.state.value.surveyConfig.description,
        )
        assertEquals(ConfigValueSource.DEFAULT, repository.state.value.valueSources[RemoteConfigKeys.PMF_SURVEY])
    }

    @Test
    fun initializationReadsPreviouslyActivatedSurvey() = runTest {
        val source = FakeRemoteConfigDataSource()
        val survey = SurveyConfig.DEFAULT.copy(enabled = true)
        source.surveyValue = Json.encodeToString(survey)
        val repository = AppConfigRepositoryImpl(source)

        assertTrue(repository.initialize().isSuccess)
        assertEquals(survey, repository.state.value.surveyConfig)
        assertEquals(ConfigValueSource.REMOTE, repository.state.value.valueSources[RemoteConfigKeys.PMF_SURVEY])
    }

    @Test
    fun fetchedSurveyChangesOnlyWhenActivated() = runTest {
        val source = FakeRemoteConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)
        repository.initialize()
        val survey = SurveyConfig.DEFAULT.copy(id = "pmf-002", enabled = true, title = "새로운 설문")
        source.surveyValue = Json.encodeToString(survey)

        assertTrue(repository.fetch().isSuccess)
        assertEquals(SurveyConfig.DEFAULT, repository.state.value.surveyConfig)
        assertTrue(repository.activate().isSuccess)
        assertEquals(survey, repository.state.value.surveyConfig)

        source.surveyValue = Json.encodeToString(survey.copy(enabled = false))
        assertTrue(repository.activate().isSuccess)
        assertFalse(repository.state.value.surveyConfig.enabled)
    }

    @Test
    fun invalidSurveyDisablesPreviouslyEnabledSurveyWithoutBreakingOtherFlags() = runTest {
        val source = FakeRemoteConfigDataSource()
        val survey = SurveyConfig.DEFAULT.copy(enabled = true)
        source.surveyValue = Json.encodeToString(survey)
        val repository = AppConfigRepositoryImpl(source)
        repository.initialize()
        assertTrue(repository.state.value.surveyConfig.enabled)

        val invalidValues = listOf(
            "not json",
            "{}",
            """{"enabled":true}""",
            Json.encodeToString(survey.copy(id = " ")),
            Json.encodeToString(survey.copy(title = "")),
            Json.encodeToString(survey.copy(description = "")),
            Json.encodeToString(survey.copy(url = "")),
            Json.encodeToString(survey.copy(url = "javascript:alert(1)")),
        )
        for (value in invalidValues) {
            source.surveyValue = value
            assertTrue(repository.activate().isSuccess)
            assertEquals(SurveyConfig.DEFAULT, repository.state.value.surveyConfig)
            assertEquals(ConfigValueSource.DEFAULT, repository.state.value.valueSources[RemoteConfigKeys.PMF_SURVEY])
            assertTrue(repository.state.value.featureFlags.commentLikeEnabled)
        }
    }

    @Test
    fun realTimeUpdateAppliesSurveySwitch() = runTest {
        val source = FakeRemoteConfigDataSource()
        val repository = AppConfigRepositoryImpl(source)
        backgroundScope.launch { repository.observeUpdates() }
        runCurrent()

        source.surveyValue = Json.encodeToString(SurveyConfig.DEFAULT.copy(enabled = true))
        source.events.emit(ConfigUpdateEvent.Updated)
        runCurrent()
        assertTrue(repository.state.value.surveyConfig.enabled)

        source.surveyValue = Json.encodeToString(SurveyConfig.DEFAULT)
        source.events.emit(ConfigUpdateEvent.Updated)
        runCurrent()
        assertFalse(repository.state.value.surveyConfig.enabled)
    }

    @Test
    fun initializationFailureLeavesSurveyDisabled() = runTest {
        val source = FakeRemoteConfigDataSource().apply { failInitialization = true }
        val repository = AppConfigRepositoryImpl(source)

        assertTrue(repository.initialize().isFailure)
        assertEquals(SurveyConfig.DEFAULT, repository.state.value.surveyConfig)
    }

    private class FakeRemoteConfigDataSource : RemoteConfigDataSource {
        var defaults: Map<String, Any> = emptyMap()
        var surveyValue: String? = null
        var failInitialization = false
        val events = MutableSharedFlow<ConfigUpdateEvent>()

        override suspend fun initialize(defaults: Map<String, Any>) {
            if (failInitialization) error("설정 초기화 실패")
            this.defaults = defaults
        }

        override suspend fun fetch() = Unit

        override suspend fun activate(): Boolean = true

        override fun getValue(key: String): ConfigParameterValue {
            val survey = surveyValue
            return if (key == RemoteConfigKeys.PMF_SURVEY && survey != null) {
                ConfigParameterValue(survey, ConfigValueSource.REMOTE)
            } else {
                ConfigParameterValue(defaults.getValue(key).toString(), ConfigValueSource.DEFAULT)
            }
        }

        override fun getFetchInfo(): ConfigFetchInfo = ConfigFetchInfo(null, ConfigFetchStatus.NOT_FETCHED)

        override fun updates(): Flow<ConfigUpdateEvent> = events
    }
}
