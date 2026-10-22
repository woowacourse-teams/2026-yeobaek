import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    let appContainer: AppContainer

    func makeUIViewController(context: Self.Context) -> UIViewController {
        #if DEBUG
        return DebugViewControllerKt.DebugViewController(
            appContainer: appContainer
        )
        #else
        return MainViewControllerKt.MainViewController(
            appContainer: appContainer
        )
        #endif
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    let appContainer: AppContainer

    var body: some View {
        ComposeView(appContainer: appContainer)
            .ignoresSafeArea()
    }
}
