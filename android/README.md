# 여백

같은 책을 읽는 사람들과 문장에 생각을 남기고 나누는 **여백**의 Android 및 iOS 앱입니다.

Kotlin Multiplatform과 Compose Multiplatform으로 화면과 주요 로직을 공유합니다.

## 프로젝트 구조

`android/` 폴더에 Android와 iOS 모바일 프로젝트가 함께 있습니다.

![여백 모바일 모듈 구성도](https://github.com/user-attachments/assets/298b966b-103d-4122-ab7f-4744ef1f4da9)

| 항목 | 역할 |
| :--- | :--- |
| [`:androidApp`](androidApp/) | Android 앱을 시작하고 플랫폼 SDK를 연동합니다. |
| [`iosApp`](iosApp/) | Xcode로 빌드하는 iOS 앱 프로젝트입니다. Swift에서 공통 Compose 화면을 띄웁니다. |
| [`:shared`](shared/) | Android와 iOS가 공유하는 화면과 로직을 담습니다. |
| [`:devtools`](devtools/) | 두 플랫폼의 Debug 빌드에 개발자 도구를 제공합니다. |
| [`gradle/`](gradle/) | 의존성 버전과 Gradle Wrapper 설정을 관리합니다. |

### 공통 코드 구성

| 항목 | 역할 |
| :--- | :--- |
| [`commonMain`](shared/src/commonMain/) | Android와 iOS에서 함께 실행하는 코드입니다. |
| [`feature`](shared/src/commonMain/kotlin/com/yeobaek/feature/) | 기능별 Compose 화면과 ViewModel을 담습니다. |
| [`data`](shared/src/commonMain/kotlin/com/yeobaek/data/) | API, Repository, 로컬 설정을 관리합니다. |
| [`core`](shared/src/commonMain/kotlin/com/yeobaek/core/) | `AppContainer`, 네트워크, 디자인 시스템 등 여러 기능에서 쓰는 코드를 담습니다. |
| [`androidMain`](shared/src/androidMain/) | Android 전용 Kotlin 구현을 담습니다. |
| [`iosMain`](shared/src/iosMain/) | iOS 전용 Kotlin 구현을 담습니다. |

공통 리소스는 [`composeResources/`](shared/src/commonMain/composeResources/)에서 관리합니다.

## 실행 준비

| 항목 | 기준 |
| :--- | :--- |
| JDK | 21 |
| Android | Android Studio, SDK Platform 36, 실행 기기 API 24 이상 |
| iOS | macOS와 Xcode, iOS 18.2 이상, ARM64 시뮬레이터 |

의존성과 플러그인 버전은 [gradle/libs.versions.toml](gradle/libs.versions.toml)에서 관리합니다.

### API 주소

`android/local.properties`에 팀에서 공유받은 서버 주소를 입력합니다. Android Studio가 생성한 `sdk.dir`은 유지합니다.

```properties
BASE_URL=https://production.example.invalid/
TEST_BASE_URL=https://development.example.invalid/
```

위 주소는 예시입니다. `BASE_URL`은 Release, `TEST_BASE_URL`은 Debug에서 사용합니다. PostHog 설정은 선택 사항이며 API 키가 없으면 분석 수집을 비활성화합니다.

### Firebase

팀에서 공유받은 설정 파일을 다음 위치에 배치합니다. 경로는 `android/` 기준입니다.

| 환경 | 설정 파일 |
| :--- | :--- |
| Android Debug | `androidApp/src/debug/google-services.json` |
| Android Release | `androidApp/google-services.json` |
| iOS Debug | `iosApp/Configuration/firebase/debug/GoogleService-Info.plist` |
| iOS Release | `iosApp/Configuration/firebase/release/GoogleService-Info.plist` |

Android 개발에는 Debug 파일을 준비합니다. iOS는 Xcode 빌드 입력에 두 경로가 등록되어 있어 Debug와 Release 파일이 모두 필요합니다. 위 설정 파일과 `local.properties`는 Git에서 제외되어 있습니다.

## 실행 방법

아래 명령은 모두 저장소의 `android/` 폴더에서 실행합니다. Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다.

### Android

Android Studio에서 `android/` 폴더를 열고 Gradle JVM을 JDK 21로 설정합니다. `androidApp` 모듈의 `debug` variant와 실행할 기기를 선택한 뒤 Run을 실행합니다.

Debug APK 빌드:

```bash
./gradlew :androidApp:assembleDebug
```

APK는 `androidApp/build/outputs/apk/debug/`에 생성됩니다. Debug 앱은 **조밀**이라는 이름으로 표시됩니다.

### iOS

[iosApp/iosApp.xcodeproj](iosApp/iosApp.xcodeproj/)를 Xcode에서 열고, `iosApp` scheme과 ARM64 시뮬레이터를 선택해 실행합니다. 실제 기기에서 실행할 때는 Signing 설정도 필요합니다.

공통 프레임워크는 Xcode 빌드 단계에서 자동으로 준비합니다. Debug는 `devtools`, Release는 `shared` 모듈을 사용합니다.

## 테스트와 코드 검사

```bash
# 공통 테스트와 Android 호스트 테스트
./gradlew :shared:testAndroidHostTest

# Android Lint
./gradlew :androidApp:lintDebug

# Kotlin 코드 스타일 검사
./gradlew :shared:ktlintCheck
```

macOS에서 iOS 테스트 소스를 컴파일하려면 다음 명령을 실행합니다. 테스트 실행은 포함하지 않습니다.

```bash
./gradlew :shared:compileTestKotlinIosSimulatorArm64
```
