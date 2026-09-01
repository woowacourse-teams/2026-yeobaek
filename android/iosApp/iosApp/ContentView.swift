import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    let analyticsClient: AnalyticsClient

    func makeUIViewController(context: Self.Context) -> UIViewController {
        #if DEBUG
        let isDebug = true
        #else
        let isDebug = false
        #endif

        return MainViewControllerKt.MainViewController(
            isDebug: isDebug,
            appVersion: Bundle.main.object(
                forInfoDictionaryKey: "CFBundleShortVersionString"
            ) as? String ?? "",
            analyticsClient: analyticsClient
        )
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
