# 백엔드 PR 작성 하네스

이 지침은 백엔드 변경을 포함하는 쓰기 작업의 종료 관문이다. 작업을 수행한 AI 에이전트가
검증, 커밋, 브랜치 푸시를 마친 뒤 별도 승인 요청 없이 PR을 생성하거나 현재 브랜치의 열린
PR을 갱신한다.

이 하네스는 `backend/` 안의 에이전트 지침과 본문 템플릿만 사용한다. 저장소 공용
`.github` 설정과 `android/` 파일은 수정하지 않는다. 선택 배경은
[AI 에이전트 PR 작성 자동화 ADR](../../ADR/협업_AI_에이전트_PR_작성_자동화.md)에 기록되어 있다.

## 1. 적용 조건과 중단 조건

다음 조건을 모두 충족하면 실행한다.

- 백엔드 변경을 포함하는 쓰기 작업이다.
- [커밋 전 게이트](../검증/커밋_전_게이트.md)를 통과했거나, 문서만 변경해 지침에 따라
  게이트를 생략했다.
- 변경을 작업 브랜치에 커밋했고 원격 저장소에 푸시했다.

다음 중 하나라도 해당하면 PR을 만들거나 갱신하지 않고 blocker를 보고한다.

- 현재 브랜치가 `main` 또는 `develop`이다.
- 브랜치 푸시가 실패했거나 원격에 현재 커밋이 반영되지 않았다.
- 3장의 근거나 예외 확인이 준비되지 않았다.
- GitHub CLI 인증 사용자가 4장의 백엔드 개발자 목록에 없다.
- 기존 PR의 실제 author가 4장의 백엔드 개발자 목록에 없다.
- 현재 head에 대응하는 열린 PR이 둘 이상이어서 갱신 대상을 하나로 확정할 수 없다.
- GitHub 인증·권한 문제로 생성, 갱신 또는 사후 검증을 수행할 수 없다.

## 2. 브랜치와 PR 제목

[Yeobaek Flow](../../ADR/협업_브랜치_전략으로_Yeobaek_Flow_선택.md)를 따른다.

- 일반 작업: `develop`에서 분기한 작업 브랜치를 `develop`으로 제출한다.
- 운영 긴급 수정: `main`에서 분기한 `hotfix/*` 브랜치를 `main`으로 제출한다.
- `main`과 `develop`에는 직접 커밋하거나 직접 푸시하지 않는다.

PR 제목은 AngularJS/Conventional Commits 형식인 `<type>(<scope>): <summary>`를 사용한다.
scope가 불필요하면 `<type>: <summary>`로 쓴다. 예: `feat(member): 회원 가입 구현`,
`docs: 백엔드 PR 작성 하네스 추가`.

작업 종류는 제목의 type으로 표현한다. label은 추가하지 않으며, 기존 PR을 갱신할 때 남아
있는 label도 모두 제거한다.

## 3. 대화 근거 관문

PR 본문은 [백엔드 PR 본문 템플릿](../../템플릿/백엔드_PR_본문.md)을 원본으로 삼아 작성한다.
PR 전체의 변경 이유를 먼저 적고, 독립적으로 설명할 수 있는 각 구현 항목마다 다음 내용을
빠짐없이 적는다.

- 구현 내용
- 구현 이유
- 검토한 선택지
- 최종 선택
- 선택 근거

