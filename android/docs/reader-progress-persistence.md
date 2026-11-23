# 읽기 위치 보존과 서버 동기화

읽기 위치는 계정·공간 유형·공간 ID·책 ID별로 Room에 저장한다. 서버 저장이 실패해도 로컬 DB에 커밋된 최신 위치는 유지한다. 본문 전체의 오프라인 캐시는 이 변경에 포함하지 않는다.

## 저장과 복원

- 실제 사용자 스크롤과 완료된 문단 이동은 앱 범위의 직렬 큐에 checkpoint를 제출한다. 문단과 revision은 한 트랜잭션으로 기록한다.
- 화면 초기 배치·글자 크기 변경·알림 진입만으로 새 checkpoint를 만들지 않는다.
- 뒤로가기는 제출한 로컬 쓰기 완료까지만 기다린다. 디스크 저장이 실패하면 화면에 남아 오류를 안내한다.
- 동일 위치의 중복 기록은 revision을 증가시키지 않는다. 앞 문단을 다시 읽는 것은 새 변경으로 취급한다.
- 서버 상세 요청 전에 미반영 위치를 확보한다. 로딩 중 동기화가 끝나더라도 오래된 서버 응답보다 이 스냅샷 또는 더 최신 로컬 revision을 사용한다.
- 일반 진입은 미반영 로컬 위치, 서버 위치, 책 시작 위치 순서로 복원한다. 알림의 명시적인 댓글 위치는 우선한다. 수동 이동 전까지 기존 미반영 위치를 덮어쓰지 않는다.
- 초기 적용 결과는 요청값을 되풀이하지 않고 레이아웃에서 목표 문단이 실제 표시되는지 확인한다. 화면 끝에서 상단 정렬이 불가능해도 목표 문단이 보이면 적용 성공이다.

## 전송과 실패 처리

동기화는 ViewModel 수명과 독립된 앱 scope에서 실행한다. 빠른 checkpoint의 서버 전송은 1초 동안 모으되 로컬 기록은 지연하지 않는다. 뒤로가기·백그라운드 전환은 즉시 동기화를 요청한다. 같은 위치의 요청은 직렬 처리한다.

HTTP 408·429·5xx와 네트워크 I/O 오류는 최대 3회, 1초·2초 간격으로 재시도한다. 다른 HTTP 오류·잘못된 응답은 즉시 멈추고 로컬 위치를 유지한다. 각 재시도는 DB의 최신 위치를 다시 읽는다. 서버가 확인한 문단이 전송 문단과 다르면 완료 처리하지 않는다.

오래된 응답은 더 최신 revision의 미반영 상태를 해제하지 않는다. 다음 이벤트에서 다시 전송을 요청한다.

- 앱 첫 실행과 포그라운드 복귀: 앱 루트 ON_START
- 연결 복구: Android 검증된 기본 인터넷 연결, iOS NWPathMonitor
- 계정 확정·변경: 해당 계정의 미반영 위치 확인

네트워크 연결 신호는 해당 서버나 DNS의 정상 여부를 보장하지 않는다. 프로세스 종료·iOS 실행 중단 동안 전송을 보장하지 않으며 다음 실행·복귀 때 재개한다. 커밋되기 전에 프로세스가 종료된 최신 메모리 위치까지 보존한다고 주장하지 않는다.

전송에는 checkpoint 소유 계정을 명시적으로 결합한다. 요청 허용 대기 후에도 현재 계정을 확인하고 헤더를 하나로 설정한다. 계정 변경 시 이전 네트워크 작업을 취소하고 늦은 응답의 반영·분석 이벤트를 차단한다.

## 로그와 지표

- `reader_progress_checkpointed`: 로컬 쓰기 결과. 책·공간·문단·`progress_revision`·`result`.
- `reader_progress_saved`: 서버 동기화 시도 결과. 기존 이름은 유지하되 새 구현은 `schema_version=2`. `save_attempt_id`, `attempt_number`, `will_retry`, 변경 번호, 목표·서버 확인 문단, trigger, duration_ms, result를 기록한다.
- `reader_progress_restored`: 실제 초기 적용 결과. `resume_source=local_pending/saved_progress/book_start/notification`, 기대·실제 문단, 선택한 로컬 revision, schema_version을 기록한다.
- Firebase는 중간 재시도를 WARN 로그로 남기고 최종 실패만 비치명적 예외로 수집한다. 로컬 쓰기 실패와 서버 저장 실패는 다른 operation이다.
- 회원 ID·닉네임·본문·댓글·인증정보는 CrashContext나 추가 이벤트 속성에 넣지 않는다. 기존 PostHog identify와 공통 installation_id를 사용한다.

