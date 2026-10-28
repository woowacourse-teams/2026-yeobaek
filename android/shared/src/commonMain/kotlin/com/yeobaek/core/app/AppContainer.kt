package com.yeobaek.core.app

import com.russhwolf.settings.Settings
import com.yeobaek.BuildKonfig
import com.yeobaek.core.analytics.AnalyticsClient
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.NoOpAnalyticsClient
import com.yeobaek.core.crashlytics.FirebaseCrashSink
import com.yeobaek.core.network.ApiProvider
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.network.NetworkProvider
import com.yeobaek.core.notification.CommentNotifications
import com.yeobaek.core.notification.NotificationPresentation
import com.yeobaek.core.notification.NotificationRegistrationCoordinator
import com.yeobaek.core.update.UpdatePolicyPlatform
import com.yeobaek.core.update.UpdatePolicyRepository
import com.yeobaek.core.update.UpdatePolicySource
import com.yeobaek.core.update.requiredUpdateState
import com.yeobaek.data.local.GuideOnboardingPreferences
import com.yeobaek.data.local.NotificationRegistrationPreferences
import com.yeobaek.data.local.ReaderPreferences
import com.yeobaek.data.local.UpdatePolicyPreferences
import com.yeobaek.data.local.UserPreferences
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.data.repository.ReaderRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.data.repositoryImpl.remote.BookRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.CommentRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.GroupRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.NotificationRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.PublicRoomRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.ReaderRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.UserRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppContainer(
    private val isDebug: Boolean,
    val appVersion: String,
    val appVersionCode: Int,
    analyticsClient: AnalyticsClient = NoOpAnalyticsClient,
    updatePolicySource: UpdatePolicySource? = null,
    updatePolicyPlatform: UpdatePolicyPlatform = UpdatePolicyPlatform.ANDROID,
) {
    private val settings = Settings()

    val userPreferences = UserPreferences(settings)
    val readerPreferences = ReaderPreferences(settings)
    val guideOnboardingPreferences = GuideOnboardingPreferences(settings)

    val analyticsTracker = AnalyticsTracker(analyticsClient)

    val crashReporter = CrashReporter(isDebug = isDebug)

    val appName = if (isDebug) "조밀" else "여백"

    private val networkProvider = NetworkProvider(
        userPreferences = userPreferences,
        isDebug = isDebug,
    )

    private val apiProvider = ApiProvider(
        ktorfit = networkProvider.ktorfit,
    )

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val updatePolicyRepository = UpdatePolicyRepository(
        preferences = UpdatePolicyPreferences(settings, updatePolicyPlatform),
        source = updatePolicySource,
        onFailure = { exception ->
            FirebaseCrashSink.log("Update policy refresh failed: ${exception::class.simpleName}")
        },
    )
    val isUpdateRequired = updatePolicyRepository.policy.requiredUpdateState(
        currentVersionCode = appVersionCode,
        scope = applicationScope,
    )
    val commentNotifications = CommentNotifications()
    private val notificationRegistration = NotificationRegistrationCoordinator(
        memberId = userPreferences.userId,
        preferences = NotificationRegistrationPreferences(settings),
        repository = NotificationRepositoryImpl(apiProvider.notificationApi),
        server = networkProvider.baseUrl,
        scope = applicationScope,
        onFailure = { exception ->
            FirebaseCrashSink.log("Notification registration failed: ${exception::class.simpleName}")
        },
    )

    val userRepository: UserRepository = UserRepositoryImpl(
        userApi = apiProvider.userApi,
        userPreferences = userPreferences,
    )
    val bookRepository: BookRepository = BookRepositoryImpl(
        bookApi = apiProvider.bookApi,
    )
    val commentRepository: CommentRepository = CommentRepositoryImpl(
        commentApi = apiProvider.commentApi,
    )
    val groupRepository: GroupRepository = GroupRepositoryImpl(
        clubApi = apiProvider.clubApi,
    )
    val publicRoomRepository: PublicRoomRepository = PublicRoomRepositoryImpl(
        publicRoomApi = apiProvider.publicRoomApi,
    )
    val readerRepository: ReaderRepository = ReaderRepositoryImpl(
        readerApi = apiProvider.readerApi,
    )

    val posthogApiKey = if (isDebug) BuildKonfig.POSTHOG_DEBUG_API_KEY else BuildKonfig.POSTHOG_RELEASE_API_KEY

    val apiBaseUrl = networkProvider.baseUrl

    init {
        analyticsClient.setup(
            apiKey = posthogApiKey,
            host = BuildKonfig.POSTHOG_HOST,
            isDebug = isDebug,
        )
        analyticsClient.register(
            mapOf(ENVIRONMENT_PROPERTY to if (isDebug) ENVIRONMENT_DEVELOPMENT else ENVIRONMENT_PRODUCTION),
        )
        userPreferences.getUserId()?.let(analyticsTracker::identify)
    }

    fun updateFirebaseInstallationId(installationId: String) {
        notificationRegistration.updateInstallationId(installationId)
    }

    fun refreshUpdatePolicy() {
        applicationScope.launch {
            updatePolicyRepository.refresh()
        }
    }

    fun handleReceivedNotification(data: Map<String, String>): NotificationPresentation =
        commentNotifications.handleReceived(data)

    fun onNotificationOpened(data: Map<String, String>) {
        commentNotifications.onOpened(data)
    }

    fun close() {
        applicationScope.cancel()
        networkProvider.close()
    }

    companion object {
        private const val ENVIRONMENT_PROPERTY = "environment"
        private const val ENVIRONMENT_DEVELOPMENT = "development"
        private const val ENVIRONMENT_PRODUCTION = "production"
    }
}
