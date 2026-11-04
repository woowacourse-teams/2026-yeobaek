# 모임 초대 공유와 링크 참여

## 구현 범위

모임 상세에서 초대코드를 복사하거나 초대 문구를 시스템 공유창으로 전달할 수 있다. 설치된 앱이 초대 링크를 받으면 기존 모임 참여 화면에 코드를 자동 입력한다. 사용자가 코드를 수정하거나 직접 입력하는 방식도 유지한다.

링크를 여는 것만으로 참여하지 않는다. **모임 참여하기**를 눌렀을 때 기존 참여 API를 호출한다. 참여 전 모임명·책 정보를 조회하는 새 API는 추가하지 않는다. 백엔드 변경, 미설치 사용자 처리, 웹 페이지, 스토어 이동, Android App Links, iOS Universal Links는 이번 구현 범위에 포함하지 않는다.

## 링크 규칙

```text
yeobaek://invite?code=ABC123
```

- 스킴: `yeobaek`
- 호스트: `invite`
- 파라미터: `code` 하나, 영문 대문자·숫자 6자리
- 경로, 포트, 사용자 정보, 추가·중복 파라미터, 프래그먼트, 퍼센트 인코딩은 허용하지 않는다.
- 앱이 생성한 형식과 정확히 일치하는 주소만 받는다. 다른 스킴은 무시하고 잘못된 여백 주소에는 안내를 표시한다. Android의 OS 진입 필터는 `invite` 호스트만 등록한다.

위의 코드는 테스트용 예시다. 링크·공유 문구는 `InviteLink`에서 공통 생성·검증한다. 문구에는 모임명, 초대코드, 링크와 링크가 열리지 않을 때 코드를 직접 입력하는 방법을 넣는다.

## 실행 흐름

1. 모임 상세의 **초대 링크 공유**를 누르면 Android의 Sharesheet 또는 iOS의 `UIActivityViewController`를 연다. 코드 복사는 기존 아이콘으로 제공한다.
2. 외부 앱이 링크를 OS에 전달하면 등록된 여백 앱이 주소를 받는다. 외부 메신저가 커스텀 스킴을 클릭 가능한 링크로 처리하는지는 별도 확인이 필요하다.
3. 플랫폼 수신 코드는 주소를 `AppInvitations`에 전달한다. 공통 코드가 주소를 검증하고 요청 ID와 초대코드를 임시 보관한다.
4. 앱 초기화, 사용자 등록과 필수 안내가 끝나고 내비게이션이 준비될 때까지 기다린다.
5. 기존 모임 참여 화면을 열고 초대코드를 한 번 입력한다. 사용자가 수정한 값은 같은 요청의 재전달이나 화면 상태 복원으로 덮어쓰지 않는다.
6. **모임 참여하기**를 누르면 기존 `GroupRepository.joinGroup`을 호출한다. 형식 검증은 앱이 하고, 코드가 실제로 유효한지와 참여 가능한지는 서버가 최종 판단한다.
7. 참여 성공 시 기존 흐름대로 홈으로 이동한다. 실패하면 기존 입력 오류 상태로 표시하고 다시 입력할 수 있다.

## 상태와 뒤로가기

| 상황 | 처리 |
| --- | --- |
| 앱이 종료된 상태에서 링크 수신 | 초기화 후 홈을 기반으로 참여 화면을 연다. 뒤로가기는 홈으로 이동한다. |
| 앱을 사용하던 중 링크 수신 | 이전 화면 위에 참여 화면을 연다. 뒤로가기는 이전 화면으로 이동한다. |
| 등록·안내가 필요한 사용자 | 요청을 보관하고 등록·안내가 끝나면 참여 화면을 연다. 완료한 등록 화면은 뒤로가기 경로에 남기지 않는다. |
| 참여 화면에서 새 링크 수신 | 진행 중인 참여 요청이 없으면 새 코드로 참여 화면을 교체한다. |
| 참여 API 요청 중 새 링크 수신 | 현재 요청이 끝날 때까지 새 링크를 보관한다. 새 코드로 자동 참여하지 않는다. |
| 같은 Android Intent로 Activity 재생성 | 저장한 요청 ID를 사용해 중복 진입을 막는다. 새 클릭은 별도 요청으로 처리한다. |
| 잘못된 초대 링크 | 안내창을 표시하며 참여 API를 호출하지 않는다. |

