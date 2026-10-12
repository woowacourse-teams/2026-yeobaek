import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    let analyticsClient: AnalyticsClient

    func makeUIViewController(context: Self.Context) -> UIViewController {
        let appVersion = Bundle.main.object(
            forInfoDictionaryKey: "CFBundleShortVersionString"
        ) as? String ?? ""

        #if DEBUG
        return DebugViewControllerKt.DebugViewController(
            appVersion: appVersion,
            analyticsClient: analyticsClient
        )
        #else
        return MainViewControllerKt.MainViewController(
            isDebug: false,
            appVersion: appVersion,
            analyticsClient: analyticsClient
        )
        #endif
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    let analyticsClient: AnalyticsClient

    var body: some View {
        ComposeView(analyticsClient: analyticsClient)
            .ignoresSafeArea()
    }
}
