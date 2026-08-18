# Project Skills

팀 공용 Claude Code 스킬 디렉터리입니다. 저장소 루트에 있으므로 저장소 안 어느 디렉터리(루트, `backend/` 등)에서 Claude Code를 실행하든 별도 설치 없이 세션 시작 시점부터 바로 사용할 수 있습니다. Codex용 복사본은 `.agents/skills/`에 있으며, `grilling`과 `deep-interview`의 `SKILL.md`는 두 디렉터리에서 동일하게 유지합니다.

## grill-me / grilling

- `/grill-me`: 개발자가 자기 언어로 계획이나 설계를 설명한 뒤 호출하면, 결정 트리의 구멍을 집요하게 검증합니다. 내부적으로 프로젝트 하네스에 맞춘 `grilling` 스킬을 호출합니다.
- 출처: [mattpocock/skills](https://github.com/mattpocock/skills) (`skills/productivity/grill-me`, `skills/productivity/grilling`) — MIT License, Copyright (c) Matt Pocock.

## deep-interview

- `/deep-interview`: 설계 초안을 만들기 전의 모호한 요청을 한 번에 한 질문씩 구체화합니다.
  목표·범위·제약·완료 기준·영향 범위가 정리되면 `/grill-me` 또는 공식 승인 관문으로 넘깁니다.
- 출처: [devbrother2024/skills](https://github.com/devbrother2024/skills) (`deep-interview`) — MIT License.
- 프로젝트 규칙: `backend/docs/지침/deep-interview_활용.md`와 피드백 루프 하네스가 upstream
  기본 동작보다 우선합니다.