구현 내용은 diff로 확인한 사실을 쓴다. 구현 이유와 선택 근거에는 작업 중 개발자와 AI가
나눈 대화에서 **개발자가 확정한 문장만** 사용한다. 개발자가 원문 사용을 승인했다면 원문을,
[피드백 루프 하네스 R2](피드백_루프_하네스.md#1-철칙)에 따라 정리 초안을 명시적으로
선택했다면 그 정리 초안을 사용한다. PR 조립 단계에서 판단을 새로 보충하거나 의미를 바꾸어
요약하지 않는다.

근거가 없거나 선택 이유를 설명하기에 약하면 PR 생성 전에 해당 구현 항목을 특정해 개발자에게
근거 보충을 요청한다. 다음 중 하나가 충족되기 전에는 PR을 생성하거나 갱신하지 않는다.

1. 개발자가 사용할 근거 문장을 확정한다.
2. 개발자가 **근거 보충이 어렵다**고 명시한다.
3. 개발자가 **근거가 필요 없는 사안**이라고 명시한다.

2번이나 3번이면 그 항목의 선택 근거만 예외적으로 생략하고, PR 본문의 `근거 예외 확인`에
개발자가 확정한 예외 문장을 그대로 남긴다. 단순 무응답, AI의 판단, “사소해 보인다”는 추정은
예외가 아니다.

[피드백 루프 하네스](피드백_루프_하네스.md)의 “이유를 한 번 물은 뒤 구현 진행” 규칙은
그대로 유지된다. 다만 이 장은 PR 공개 기록을 만드는 별도 관문이므로, 구현이 완료되었어도
근거나 명시적 예외 확인이 없으면 PR 제출 단계에서 멈춘다.

## 4. 제출자와 reviewer

백엔드 개발자 GitHub 계정은 다음 세 명으로 고정한다.

- `alstj2384`
- `Yeji-Kim-Erica`
- `kjoon418`

PR 제출 직전에 다음 명령으로 GitHub CLI 인증 사용자를 확인한다.

```shell
gh api user --jq '.login'
```

반환된 login이 위 목록 중 하나여야 한다. 아니라면 credential blocker로 중단하며 다른 사람을
제출자로 추정하지 않는다.

- 새 PR에서는 인증 사용자가 author이므로 그 사용자를 제외한 나머지 두 계정을 reviewer로
  계산한다.
- 기존 PR에서는 다음 명령으로 실제 author를 조회한다.

  ```shell
  gh pr view "$number" --json author --jq '.author.login'
  ```

  author가 위 목록 중 하나가 아니면 중단한다. 인증 사용자가 아니라 실제 author를 제외한
  나머지 두 계정을 reviewer로 계산한다.

## 5. 생성 또는 갱신

아래 명령의 `$base`, `$head`, `$title`, `$bodyFile`, `$authenticatedLogin`, `$author`,
`$reviewer1`, `$reviewer2`, `$number`, `$label`은 에이전트가 앞 단계에서 확정하거나 명령
결과에서 추출한 값이다. `$bodyFile`은 추적 중인 `docs/템플릿/백엔드_PR_본문.md`를 복사해
모든 항목을 채운 임시 파일이다. 이 파일에는 HTML 안내 주석, `구현 항목 이름`, 템플릿의
안내 문구, 빈 필수 필드를 남기지 않는다. 템플릿 자체를 작업별 내용으로 덮어쓰지 않는다.

1. 현재 브랜치를 head로 확정하고 원격에 푸시한다.

   ```shell
   git push -u origin "$head"
   ```

2. 현재 head의 열린 PR을 찾는다.

   ```shell
   gh pr list --head "$head" --state open --json number,url
   ```

3. 열린 PR이 없으면 `$author`를 `$authenticatedLogin`으로 정하고 author를 제외한 reviewer
   두 명을 계산한다. base, head, 제목, 본문 파일, reviewer 두 명을 모두 명시해 생성하며
   label 인수는 사용하지 않는다.

   ```shell
   gh pr create --base "$base" --head "$head" --title "$title" --body-file "$bodyFile" --reviewer "$reviewer1" --reviewer "$reviewer2"
   ```

   생성 후 2단계 명령을 다시 실행해 생성된 PR의 번호를 확정한다.

4. 열린 PR이 하나면 4장의 명령으로 실제 author를 조회하고 allowlist에 있는지 검증한 뒤,
   author를 제외한 reviewer 두 명을 계산한다. 그 번호를 지정해 base, 제목, 본문, reviewer를
   갱신한다. `gh pr edit`은 head 변경 옵션을 제공하지 않으므로, 2단계에서 현재 head로 찾은
   PR만 갱신한다.

   ```shell
   gh pr edit "$number" --base "$base" --title "$title" --body-file "$bodyFile" --add-reviewer "$reviewer1" --add-reviewer "$reviewer2"
   ```

5. 다음 명령으로 label 이름을 추출한다.

   ```shell
   gh pr view "$number" --json labels --jq '.labels[].name'
   ```

   반환된 각 이름을 `$label`로 두고 다음 명령을 실행해 모두 제거한다.

   ```shell
   gh pr edit "$number" --remove-label "$label"
   ```

## 6. 사후 검증

생성 또는 갱신 직후 다음 명령을 실행한다.

```shell
gh pr view "$number" --json number,title,body,labels,reviewRequests,author,baseRefName,headRefName,url
```

출력에서 다음 조건을 모두 직접 대조한다.

- `baseRefName`과 `headRefName`이 2장에서 확정한 base와 head다.
- `title`이 Conventional Commits 형식이고 작업을 대표한다.
- `body`에 전체 변경 이유와 각 구현 항목의 다섯 필수 내용이 있다.
- 근거 생략 항목마다 개발자의 명시적 예외 확인이 있다.
- `body`에 HTML 안내 주석, `구현 항목 이름`, 템플릿 안내 문구 또는 빈 필수 필드가 없다.
- `labels`가 비어 있다.
- `author.login`이 백엔드 개발자 목록에 있다.
- `reviewRequests`에 `author.login`을 제외한 백엔드 개발자 두 명이 모두 있다.

하나라도 다르면 수정 후 같은 명령으로 다시 검증한다. 검증된 PR URL을 작업 완료 보고에
포함해야 종료할 수 있다.