요청은 앱 상태 복원에 필요한 작은 임시 상태로 보관한다. 초대코드를 사용자 설정이나 분석 이벤트에 저장하지 않는다. 공유창이 열린 것을 초대 전송 완료나 실제 초대한 사람 수로 간주하지 않는다.

## 코드 위치

| 역할 | 파일 |
| --- | --- |
| URI 생성·검증·공유 문구 | `shared/.../core/invitation/InviteLink.kt` |
| 요청 보관·소비·중복 처리 | `shared/.../core/invitation/AppInvitations.kt` |
| 준비 상태 확인·참여 화면 이동 | `shared/.../InvitationEffects.kt`, `App.kt` |
| 입력 상태 보존·중복 참여 방지 | `shared/.../feature/group/join/JoinInputState.kt`, `JoinViewModel.kt` |
| Android 주소 수신 | `androidApp/.../InvitationIntent.kt`, `MainActivity.kt`, `DebugMainActivity.kt`, 각 manifest |
| iOS 주소 수신 | `iosApp/iosApp/Info.plist`, `ContentView.swift` |
| 플랫폼별 공유창 | `shared/src/androidMain/.../InviteShareSheet.android.kt`, `shared/src/iosMain/.../InviteShareSheet.ios.kt` |
| 상세 UI | `shared/.../feature/group/detail/DetailScreen.kt`, `component/InviteCodeCard.kt` |
| 개발자 도구 | `devtools/.../DevToolsScreen.kt`의 모임 초대 테스트 |

## 직접 실행하기

Debug 앱의 **개발자 도구 → 모임 초대 테스트**에서 테스트 코드를 입력한 뒤 정상 링크, 잘못된 링크, 공유창을 실행할 수 있다. 링크 실행은 OS URL 처리 경로를 사용한다. 코드가 실제 모임에 해당하더라도 참여 버튼을 누르기 전에는 참여 요청을 보내지 않는다.

Android 앱 설치 후 다음 명령으로 암시적 Intent를 실행한다. 기기가 여러 개라면 `adb -s <serial>`을 사용한다.

```sh
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d 'yeobaek://invite?code=ABC123'

adb shell am start -W \
  -a android.intent.action.VIEW \
  -d 'yeobaek://invite?code=BAD'
```

iOS 시뮬레이터에 앱을 설치한 뒤 다음 명령으로 실행한다. 시뮬레이터가 여러 개라면 `booted` 대신 기기 UUID를 지정한다.

```sh
xcrun simctl openurl booted 'yeobaek://invite?code=ABC123'
```

앱 종료 상태, 백그라운드 상태, 실행 중 상태에서 각각 확인한다. 등록이 필요하면 먼저 등록을 완료하고, 이미 등록된 상태라면 입력된 코드와 뒤로가기 목적지를 확인한다. 공유 테스트에서는 공유창을 확인한 뒤 취소하면 된다.

## 자동 검증

```sh
./gradlew :shared:testAndroidHostTest :shared:ktlintCheck \
  :androidApp:lintDebug :shared:compileTestKotlinIosSimulatorArm64

./gradlew :androidApp:connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.yeobaek.InvitationIntentTest,com.yeobaek.InviteShareSheetTest,com.yeobaek.InviteDetailUiTest,com.yeobaek.InvitationNavigationTest'
```

- 공통 테스트: URI 허용·거부 사례, 요청 소비·복원·교체, 입력값 보존·형식 검증·중복 제출 방지.
- Android 기기 테스트 13개: 실제 manifest 해석, Intent 재전달, 네이티브 공유창, 상세 화면의 복사·공유, 참여 내비게이션 7개 시나리오. 참여 내비게이션 테스트는 가짜 저장소를 사용하므로 서버에 모임 참여를 생성하지 않는다.
- 상태 복원 테스트는 Compose의 저장 상태 복원과 ViewModel 상태를 검증한다. 실제 OS 프로세스 종료 후 복원과 동일한 테스트라고 간주하지 않는다.

2026-11-04 기준 위 검사와 iOS 시뮬레이터 앱 빌드가 통과했다. Pixel 7 API 36에서 기기 테스트를 통과했고, iPhone 17 Pro·iPad mini 시뮬레이터(iOS 26.5)에서 시스템 공유창이 열리는 것을 확인했다. iOS에서 OS URL 수신과 잘못된 링크 안내도 확인했다. 실제 메신저별 링크 인식·수신자에게 전달된 링크의 클릭 동작, 실서버에서 유효한 코드로 참여 완료하는 시나리오는 이 검증에 포함하지 않았다.
