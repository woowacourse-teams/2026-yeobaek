# Git Flow 지침

> 선택 배경: [브랜치 전략으로 Yeobaek Flow 선택](../backend/docs/ADR/협업_브랜치_전략으로_Yeobaek_Flow_선택.md)

## 브랜치 역할

- `main`: 운영에 배포 가능한 안정 버전만 유지한다.
- `develop`: 다음 배포를 위한 변경 사항을 통합한다.
- `feature/*`, `bug/*`, `refactor/*`, `chore/*`: `develop`에서 분기해 작업하고, Pull Request로 `develop`에 병합한다.
- `hotfix/*`: 운영 긴급 수정 시 `main`에서 분기하고, 수정 후 `main`과 `develop`에 모두 병합한다.
- `release/*`: 현재는 사용하지 않으며, 출시 후보를 장기간 검증해야 할 때 도입한다.

브랜치 이름은 작업 내용을 짧게 나타내는 kebab-case를 사용한다.

```text
feature/group-create
bug/reader-scroll-offset
release/1.0.0
hotfix/login-crash
```

## 작업 순서

1. 일반 작업은 최신 `develop`에서, 운영 긴급 수정은 최신 `main`에서 작업 브랜치를 만든다.
2. 하나의 브랜치에는 하나의 목적에 해당하는 변경만 담는다.
3. 일반 작업은 `develop`으로, 운영 긴급 수정은 `main`으로 Pull Request를 생성한다.
4. 리뷰와 CI를 통과하면 병합하고 작업 브랜치를 삭제한다.
5. `hotfix/*`를 `main`에 병합한 뒤 같은 변경을 `develop`에도 반드시 반영한다.

`main`과 `develop`에는 직접 커밋하거나 직접 푸시하지 않는다.

Pull Request는 두 명 이상의 승인을 받고 필수 CI를 통과해야 병합할 수 있다. 새 커밋이 추가되면 기존 승인을 무효화하고 다시 리뷰한다. `main`과 `develop`의 강제 푸시와 삭제는 허용하지 않는다.

## 배포 흐름

`develop`에서 기능을 통합하고 Android 개발자가 로컬 연동 테스트를 수행한 뒤, `develop`에서 `main`으로 Pull Request를 생성한다. 리뷰와 CI를 통과한 변경만 `main`에 병합해 운영에 배포한다.

```text
feature/* ─┐
bug/*     ─┼─> develop ── 검증 후 PR ──> main
refactor/*─┤
chore/*   ─┘

hotfix/*: main에서 분기 ──> main과 develop에 모두 반영
```

## Release 브랜치 도입 기준

현재는 `develop`의 한 스냅샷을 출시 후보로 고정할 필요가 없으므로 `release/*`를 사용하지 않는다. 다음 상황이 생기면 도입을 검토한다.

- 한 스냅샷을 장기간 QA하는 동안 `develop`에서 다음 기능 개발을 계속해야 할 때
- 스토어 심사나 출시 준비로 출시 후보의 안정화 기간이 길어질 때
- 특정 출시 버전만 대상으로 수정하고 재검증해야 할 때
