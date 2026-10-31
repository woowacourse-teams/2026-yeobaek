import SwiftUI
import UIKit
import FirebaseCore
import FirebaseMessaging
import Shared
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate {
    lazy var analyticsClient = IOSPostHogAnalyticsClient()
    lazy var appContainer = AppContainer(
        isDebug: isDebug,
        appVersion: Bundle.main.object(
            forInfoDictionaryKey: "CFBundleShortVersionString"
        ) as? String ?? "",
        appVersionCode: Int32(
            Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""
        ) ?? 0,
        installationId: IOSInstallationIdStore.shared.getOrCreate(),
        analyticsClient: analyticsClient,
        updatePolicySource: IOSRealtimeUpdatePolicySource(
            source: IOSFirebaseUpdatePolicySource(isDebug: isDebug)
        ),
        updatePolicyPlatform: .ios
    )

    private var isDebug: Bool {
        #if DEBUG
        true
        #else
        false
        #endif
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions:
            [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        appContainer.startUpdatePolicyUpdates()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        requestNotificationPermissionOnce()
        application.registerForRemoteNotifications()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
        Messaging.messaging().register { error in
            if let error {
                NSLog("FCM registration failed: %@", error.localizedDescription)
            }
        }
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NSLog("APNs registration failed: %@", error.localizedDescription)
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

extension AppDelegate: MessagingDelegate {
    func messaging(
        _ messaging: Messaging,
        didReceiveRegistration installationId: String?
    ) {
        if let installationId {
            appContainer.updateFirebaseInstallationId(installationId: installationId)
        }
        #if DEBUG
        if let installationId {
            NSLog("FCM registered installation ID: %@", installationId)
        }
        #endif
    }
}

extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if response.actionIdentifier == UNNotificationDefaultActionIdentifier {
            let data = notificationData(response.notification)
            appContainer.onNotificationOpened(data: data)
        }
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler:
            @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        #if DEBUG
        NSLog("FCM notification received: %@", notification.request.identifier)
        #endif
        let presentation = appContainer.handleReceivedNotification(
            data: notificationData(notification)
        )
        completionHandler(presentation == .show ? [.banner, .list, .sound] : [])
    }

    private func notificationData(_ notification: UNNotification) -> [String: String] {
        notification.request.content.userInfo.reduce(into: [String: String]()) { result, entry in
            if let key = entry.key as? String, let value = entry.value as? String {
                result[key] = value
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
            ContentView(appContainer: appDelegate.appContainer)
        }
    }
}
