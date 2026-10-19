import SwiftUI
import UIKit
import FirebaseCore
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate {
    lazy var analyticsClient = IOSPostHogAnalyticsClient()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions:
            [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        requestNotificationPermissionOnce()
        return true
    }

    private func requestNotificationPermissionOnce() {
        let defaults = UserDefaults.standard
        let requestedKey = "notification_permission_request_attempted"

        guard !defaults.bool(forKey: requestedKey) else { return }

        defaults.set(true, forKey: requestedKey)

        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .sound, .badge]
        ) { _, error in
            if let error = error {
                NSLog(
                    "Notification permission request failed: %@",
                    error.localizedDescription
                )
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView(analyticsClient: appDelegate.analyticsClient)
        }
    }
}
