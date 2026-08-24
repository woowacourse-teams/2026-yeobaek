# ADR: 브랜치 전략으로 Yeobaek Flow 선택

## 상태

확정 (2026-08-24, 개발자).

## 맥락

개발한 내용을 사용자에게 공개하기 전에 검증할 수 있는 공간이 필요하다. 현재 백엔드는
`develop` 변경을 테스트한 뒤 Android 개발자가 로컬에서 사용할 `:develop` 이미지를
발행하고, `main` 변경은 운영에 배포한다
([backend-ci.yml](../../../.github/workflows/backend-ci.yml),
[backend-deploy.yml](../../../.github/workflows/backend-deploy.yml)).

이 결정은 P-124에서 `develop` 브랜치의 백엔드 변경을 Android 개발자가 로컬에서
연동 테스트할 수 있도록 환경을 구축한 선택과 맞닿아 있다.

## 검토한 선택지 (2026-08-24 개발자 기록)

### 선택지 A: GitHub Flow

`main`만 장기 브랜치로 유지한다. 기능 개발 시 작업 브랜치를 만들고, 검증이 끝나면
`main`에 바로 병합한다.

**장점**

- 브랜치 구조와 병합 과정이 단순하다.
- 변경 사항을 빠르게 운영에 반영할 수 있다.

**단점**

- CI 테스트만으로 발견하기 어려운 API 연동 문제도 운영 배포 전에 검증해야 한다.
- 백엔드 변경을 Android 앱과 통합 검증할 브랜치가 없다.

### 선택지 B: Git Flow

`main`, `develop` 및 작업 브랜치를 분리한다.

**장점**

- `develop` 변경으로 생성된 백엔드 이미지를 Android 개발자가 로컬에서 검증할 수 있다.
- 기능 개발 완료 시점과 운영 배포 시점을 분리할 수 있다.
- 백엔드와 Android 앱의 통합 문제를 운영 반영 전에 확인할 수 있다.

**단점**

- `develop`과 `main` 사이의 추가 병합 단계가 필요하다.

## 결정

**Git Flow를 단순화한 Yeobaek Flow를 사용한다.**

별도 작업 브랜치에서 Pull Request를 만들면 리뷰 시점을 확보하고, 작업 공간을 분리하며,
병합 전에 CI 검증을 수행할 수 있다. `develop`에서 기능을 통합하고 Android 개발자가
로컬 연동 테스트를 수행한 뒤 `main`으로 병합한다.

브랜치의 역할은 다음과 같다.

- `main`: 운영 배포 기준
- `develop`: 다음 운영 배포를 위한 통합 및 로컬 연동 테스트 기준
- `feature/*`, `bug/*`, `refactor/*`, `chore/*`: 개별 작업 브랜치
- `hotfix/*`: 운영 긴급 수정 브랜치
- `release/*`: 필요해질 때 도입하는 출시 후보 브랜치

## 운영 규칙

- 일반 작업은 `develop`에서 분기하고 Pull Request로 `develop`에 병합한다.
- 운영 긴급 수정은 `main`에서 `hotfix/*`로 분기하고, 수정 사항을 `main`과
  `develop`에 모두 반영한다.
- `main`과 `develop`에는 직접 푸시하지 않는다.
- Pull Request는 작성자를 제외한 두 명의 승인을 받아야 한다.
- Pull Request CI는 `./gradlew build`를 실행해 단위·통합 테스트와 PMD·SpotBugs
  정적 분석을 수행한다.

구체적인 작업 절차는 [Git Flow 지침](../../../docs/Git_Flow_지침.md)을 따른다.

## Release 브랜치

P-124를 통해 `develop`의 한 스냅샷을 Android 개발자가 로컬에서 연동 테스트할 수
있으므로 현재는 별도의 `release/*` 브랜치를 사용하지 않는다.

다만 한 스냅샷을 장기간 QA하는 동안 `develop`에서 다음 기능 개발을 계속해야 하는
상황이 생기면 `release/*`를 도입한다. 스토어 심사나 출시 준비로 출시 후보의 안정화
기간이 길어지거나 특정 출시 버전만 수정하고 재검증해야 할 때도 도입을 검토한다.

## 한계·트레이드오프

- 작업 브랜치를 `develop`에 병합한 뒤 다시 `main`으로 병합하는 단계가 추가된다.
- `main`의 긴급 수정은 `develop`에도 반영해야 하므로 동기화 책임이 생긴다.
