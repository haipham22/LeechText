import UIKit
import SwiftUI
import appshared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea() // Compose tự lo insets (statusBarsPadding/navigationBarsPadding) — SwiftUI không offset, tránh double-space
    }
}
