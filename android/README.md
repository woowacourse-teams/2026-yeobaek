This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### Pull request CI

Pull requests targeting `develop` run Android and iOS verification with non-sensitive CI
configuration. Android verification builds the Debug APK, runs Android host tests, Android Lint,
and ktlint. iOS verification compiles the iOS test sources and builds the iOS Simulator app without
code signing. Gradle, Kotlin/Native, and Swift Package caches reduce repeated build time. Common
tests execute on the Android host because standalone Kotlin/Native tests require separate Firebase
framework linkage that the app's Swift Package setup does not provide.

Each verification category has its own named workflow step. Start with the first failed step,
then inspect its plain Gradle output and stack trace. Test, Android Lint, and ktlint reports are
uploaded as the `android-ci-reports` artifact even when verification fails.

To prevent a failed verification from being merged, configure the `develop` branch protection
rule to require the `Android PR verification` and `iOS PR verification` status checks.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
