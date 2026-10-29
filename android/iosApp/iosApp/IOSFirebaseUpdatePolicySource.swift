import FirebaseRemoteConfig
import Foundation
import Shared

final class IOSFirebaseUpdatePolicySource: NSObject, IOSUpdatePolicySource {
    private let remoteConfig: RemoteConfig
    private let platform = UpdatePolicyPlatform.ios
    private var registration: ConfigUpdateListenerRegistration?

    init(isDebug: Bool) {
        remoteConfig = RemoteConfig.remoteConfig()
        super.init()

        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = isDebug ? 0 : 7_200
        settings.fetchTimeout = 10
        remoteConfig.configSettings = settings
        remoteConfig.setDefaults([
            platform.minimumVersionKey: NSNumber(value: 0),
            UpdatePolicy.companion.FORCE_UPDATE_MESSAGE_KEY:
                UpdatePolicy.companion.DEFAULT_FORCE_UPDATE_MESSAGE as NSString
        ])
    }

    func fetch(completionHandler_ completionHandler: @escaping (UpdatePolicy?, Error?) -> Void) {
        remoteConfig.fetchAndActivate { [remoteConfig, platform] status, error in
            if let error {
                completionHandler(nil, error)
                return
            }
            guard status != .error else {
                completionHandler(nil, PolicyError.fetchFailed)
                return
            }

            Self.readPolicy(remoteConfig: remoteConfig, platform: platform, completionHandler: completionHandler)
        }
    }

    func activate(completionHandler: @escaping (UpdatePolicy?, Error?) -> Void) {
        remoteConfig.activate { [remoteConfig, platform] _, error in
            if let error {
                completionHandler(nil, error)
                return
            }
            Self.readPolicy(remoteConfig: remoteConfig, platform: platform, completionHandler: completionHandler)
        }
    }

    func startListening(
        onUpdate: @escaping () -> Void,
        onFailure: @escaping (String) -> Void
    ) {
        stopListening()
        let relevantKeys = [platform.minimumVersionKey, UpdatePolicy.companion.FORCE_UPDATE_MESSAGE_KEY]
        registration = remoteConfig.addOnConfigUpdateListener { update, error in
            if let error {
                onFailure(error.localizedDescription)
                return
            }
            if let update, !update.updatedKeys.isDisjoint(with: relevantKeys) {
                onUpdate()
            }
        }
    }

    func stopListening() {
        registration?.remove()
        registration = nil
    }

    deinit {
        registration?.remove()
    }

    private static func readPolicy(
        remoteConfig: RemoteConfig,
        platform: UpdatePolicyPlatform,
        completionHandler: (UpdatePolicy?, Error?) -> Void
    ) {
        let minimumVersion = remoteConfig.configValue(forKey: platform.minimumVersionKey)
        let message = remoteConfig.configValue(forKey: UpdatePolicy.companion.FORCE_UPDATE_MESSAGE_KEY)
        guard minimumVersion.source == .remote, message.source == .remote else {
            completionHandler(nil, PolicyError.missingParameters)
            return
        }
        do {
            let policy = try UpdatePolicy.companion.fromRemoteValues(
                minimumVersion: minimumVersion.stringValue,
                message: message.stringValue
            )
            completionHandler(policy, nil)
        } catch {
            completionHandler(nil, error)
        }
    }

    private enum PolicyError: Error {
        case fetchFailed
        case missingParameters
    }
}
