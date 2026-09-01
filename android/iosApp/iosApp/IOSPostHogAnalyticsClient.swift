import Foundation
import PostHog
import Shared

final class IOSPostHogAnalyticsClient: NSObject, AnalyticsClient {
    private var isEnabled = false

    func setup(
        apiKey: String,
        host: String,
        isDebug: Bool
    ) {
        isEnabled = !apiKey.isEmpty
        guard isEnabled else { return }

        let config = PostHogConfig(
            projectToken: apiKey,
            host: host
        )
        config.debug = isDebug
        config.captureApplicationLifecycleEvents = false
        config.captureScreenViews = false
        config.captureElementInteractions = false
        config.rageClickConfig.enabled = false

        PostHogSDK.shared.setup(config)
    }

    func capture(
        eventName: String,
        properties: [String: String]
    ) {
        guard isEnabled else { return }

        PostHogSDK.shared.capture(
            eventName,
            properties: properties
        )
    }

    func screen(screenName: String) {
        guard isEnabled else { return }

        PostHogSDK.shared.screen(screenName)
    }

    func identify(userId: String) {
        guard isEnabled else { return }

        PostHogSDK.shared.identify(userId)
    }
}
