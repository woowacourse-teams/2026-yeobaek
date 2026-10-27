import FirebaseRemoteConfig
import Foundation
import Shared

final class IOSFirebaseUpdatePolicySource: NSObject, UpdatePolicySource {
    private let remoteConfig: RemoteConfig
    private let platform = UpdatePolicyPlatform.ios

    init(isDebug: Bool) {
        remoteConfig = RemoteConfig.remoteConfig()
        super.init()

        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = isDebug ? 0 : 3_600
        settings.fetchTimeout = 10
        remoteConfig.configSettings = settings
        remoteConfig.setDefaults([
            platform.minimumVersionKey: NSNumber(value: 0),
            UpdatePolicy.companion.FORCE_UPDATE_MESSAGE_KEY:
                UpdatePolicy.companion.DEFAULT_FORCE_UPDATE_MESSAGE as NSString
        ])
    }

    func fetch(completionHandler: @escaping (UpdatePolicy?, Error?) -> Void) {
        remoteConfig.fetchAndActivate { [remoteConfig, platform] status, error in
            if let error {
                completionHandler(nil, error)
                return
            }
            guard status != .error else {
                completionHandler(nil, PolicyError.fetchFailed)
                return
            }

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
    }

    private enum PolicyError: Error {
        case fetchFailed
        case missingParameters
    }
}