위치 복원 성공률은 **저장 실패 후 재진입이 관측된 최신 유효 checkpoint 중 기대 위치로 복원된 비율**이다. installation_id·현재 사용자·공간·책·revision을 연결하고, 후속 변경으로 대체된 위치와 알림 진입은 분리한다. 재진입하지 않은 대상은 미관측으로 별도 집계한다. 이벤트 미수집과 지연 수집도 별도 한계다.

서버 저장 성공률은 수집된 서버 시도 결과의 비율이다. 새 버전은 자동 checkpoint와 재시도를 포함하므로 이전의 화면 종료 저장만 있었던 버전과 단순 비교하지 않는다. 복원 성공률·대상 수·관측 기간·앱 버전을 함께 보고한다.

## 검증

```sh
./gradlew :shared:testAndroidHostTest :shared:ktlintCheck
./gradlew :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest
./gradlew :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yeobaek.ReaderProgressRecoveryTest
./gradlew :shared:compileTestKotlinIosSimulatorArm64
```

주요 회귀 시나리오:

1. 서버 1번, 로컬 17번 저장 실패 → DB 재오픈 후 17번 표시.
2. 서버 응답 대기 중 스크롤·뒤로가기 → 최신 로컬 commit 뒤 화면 이동.
3. 17번 요청 중 34번 checkpoint → 17번 응답이 34번의 미반영 상태를 해제하지 않음.
4. 48번에서 17번으로 돌아가 읽기 → 17번을 최신 위치로 저장.
5. 화면 로딩 중 ack·새 checkpoint → 더 최신 위치 선택.
6. 알림으로 책 끝 근처 진입·초기 reflow → 사용자 이동 전까지 기존 pending 유지.
7. 로컬 쓰기 실패 → 뒤로가기 차단, 오류 안내, 재시도 성공 후 이동.
8. 계정 변경·늦은 응답 → 이전 계정 데이터 전송·완료·분석 이벤트 차단.
9. 진단 도구 예외 → checkpoint·서버 반영 결과에 영향 없음.

실제 네트워크 장애 수동 확인은 테스트 계정·같은 책/모임에서 진행한다. 정상 위치를 저장한 뒤 연결을 끊고 이미 로드된 문단 안에서 이동한다. 뒤로가기 후 앱 종료, 연결 복구, 재실행 시 마지막 로컬 위치와 서버 재반영을 확인한다. 비행기 모드는 특정 DNS 예외 종류를 보장하지 않는다.

## 서버 API의 한계

현재 PUT 본문은 passageId만 받는다. 로컬 revision 검사는 로컬 완료 처리와 클라이언트 요청 순서를 보호한다. 이미 타임아웃된 옛 요청이 서버에서 뒤늦게 처리되거나 다른 기기가 같은 위치를 갱신하는 순서까지 보장하지 않는다. 이를 해결하려면 서버의 버전 조건부 갱신·요청 중복 방지와 다중 기기 충돌 정책을 별도로 구현해야 한다.

## 2026-11-23 검증 기록

- 공통/Android 호스트 테스트: 176개 통과, 실패·오류 0개. 실제 ReaderViewModel 5개, 동기화 18개, 파일 기반 DB 5개, 복원 결정 6개, HTTP 전송 4개, 텔레메트리 7개, 네트워크 상태 6개를 포함한다.
- `:shared:ktlintCheck`, `:androidApp:lintDebug`, Android Debug 앱·기기 테스트 APK 빌드 통과.
- Xcode Debug iOS 시뮬레이터 앱 빌드 통과. 심볼 업로드·코드 서명은 실행하지 않았다.
- iOS 저장소 런타임: Firebase 의존성을 제외한 임시 KMP 프로젝트에서 **동일한 생산/테스트 소스 9개의 SHA-256 일치**를 확인하고 실제 Room·bundled SQLite로 5개 테스트 통과. 잘못된 기대값을 넣은 대조 실험은 실제로 실패했으며 소스 복원 후 다시 5개 통과했다.
- 저장소 네이티브 테스트는 앱 전체의 iOS 화면 검증을 의미하지 않는다. 원래 `:shared:iosSimulatorArm64Test`는 GitLive가 요구하는 Firebase framework와 Xcode SwiftPM object의 링크 구성 차이로 실행되지 않아 분리 검증했다.
- Android API 36 에뮬레이터에서 실제 ReaderScreen·Room을 연결한 복원/뒤로가기/초기 배치 경계 시나리오 3개와 기존 댓글 화면 9개, 총 12개가 통과했다. 테스트는 운영 서버에 장애를 주지 않고 주입한 전송기를 사용한다. 기존 댓글 테스트는 현재 presentation 인자를 전달하고 비동기 문장 수 갱신을 조건으로 기다리도록 맞췄다.
- 로컬 증거: `shared/build/reports/reader-progress-verification/`의 빌드 로그, 기기 테스트 결과, iOS 저장소 결과·소스 해시. 운영 개선율이나 사용자별 위치 유실 감소율은 아직 측정하지 않았다.
