# 여백 API 명세 (계약)

> 기반 문서: [`PRD.md`](PRD.md) · 상태: **구현 계약**
> 이 문서는 백엔드 구현과 Android 연동이 따르는 API 계약의 기준이다. 런타임 OpenAPI 스펙(`/v3/api-docs`)과 Swagger UI(`/docs`)도 이 계약을 따른다.

> **공개방 선행 명세:** 10절은 Android 병렬 개발을 위한 **구현 예정 계약**이다. 이 문서 변경으로
> 서버 기능이나 런타임 OpenAPI가 추가되는 것은 아니다. 10절의 신규 API와 기존 API 확장은
> 후속 구현·배포 이후 사용할 수 있으며, 기존 모임 API의 요청·응답과 구버전 지원은 유지한다.

## 0. 공통 규약

- Base path: `/api` (호스트는 배포 후 공유)
- 요청/응답 본문: JSON (UTF-8), 필드는 camelCase
- **회원 식별**: 회원 생성을 제외한 모든 API는 `X-Member-Id: {회원ID}` 헤더 필수.
  클라이언트는 회원 생성 시 받은 ID를 기기에 저장하고 인터셉터로 전 요청에 첨부한다.
- 시각 필드: ISO-8601 (`2026-08-05T14:30:00`)
- 에러 응답: 상태 코드 + `{ "code": "CLUB_NOT_FOUND", "message": "사람이 읽을 수 있는 설명" }`
  - API는 다음 상태 코드를 사용한다:
    - `401` 인증 실패 (관리자 토큰 누락·불일치)
    - `403` 권한 없음 (모임 미소속, 남의 댓글 등)
    - `400` 그 외 모든 클라이언트 오류. 상세 원인은 바디의 `code`로 구분한다
    - `500` 서버 내부 처리 오류
  - `X-Member-Id` 누락 또는 존재하지 않는 회원: `400`
  - `message`는 예외 발생 지점의 문맥을 설명하는 표시용 값이며 계약이 아니다. 같은 `code`에서도
    요청 문맥에 따라 달라질 수 있으므로 클라이언트 분기는 `code`로만 한다

### 에러 코드

| code | 상태 | 의미 |
|---|---|---|
| `INVALID_REQUEST` | 400 | 본문·파라미터·헤더 형식 오류, 필드 검증 실패 전반 |
| `MEMBER_NOT_FOUND` | 400 | `X-Member-Id` 또는 경로의 `memberId`가 가리키는 회원 없음 |
| `BOOK_NOT_FOUND` | 400 | 대상 도서 없음 |
| `BOOK_NOT_AVAILABLE` | 400 | 대상 도서가 삭제되어 더 이상 이용할 수 없음 |
| `CLUB_NOT_FOUND` | 400 | 대상 모임 없음 |
| `PUBLIC_ROOM_NOT_FOUND` | 400 | 대상 공개방 없음 (10절 구현 예정 계약) |
| `JOIN_CODE_NOT_FOUND` | 400 | 참여 코드에 해당하는 모임 없음 |
| `PASSAGE_NOT_FOUND` | 400 | 대상 본문 없음 |
| `SENTENCE_NOT_FOUND` | 400 | 대상 문장 없음 |
| `COMMENT_NOT_FOUND` | 400 | 대상 댓글이 없거나 신고자에게 보이지 않음 |
| `CANNOT_REPORT_OWN_COMMENT` | 400 | 본인이 작성한 댓글 신고 시도 |
| `CANNOT_BLOCK_SELF` | 400 | 자기 자신을 차단하려는 시도 |
| `AUTHOR_NOT_FOUND` | 400 | (관리자) `authorId`가 가리키는 작가 없음 |
| `DUPLICATE_AUTHOR` | 400 | (관리자) 한 업로드 안에 같은 작가 중복 기재 |
| `AUTHOR_NAME_MISMATCH` | 400 | (관리자) ISNI로 찾은 기존 작가와 요청의 이름 불일치 |
| `DUPLICATE_BOOK` | 400 | (관리자) 제목·출판사·출판연도·작가 구성이 동일한 도서 존재 |
| `NOT_CLUB_MEMBER` | 403 | 모임에 참여하지 않은 회원의 접근 |
| `NOT_COMMENT_OWNER` | 403 | 본인 댓글이 아닌 수정·삭제 시도 |
| `UNAUTHORIZED` | 401 | 관리자 토큰 누락·불일치 (서버에 토큰 미설정 시 관리자 API 전부 이 응답) |
| `INTERNAL_ERROR` | 500 | 서버 내부 처리 중 발생한 예상하지 못한 오류 |

### 도서 상태

모임 목록·상세와 마지막 읽기처럼 삭제된 도서의 식별 정보를 보존해야 하는 응답은 도서
객체에 `status`를 포함한다.

| status | 의미 | Android 처리 |
|---|---|---|
| `ACTIVE` | 현재 이용 가능한 도서 | 기존 읽기 흐름을 제공한다 |
| `DELETED` | 관리자가 삭제하여 이용할 수 없는 도서 | “더 이상 읽을 수 없는 책이에요”를 표시하고 읽기 진입을 막는다 |

현재 값은 `ACTIVE`, `DELETED` 두 개다. `status`는 확장 가능한 enum 계약이므로 Android는
`ACTIVE`일 때만 읽기 기능을 허용하고, 지원하지 않는 미래 값은 이용 불가로 안전하게 처리해야 한다.

### 삭제된 도서가 기존 API에 미치는 영향

| API 영역 | 삭제 전 | 삭제 후 |
|---|---|---|
| 도서 목록·검색 | 결과에 포함 | 결과에서 제외 |
| 도서 상세 | 도서·목차 반환 | `BOOK_NOT_AVAILABLE` |
| 모임 생성 | 도서 선택 가능 | `BOOK_NOT_AVAILABLE` |
| 기존 모임 참여 | 참여 가능 | `BOOK_NOT_AVAILABLE` |
| 내 모임·모임 상세 | `book.status=ACTIVE` | 모임을 유지하고 `book.status=DELETED` |
| 본문·진도 | 조회·갱신 가능 | `BOOK_NOT_AVAILABLE` |
| 마지막 읽기 | 가장 최근 기록 반환 | 같은 기록과 `book.status=DELETED` 반환 |
| 댓글 조회·작성·수정·삭제·신고 | 가능 | 데이터를 보존하고 `BOOK_NOT_AVAILABLE` |
| 관리자 작가·작품 목록 | `status=ACTIVE`로 표시 | 삭제 도서도 `status=DELETED`로 표시 |
| 동일 도서 업로드 | 활성 중복이면 차단 | 삭제된 중복만 있으면 새 ID로 허용 |

## 1. 회원

### 회원 생성
`POST /api/members` — 헤더 불필요 (최초 진입)

요청:
```json
{ "nickname": "민서" }
```
- `nickname`: 1~20자, 공백만은 불가. 이미 사용 중인 닉네임이면 `400` (`INVALID_REQUEST`)을
  반환한다.
- 중복 검사는 DB unique 제약 없이 회원 생성 전에 수행한다. 동시에 같은 닉네임을 생성하는 요청까지
  완전히 차단하지는 않으며, 실제 중복이 문제가 되면 DB 제약과 동시성 처리를 별도로 결정한다
  (2026-09-16 개발자 결정).

응답 `201`:
```json
{ "memberId": 1, "nickname": "민서" }
```

### 계정 삭제
`DELETE /api/members/me`

공개방 도입 후의 방문·진도·댓글 데이터 삭제 범위는 10.9절에서 이 계약을 확장한다 (구현 예정).

요청 본문은 없다.

응답 `204 No Content`. 응답이 반환된 시점에 다음 삭제가 모두 완료된다.

- 요청 회원의 계정
- 요청 회원이 작성한 모든 댓글
- 요청 회원의 모든 모임 참여 기록과 저장된 진도
- 요청 회원에게 귀속된 모든 댓글 조회 상태
- 요청 회원이 차단했거나 요청 회원을 차단한 모든 차단 관계

삭제된 댓글은 이후 서버 조회의 댓글 목록과 문장별 `commentCount`에서 제외된다.
다만 클라이언트가 이미 받은 댓글 문장 목록과 과거 집계는 현재 탐색 동안 유지할 수 있으며,
새 목록 요청부터 삭제를 반영한다. 서버의 계정·댓글·조회 상태 삭제 시점은 변경하지 않는다.
삭제된 모임 참여 기록은 모임 상세의 `members`와 내 모임 목록의 `memberCount`에서 제외된다.

삭제 성공 후 클라이언트는 기기에 저장한 회원 ID를 제거하고 최초 회원 생성 화면으로 이동한다.
삭제된 ID를 `X-Member-Id`로 사용하는 이후 요청은 `400` (`MEMBER_NOT_FOUND`)을 반환한다.

### 차단 목록
`GET /api/members/me/blocks`

응답 `200`:
```json
{
  "blockedMembers": [
    { "memberId": 2, "nickname": "지수" }
  ]
}
```

- `blockedMembers`는 요청 회원이 차단한 회원만 반환한다.
- 닉네임 오름차순으로 정렬하고, 닉네임이 같으면 `memberId` 오름차순으로 정렬한다.

### 사용자 차단
`PUT /api/members/me/blocks/{memberId}`

요청 본문은 없다. 응답 `204 No Content`.

- 차단은 서비스 전체에 적용되는 단방향 관계다. A가 B를 차단하면 A에게만 B의 댓글이 보이지
  않으며, B는 A의 댓글을 계속 볼 수 있다.
- 이미 차단한 회원을 다시 차단해도 `204`를 반환한다 (멱등).
- 자기 자신을 차단하려 하면 `400` (`CANNOT_BLOCK_SELF`)을 반환한다.
- 존재하지 않는 회원을 차단하려 하면 `400` (`MEMBER_NOT_FOUND`)을 반환한다.
- 차단은 양쪽 회원의 모임 참여 상태와 댓글 작성·수정·삭제 권한을 변경하지 않는다.

### 사용자 차단 해제
`DELETE /api/members/me/blocks/{memberId}`

응답 `204 No Content`.

- 차단 관계가 없어도 `204`를 반환한다 (멱등).
- 존재하지 않는 회원이면 `400` (`MEMBER_NOT_FOUND`)을 반환한다.

## 2. 도서

### 도서 목록 · 검색 (모임 생성 시 선택용)
`GET /api/books?keyword={검색어}`

- `keyword`(선택): 제목 **또는** 작가 이름에 부분 일치하는 도서만 반환. 미지정·공백이면 전체 목록 (2026-08-06 추가 — 모임 만들기 플로우의 검색용).
- 삭제된 도서는 목록과 검색 결과에서 제외한다.

응답 `200`:
```json
{
  "books": [
    {
      "bookId": 1,
      "title": "운수 좋은 날",
      "authors": ["현진건"],
      "coverImageUrl": "https://<public-base-url>/yeobaek/book-covers/550e8400-e29b-41d4-a716-446655440000.jpg",
      "publisher": "자체 제작",
      "publishedYear": 1924,
      "passageCount": 312
    }
  ]
}
```

### 도서 상세 + 목차
`GET /api/books/{bookId}`

응답 `200`:
```json
{
  "bookId": 1,
  "title": "운수 좋은 날",
  "authors": ["현진건"],
  "coverImageUrl": "https://<public-base-url>/yeobaek/book-covers/550e8400-e29b-41d4-a716-446655440000.jpg",
  "publisher": "자체 제작",
  "publishedYear": 1924,
  "passageCount": 312,
  "chapters": [
    { "chapterId": 1, "title": "1장", "sequence": 1, "startPassageSequence": 1, "endPassageSequence": 105 }
  ]
}
```
- `coverImageUrl`: 공개 표지 이미지 URL. 표지가 없는 도서는 `null`이며 클라이언트가 기본 이미지를 표시한다.
- `startPassageSequence`/`endPassageSequence`: 해당 챕터에 속한 본문의 전체 순서 범위. 리더의 챕터 이동·범위 조회에 사용.
- 존재하지 않는 도서: `400` (`BOOK_NOT_FOUND`). 삭제된 도서: `400` (`BOOK_NOT_AVAILABLE`).

## 3. 모임

### 모임 생성
`POST /api/clubs`

요청:
```json
{ "name": "교환독서 1기", "bookId": 1 }
```
- `name`: 1~20자, 공백만은 불가 (2026-08-07 추가 — 프로토타입 대조 결정).

응답 `201` — 생성자는 자동으로 모임에 참여된다:
```json
{
  "clubId": 1,
  "name": "교환독서 1기",
  "joinCode": "A3F9KQ",
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "coverImageUrl": null, "passageCount": 312, "status": "ACTIVE" }
}
```
- `joinCode`: 서버 발급, 전역 unique, 영구 고정.
- 삭제된 도서 ID로 생성 시도: `400` (`BOOK_NOT_AVAILABLE`).

### 참여 코드로 모임 참여
`POST /api/clubs/join`

요청:
```json
{ "joinCode": "A3F9KQ" }
```

- 참여 코드는 대문자·숫자 6자리이다. 누락·`null`·형식 오류는 `400` (`INVALID_REQUEST`)로 거부한다.

응답 `200`:
```json
{
  "clubId": 1,
  "name": "교환독서 1기",
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "coverImageUrl": null, "passageCount": 312, "status": "ACTIVE" }
}
```
- 존재하지 않는 코드: `400` (`JOIN_CODE_NOT_FOUND`). 이미 참여한 모임: `200`과 동일 응답 (멱등).
- 탈퇴한 모임에 재가입하면 기존 참여 정보와 진도를 복구한다.
- 삭제된 도서를 읽는 모임에는 새로 참여하거나 재가입할 수 없으며 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 모임 탈퇴
`DELETE /api/clubs/{clubId}/members/me`

응답 `204 No Content`.

- 참여 정보는 삭제하지 않고 `LEFT`로 변경하며, 기존 댓글·작성자 정보·진도는 보존한다.
- 이미 탈퇴한 회원의 중복 요청도 `204`로 처리한다 (멱등).
- 가입 이력이 없는 회원: `403` (`NOT_CLUB_MEMBER`). 존재하지 않는 모임: `400` (`CLUB_NOT_FOUND`).
- 탈퇴 회원은 재가입 전까지 모임 상세·본문·진도·댓글 조회 및 작성과 기존 댓글 수정·삭제를 사용할 수 없다.

### 내 모임 목록
`GET /api/clubs`

응답 `200`:
```json
{
  "clubs": [
    {
      "clubId": 1,
      "name": "교환독서 1기",
      "memberCount": 4,
      "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "coverImageUrl": null, "passageCount": 312, "status": "DELETED" },
      "myProgress": {
        "lastReadPassageSequence": 42,
        "progressRate": 13,
        "lastReadAt": "2026-08-05T14:30:00"
      }
    }
  ]
}
```
- `myProgress`: 아직 읽기 시작 전이면 `null`.
- `progressRate`: 0~100 정수 (반올림). `lastReadPassageSequence ÷ passageCount × 100`.
- 탈퇴한 모임은 목록에서 제외되며 `memberCount`는 참여 중인 회원만 집계한다.
- 삭제된 도서의 모임도 목록에서 제거하지 않는다. 도서 식별 정보와 저장된 진도는 그대로
  반환하고 `book.status`만 `DELETED`로 내려준다.

### 모임 상세 (2026-08-07 추가 — 프로토타입 대조 결정)
`GET /api/clubs/{clubId}`

모임 상세 화면용: 초대 코드 표시·복사, 참여자 목록, 내 진행률.

응답 `200`:
```json
{
  "clubId": 1,
  "name": "교환독서 1기",
  "joinCode": "A3F9KQ",
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "coverImageUrl": null, "passageCount": 312, "status": "DELETED" },
  "myProgress": {
    "lastReadPassageSequence": 42,
    "progressRate": 13,
    "lastReadAt": "2026-08-05T14:30:00"
  },
  "members": [
    { "memberId": 1, "nickname": "민서", "mine": true, "blocked": false },
    { "memberId": 2, "nickname": "지수", "mine": false, "blocked": true }
  ]
}
```
- `myProgress`: 내 모임 목록과 동일 형태. 아직 읽기 시작 전이면 `null`.
- `members`: 참여 중인 회원만 참여 시각 오름차순으로 반환한다. `mine`은 요청자(`X-Member-Id`) 본인 여부 (댓글의 `mine`과 동일 규약).
- `blocked`: 요청자가 해당 회원을 차단했는지 여부. 단방향 차단 관계만 반영하며 요청자 본인은
  항상 `false`다. 차단된 회원도 `members`와 `memberCount`에서 제외하지 않는다.
- 모임 미소속 회원: `403` (`NOT_CLUB_MEMBER`). 존재하지 않는 모임: `400` (`CLUB_NOT_FOUND`).
- 삭제된 도서의 기존 모임도 상세 정보를 반환한다. 모임·참여자·초대 코드·저장된 진도는
  유지하고 `book.status`를 `DELETED`로 반환하지만, 해당 코드로 신규 참여하는 요청은 차단한다.

## 4. 읽기 · 진도

모임에서의 읽기는 해당 모임 맥락에서 이루어진다 (진도·댓글이 모임 단위이므로).

이 절은 기존 모임 읽기 계약이다. 공개방의 독립 진도와 통합 최근 읽기는 10절에서 정의한다.

### 본문 범위 조회
`GET /api/clubs/{clubId}/passages?from={sequence}&to={sequence}`

- `from`·`to`: 전체 순서 기준 범위 (양 끝 포함). `to - from + 1 ≤ 100`, 초과 시 `400`.
- 모임 미소속 회원: `403`.

응답 `200`:
```json
{
  "passages": [
    {
      "passageId": 1042,
      "sequence": 42,
      "chapterId": 2,
      "sentences": [
        {
          "sentenceId": 5012,
          "sequence": 1,
          "content": "새침하게 흐린 품이 눈이 올 듯하더니...",
          "commentCount": 3
        }
      ]
    }
  ]
}
```
- `Passage`는 문단을 나타내는 컨테이너이며 문단 원문이나 문단 전체 댓글 수를 별도 필드로 제공하지 않는다.
- `sentences`는 문단에 속한 문장을 `sequence` 오름차순으로 반환한다.
- 문장의 `sequence`는 해당 문단 안에서 1부터 시작하는 순서다. `commentCount`는 이 모임에서 해당
  문장에 작성된 댓글 중 요청자에게 보이는 댓글 수다. 요청자가 차단한 회원의 댓글은 집계에서 제외한다.
- 문장 `content`에는 원문의 공백과 개행이 그대로 포함된다. 서버는 앞뒤 공백을 제거하지 않으며,
  한 문단의 문장 `content`를 `sequence` 순서로 연결하면 원래 문단 원문이 복원된다.
- 모임 도서가 삭제된 경우 본문을 반환하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 진도 갱신 (최근 열람 보고)
`PUT /api/clubs/{clubId}/progress`

요청:
```json
{ "passageId": 1042 }
```

응답 `200`:
```json
{
  "lastReadPassageSequence": 42,
  "progressRate": 13,
  "lastReadAt": "2026-08-05T14:30:00"
}
```
- 항상 마지막 열람 본문으로 덮어쓴다 (앞부분 재열람 시 진도율 후퇴 — PRD 3.4 트레이드오프).
- 클라이언트는 일반 뷰어를 종료할 때 마지막으로 화면에 표시한 문단 ID로 호출한다. 본문을 읽는
  동안에는 진도 갱신 호출을 늘리지 않고, 댓글 발견 API에 현재 문단 ID를 조회 경계로 전달한다.
- 모임 도서가 삭제된 경우 저장된 진도를 변경하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 홈 — 마지막으로 읽던 책
`GET /api/members/me/last-reading`

구버전 지원을 위해 모임 전용 계약을 유지한다. 공개방·모임 통합 조회는 10절의
`GET /api/members/me/recent-reading`을 사용한다 (구현 예정).

응답 `200` — 전 모임 중 `lastReadAt`이 가장 최근인 것:
```json
{
  "clubId": 1,
  "clubName": "교환독서 1기",
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "coverImageUrl": null, "passageCount": 312, "status": "DELETED" },
  "lastReadPassageSequence": 42,
  "progressRate": 13,
  "lastReadAt": "2026-08-05T14:30:00"
}
```
- 어떤 모임에서도 읽기 기록이 없으면 `204 No Content`.
- 탈퇴한 모임의 읽기 기록은 재가입 전까지 후보에서 제외한다.
- 가장 최근 기록의 도서가 삭제됐더라도 해당 기록을 건너뛰지 않는다. 저장된 위치·진도와
  `book.status=DELETED`를 `200`으로 반환하며, Android는 이어 읽기 동작을 제공하지 않는다.

### 댓글 문장에서 본문으로 이동

댓글 바텀시트의 “보러 가기”는 별도 뷰어 모드를 만들지 않고 일반 뷰어의 해당 위치로 이동한다.
댓글 문장 목록 응답의 `passageSequence`를 `from`·`to` 범위에 포함해 기존 본문 범위 조회 API로
본문을 불러오고 `sentenceId`를 화면 이동 대상으로 사용한다.

- 이동한 화면은 본문 이동·목차·글자 크기·문장별 댓글 조회와 작성, 진도 저장을 포함해 일반
  뷰어의 모든 동작 규칙을 따른다.
- 클라이언트는 이동 직전 일반 뷰어에서 읽던 문단을 화면 상태로 기억하고, 그 위치로 쉽게
  돌아갈 수 있는 동작을 제공한다.
- 돌아가기는 기억한 문단으로 일반 뷰어를 다시 이동하는 동작이다. 서버에 이전 진도를 보관하거나
  복원하는 별도 API를 추가하지 않으며, 돌아간 뒤에도 일반 뷰어의 진도 저장 규칙을 적용한다.

## 5. 댓글

이 절의 모임 소속·탈퇴 조건은 모임 댓글에 적용한다. 공개방 도입 후 공통 댓글 수정·삭제·신고
경로에 공개방 댓글을 전달하는 경우의 권한과 오류는 10.9절에서 정의한다 (구현 예정).

댓글 목록 조회와 작성은 문장을 대상으로 한다. 존재하지 않는 문장을 지정하면 `400`
(`SENTENCE_NOT_FOUND`)을 반환한다.

댓글은 요청 회원마다 다음 조회 상태를 갖는다.

| `viewStatus` | 의미 | 전환 조건 |
|---|---|---|
| `NEW` | 댓글 상세를 직접 확인하지 않은 댓글 | 해당 회원·댓글의 확인 기록이 없음 |
| `VIEWED` | 문장을 선택해 댓글 목록을 직접 확인한 댓글 | 문장의 댓글 상세 조회 성공 또는 본인이 댓글 작성 |

- 서버는 회원·댓글별 확인 기록만 저장한다. 기록이 하나라도 있으면 `VIEWED`, 없으면 `NEW`다.
  사전에 확인 기록을 조회해 없는 것만 저장하되, 동시 최초 조회로 생기는 중복 행은 허용한다.
  조회·집계는 기록 존재 여부로 판단해 중복 행이 응답을 바꾸지 않게 한다.
- 상태는 `NEW → VIEWED` 방향으로만 전환한다. 댓글 문장 목록 조회는 상태를 바꾸지 않는다.
  댓글 수정도 상태를 바꾸지 않으며 정렬에 사용하는 `createdAt`을 그대로 유지한다.
- 기능 도입 전에 존재한 댓글과 신규 참여 전에 작성된 댓글은 해당 회원에게 `NEW`로 시작한다.
- 모임에서 탈퇴해도 조회 상태를 보존한다. 재가입하면 기존 상태를 복원하고, 탈퇴 기간에 작성된
  댓글은 `NEW`로 추가한다.
- 댓글 수·목록·상태 전환은 요청자가 차단하지 않은 작성자의 댓글만 대상으로 한다.
- 댓글 발견 API에서 진도 안쪽은 댓글 문장이 속한 문단의 `sequence`가 요청의
  `currentPassageId`가 가리키는 문단 `sequence` 이하인 범위다. 서버에 저장된 최근 열람 문단은
  이 경계 계산에 사용하지 않으며, `currentPassageId`도 저장된 진도나 `lastReadAt`을 변경하지 않는다.

### 탑바 새 댓글 개수
`GET /api/clubs/{clubId}/comments/new-count?currentPassageId={passageId}`

- `currentPassageId`(필수): 댓글 발견 진도 경계로 사용할 일반 뷰어의 현재 문단 ID다. 이 문단까지를
  진도 안쪽으로 계산한다. 값이 없거나 숫자가 아니거나, 해당 모임 도서에 속한 문단이 아니면 `400`
  (`INVALID_REQUEST`)을 반환한다.

응답 `200`:
```json
{ "newCommentCount": 3 }
```

- 현재 진도 안쪽에 있으면서 `NEW`인 보이는 댓글의 총개수다. 미래 진도의 `NEW` 댓글은 세지 않는다.
- 기능 도입 후 본인이 작성한 댓글은 생성 즉시 `VIEWED`이므로 개수에 포함되지 않는다.
  기능 도입 전에 작성한 본인 댓글은 확인 기록이 없으므로 `NEW`이며 다른 기존 댓글과 동일하게 처리한다.
- 조회 상태를 변경하지 않는 부작용 없는 API다. 호출 시점과 주기는 클라이언트가 결정하며,
  서버 푸시는 이 계약의 범위에 포함하지 않는다.
- 모임 미소속 회원은 `403` (`NOT_CLUB_MEMBER`), 존재하지 않는 모임은 `400`
  (`CLUB_NOT_FOUND`)을 반환한다. 모임 도서가 삭제된 경우 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 댓글 문장 목록 전체 조회
`GET /api/clubs/{clubId}/commented-sentences?currentPassageId=1042`

- `currentPassageId`(필수 쿼리 파라미터): 요청 시점의 일반 뷰어 현재 문단 ID다. 이 문단까지를
  진도 안쪽으로 계산한다. 값이 없거나 숫자가 아니거나 해당 모임 도서의 문단이 아니면 `400`
  (`INVALID_REQUEST`)을 반환한다.
- 모임 책 전체에서 보이는 댓글이 있는 모든 문장을 한 번에 반환한다. 페이지 크기와 커서를
  사용하지 않으며 서버에 탐색별 스냅샷을 저장하지 않는다.
- 클라이언트는 받은 목록·순서·집계·가림 상태를 현재 탐색 동안 유지한다. 이후 댓글 작성·수정·삭제,
  조회 상태 또는 차단 관계 변경과 작성자 계정 삭제가 발생해도 이미 받은 목록은 유지할 수 있다.
  목록을 새로 요청하면 현재 데이터와 요청의 `currentPassageId`를 반영한다.
- 댓글 상세 조회에는 현재 가시성·인가를 적용한다. 과거 목록에 있던 문장도 현재 댓글이 모두
  삭제됐거나 차단됐다면 상세 응답의 댓글은 빈 배열일 수 있다.
- 당장은 모임당 문장 데이터가 수천 개 미만일 것으로 예상해 전체 응답을 선택했다. 네트워크 비용이
  문제가 되면 이 API의 하위호환을 유지하고 페이지네이션을 적용한 새 API를 제공한다.

응답 `200`:
```json
{
  "commentedSentences": [
    {
      "sentenceId": 5012,
      "content": "새침하게 흐린 품이 눈이 올 듯하더니...",
      "passageId": 1043,
      "passageSequence": 43,
      "sentenceSequence": 1,
      "future": true,
      "commentCount": 3,
      "unreadCommentCount": 2,
      "contentVisibility": "REVEAL_REQUIRED",
      "latestCommentCreatedAt": "2026-08-05T14:30:00"
    }
  ]
}
```

- `commentedSentences`에는 모임 책 전체에서 요청 회원에게 보이는 댓글이 하나라도 있는 문장을
  담는다. 댓글 본문과 작성자 정보는 포함하지 않으며, 문장 선택 후 댓글 상세 조회 API를 사용한다.
- `commentCount`는 해당 문장에서 요청 회원에게 보이는 전체 댓글 수다.
  `unreadCommentCount`는 그중 상태가 `NEW`인 댓글 수이며 `0` 이상 `commentCount` 이하다.
- 댓글 문장 목록 조회는 조회 상태를 변경하지 않는다. 같은 문장을 목록에서 여러 번 보더라도
  `unreadCommentCount`는 줄어들지 않으며, 댓글 상세 조회가 성공한 뒤 목록을 새로 요청할 때 갱신된다.
- `future`는 목록 요청의 `currentPassageId`를 기준으로 해당 문장이 진도 밖인지 나타낸다.
- `contentVisibility`는 문장 내용을 최초에 바로 노출할 수 있는지 나타내는 서버의 권위 있는
  정책 값이다.
  - `VISIBLE`: 문장 내용을 바로 노출할 수 있다.
  - `REVEAL_REQUIRED`: 사용자의 명시적인 해제 동작 전까지 문장 내용을 가려야 한다.
- 서버는 `future=true`이면서 `unreadCommentCount>0`일 때만 `REVEAL_REQUIRED`를 반환하고, 그 외에는
  `VISIBLE`을 반환한다. 클라이언트는 `future`와 `unreadCommentCount`를 조합해 노출 정책을 다시
  계산하지 않고 `contentVisibility`를 따른다.
- `REVEAL_REQUIRED`여도 응답의 `content`는 포함한다. 클라이언트는 블러·덮개 등 구체적인 표현과
  사용자가 현재 화면에서 해제했는지를 관리한다. 해제만으로 서버 상태와 응답 필드는 변경되지 않는다.
- 정렬 그룹은 다음 순서다. 같은 그룹에서는 `latestCommentCreatedAt` 내림차순, 값이 같으면
  `sentenceId` 내림차순으로 정렬한다.
  1. `future=false`이고 `unreadCommentCount>0`인 새 댓글 문장
  2. `future=true`이고 `unreadCommentCount>0`인 미래 문장
  3. 진도와 관계없이 `unreadCommentCount=0`인 확인한 문장
- 응답 필드는 해당 목록 요청 시점의 값이다. 이후 변경은 목록을 새로 요청할 때 반영한다.
- 결과가 비어 있으면 `commentedSentences`는 빈 배열이다.
- 모임 미소속 회원은 `403` (`NOT_CLUB_MEMBER`), 존재하지 않는 모임은 `400`
  (`CLUB_NOT_FOUND`)을 반환한다. 모임 도서가 삭제된 경우 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 문장의 댓글 상세 조회 + 직접 확인
`POST /api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views`

요청 본문은 없다.

응답 `200` — 작성일 오름차순:
```json
{
  "comments": [
    {
      "commentId": 7,
      "memberId": 2,
      "nickname": "지수",
      "content": "이 문장에서 멈칫했어요.",
      "createdAt": "2026-08-05T14:30:00",
      "updatedAt": null,
      "mine": false
    }
  ]
}
```
- 응답 생성과 함께 그 시점에 요청 회원에게 보이는 해당 문장의 댓글을 모두 `VIEWED`로
  전환한다. 응답 생성과 상태 전환은 서버 트랜잭션 하나로 처리한다. 서버가 트랜잭션을 완료하지
  못하면 상태를 변경하지 않지만, 커밋 후 네트워크에서 응답만 유실된 경우에는 상태 전환이 유지된다.
- `mine`: 요청자(`X-Member-Id`) 본인 작성 여부. `updatedAt`: 수정된 적 없으면 `null`.
- 요청자가 차단한 회원이 작성한 댓글은 결과에서 제외한다. 차단은 단방향이므로 차단된 회원의
  조회 결과에서는 차단자의 댓글이 계속 보인다.
- 모임에서 탈퇴했지만 계정은 유지 중인 작성자의 댓글도 닉네임과 내용을 변경하지 않고 일반
  댓글과 동일하게 반환한다.
- 존재하지 않는 모임은 `400` (`CLUB_NOT_FOUND`), 모임 미소속 회원은 `403`
  (`NOT_CLUB_MEMBER`), 존재하지 않거나 모임 도서에 속하지 않는 문장은 `400`
  (`SENTENCE_NOT_FOUND`)을 반환한다.
- 모임 도서가 삭제된 경우 보존된 댓글을 반환하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 문장의 댓글 목록 — deprecated 호환 API
`GET /api/clubs/{clubId}/sentences/{sentenceId}/comments`

- 기존 Android 클라이언트와의 호환을 위해 일시적으로 유지한다. 신규 클라이언트는 위의
  `POST .../comment-detail-views`를 사용한다.
- 응답과 오류 계약은 신규 상세 조회 API와 같다. 구버전 클라이언트의 조회도 누락되지 않도록
  보이는 댓글 전체를 `VIEWED`로 전환한다.
- 이 `GET`의 상태 전환은 마이그레이션 기간에만 허용하는 호환성 예외다. Android가 신규
  `POST`로 전환되고 배포까지 완료됐음을 확인한 뒤 별도 변경에서 이 API를 제거한다.

### 댓글 작성
`POST /api/clubs/{clubId}/sentences/{sentenceId}/comments`

요청:
```json
{ "content": "이 문장에서 멈칫했어요." }
```
- `content`: 1~1000자.

응답 `201`: 댓글 목록의 원소와 동일 형태 (`mine: true`).

- 작성자의 조회 상태는 생성 즉시 `VIEWED`다. 다른 회원에게는 `NEW`로 시작한다.
- 모임 도서가 삭제된 경우 댓글을 저장하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 댓글 수정
`PUT /api/comments/{commentId}`

요청:
```json
{ "content": "수정된 내용" }
```

응답 `200`: 댓글 목록의 원소와 동일 형태. 본인 댓글이 아니거나 작성자가 해당 모임에서 탈퇴한 상태면 `403`.

- 수정은 어떤 회원의 조회 상태도 변경하지 않으며 `createdAt`도 유지한다.
- 댓글이 연결된 도서가 삭제된 경우 기존 내용을 변경하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 댓글 삭제
`DELETE /api/comments/{commentId}`

응답 `204`. 본인 댓글이 아니거나 작성자가 해당 모임에서 탈퇴한 상태면 `403`. 하드 삭제 (PRD 3.5).

- 댓글이 연결된 도서가 삭제된 경우 댓글을 제거하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.
- 삭제된 도서의 댓글 데이터와 모임·도서·문단·문장 연결은 보존한다. 이를 다시 보여주는 API와
  탐색 방식은 후속 기능에서 결정한다.

### 댓글 신고
`POST /api/comments/{commentId}/reports`

요청 본문은 없다. 응답 `204 No Content`.

- 신고는 `(신고자, commentId)` 기준으로 한 건만 접수한다. 같은 회원이 같은 댓글을 다시
  신고해도 새 신고를 만들지 않고 `204`를 반환한다 (멱등).
- 본인이 작성한 댓글을 신고하면 `400` (`CANNOT_REPORT_OWN_COMMENT`)을 반환한다.
- 존재하지 않는 댓글이면 `400` (`COMMENT_NOT_FOUND`)을 반환한다.
- 신고자는 댓글이 속한 모임의 현재 참여자여야 한다. 모임 비참여자는 `403`
  (`NOT_CLUB_MEMBER`)을 반환한다.
- 댓글이 연결된 도서가 삭제된 경우 신고를 접수하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.
- 요청자가 차단한 회원의 댓글은 보이지 않는 댓글로 취급해 신고를 접수하지 않고 `400`
  (`COMMENT_NOT_FOUND`)을 반환한다.
- 신고 접수 자체는 댓글 노출을 변경하지 않는다. 신고 상태 조회, 처리 결과 알림, 신고 취소 API는
  제공하지 않는다.
- 신고는 별도 `comment_reports` 테이블에 저장하며 `(reporter_id, comment_id)`를 unique로 둔다.
  신고자 계정이나 대상 댓글이 삭제되면 관련 신고도 DB cascade로 함께 하드 삭제한다.
- 클라이언트는 신고 성공 후 “이 사용자를 차단할까요?”를 제안할 수 있다. 사용자가 차단을
  선택하면 별도의 사용자 차단 API를 호출한다.

## 6. 관리자 (안드로이드 비대상)

모든 `/api/admin/**` API는 `X-Admin-Token: {고정 토큰}` 헤더가 필수다. 앱은 사용하지 않는다.
토큰 누락·불일치는 `401` (`UNAUTHORIZED`). 서버에 토큰이 설정되지 않은 경우에도 전부 `401`이다 (기동은 정상).

### 관리자 통계: 모임 목록과 댓글 수
`GET /api/admin/dashboard/clubs`

응답 `200` — 모임 ID 오름차순, 전체 조회:
```json
{
  "clubs": [
    { "clubId": 1, "name": "여백 모임", "bookId": 3, "bookTitle": "운수 좋은 날",
      "bookStatus": "ACTIVE", "memberCount": 2, "commentCount": 5 }
  ]
}
```

- 참여자가 0명인 모임과 삭제된 도서의 모임도 포함한다. `bookStatus`는 `ACTIVE` 또는 `DELETED`다.
- `memberCount`는 현재 `JOINED` 상태인 참여자 수다.
- `commentCount`는 현재 저장된 전체 댓글 수다. 모임 탈퇴자의 댓글도 포함하고 개인 차단 여부는
  적용하지 않는다. 삭제된 댓글은 집계하지 않으며, 댓글 본문이나 모임 참여 코드는 제공하지 않는다.
- 모임이 없으면 `{ "clubs": [] }`다.

### 관리자 통계: 책별 모임 수
`GET /api/admin/dashboard/books`

응답 `200` — 모임 수 내림차순, 동률이면 책 ID 오름차순, 전체 조회:
```json
{
  "books": [
    { "bookId": 3, "title": "운수 좋은 날", "status": "ACTIVE", "clubCount": 4 },
    { "bookId": 7, "title": "보관 도서", "status": "DELETED", "clubCount": 0 }
  ]
}
```

- 책 ID별로 집계하며 제목이 같아도 합치지 않는다. 삭제된 도서와 모임이 0개인 도서도 포함한다.
- 도서가 없으면 `{ "books": [] }`다.

### 관리자 통계: 회원별 참여 수·평균·분포
`GET /api/admin/dashboard/members`

응답 `200` — 회원 목록은 참여 모임 수 내림차순, 동률이면 회원 ID 오름차순, 전체 조회:
```json
{
  "members": [
    { "memberId": 1, "nickname": "여백", "clubCount": 2 },
    { "memberId": 2, "nickname": "독자", "clubCount": 0 }
  ],
  "averageClubCount": 1.00,
  "distribution": [
    { "clubCount": 0, "memberCount": 1 },
    { "clubCount": 2, "memberCount": 1 }
  ]
}
```

- 참여 모임은 `JOINED` 상태만 집계하며 삭제된 도서의 모임도 포함한다.
- 평균·분포는 참여 수가 0인 회원을 포함한 현재 전체 회원 기준이다. 삭제된 계정은 포함하지 않는다.
- `averageClubCount`는 총 참여 수 / 전체 회원 수를 소수 둘째 자리까지 반올림한 숫자다.
- `distribution`은 모임 수 오름차순이며 해당 회원이 없는 구간은 생략한다.
- 회원이 없으면 `{ "members": [], "averageClubCount": 0.00, "distribution": [] }`다.
- 세 통계 API는 서로 독립된 조회다. 공통 시점의 스냅샷을 보장하지 않는다. 검색·페이징·기간 입력은 없다.

### 관리자 통계 페이지
`GET /admin/dashboard`

- 별도 통계 페이지를 반환하고 기존 `/admin`과 이동 링크를 제공한다.
- HTML 페이지는 토큰 없이 열리지만 데이터는 기존 `X-Admin-Token` 인증을 거친 세 API에서만 조회한다.
- 전체 새로고침은 세 API를 각각 호출하며, 영역별 새로고침은 해당 API만 호출한다.
- 조회 중·실패한 영역의 이전 결과는 숨긴다. 실패해도 성공한 다른 영역의 결과는 유지한다.
- 각 영역은 완료 시각을 표시한다. 빈 데이터는 오류와 구분해 표시하며, 토큰 변경 시 기존 결과를 지운다.
- 입력 토큰은 페이지 메모리에서만 사용하고 브라우저 저장소에 보관하지 않는다.

### 표지 업로드 URL 발급
`POST /api/admin/book-covers/upload-url`

요청:
```json
{ "contentType": "image/jpeg", "contentLength": 245760 }
```

- 허용 형식: JPEG(`image/jpeg`), PNG(`image/png`), WebP(`image/webp`)
- 허용 크기: 1바이트 이상 5 MiB 이하
- 서버는 원본 파일명 대신 `${BOOK_COVER_S3_PREFIX}/book-covers/{uuid}.{확장자}` 형식의 새 키를
  발급한다. prefix 환경변수의 기본값은 `yeobaek`이다.

응답 `200`:
```json
{
  "coverImageKey": "yeobaek/book-covers/550e8400-e29b-41d4-a716-446655440000.jpg",
  "uploadUrl": "https://<bucket>.s3.<region>.amazonaws.com/yeobaek/book-covers/...?X-Amz-...",
  "expiresAt": "2026-08-26T12:10:00Z",
  "requiredHeaders": {
    "Content-Type": "image/jpeg",
    "Cache-Control": "public,max-age=31536000,immutable"
  }
}
```

- Presigned PUT URL의 수명은 10분이다.
- 관리자는 `requiredHeaders`를 그대로 넣어 이미지 바이트를 `PUT uploadUrl`로 전송한다.
- S3 업로드가 실패하면 도서 생성·표지 교체 API를 호출하지 않는다.
- URL 발급 API와 관리자 화면에서 형식·크기를 검사한다. 현재 S3 자체의 엄격한 최대 크기 제한은
  적용하지 않는다.

### 도서 업로드 (인제스트 규격 JSON — 문장 단위 계약)
`POST /api/admin/books`

요청:
```json
{
  "title": "운수 좋은 날",
  "publisher": "자체 제작",
  "publishedYear": 1924,
  "coverImageKey": "yeobaek/book-covers/550e8400-e29b-41d4-a716-446655440000.jpg",
  "authors": [
    { "name": "현진건", "isni": "0000 0001 2345 964X" },
    { "authorId": 12 }
  ],
  "chapters": [
    {
      "title": "1장",
      "passages": [
        {
          "sentences": [
            { "content": "새침하게 흐린 품이 눈이 올 듯하더니... " },
            { "content": "비가 오고 얼다가 만 날씨에..." }
          ]
        }
      ]
    }
  ]
}
```

필드 규칙:
- `title`: 필수, 1~100자 (공백만 불가)
- `publisher`: 선택(null 허용), 최대 100자
- `publishedYear`: 선택(null 허용), 정수 (범위 제한 없음)
- `coverImageKey`: 선택(null 허용). 표지 업로드 URL 발급 API가 반환한
  `${prefix}/book-covers/{uuid}.(jpg|png|webp)` 키만 허용한다.
- `authors`: **최소 1명.** 각 원소는 두 형태 중 하나
  - `{ "name", "isni"? }` — `name` 필수 1~100자. `isni`는 선택: 공백·하이픈 제거 후 16자리(끝자리 `X` 허용) 형식 검증(체크섬 검증 없음). ISNI가 기존 작가와 일치하면 재사용하되 유효한 이름이 다르면 `400` (`AUTHOR_NAME_MISMATCH`). 이름 누락·공백·길이 초과 등 VO 검증 실패는 기존 ISNI 여부와 무관하게 서비스 호출 전에 `400` (`INVALID_REQUEST`)로 거부한다. 일치하는 작가가 없으면 신규 생성. ISNI 없이 이름만 주면 항상 신규 생성
  - `{ "authorId" }` — 기존 작가 참조 (관리자가 작가 조회로 확인 후 기재). 미존재 시 `400` (`AUTHOR_NOT_FOUND`)
  - 같은 작가가 한 업로드에 중복 기재되면(같은 ISNI·같은 `authorId`·상호 동일 인물) `400` (`DUPLICATE_AUTHOR`)
  - 작자를 알 수 없는 저작물은 이름 `"작자 미상"`으로 등록한다 (인제스트 가이드 규칙)
- `chapters`: **최소 1개.** `title` 필수 1~100자. 각 장의 `passages`도 **최소 1개**
- `passages[].sentences`: **최소 1개.** 문단 원문만 받던 기존 형식은 지원하지 않는다.
- 문단당 문장 수에는 별도 최대 개수 제한을 두지 않는다.
- `passages[].sentences[].content`: 필수 (공백만 불가), 저장 한도 65,535바이트(TEXT) — 초과 시 `400`.
  원문의 공백과 개행을 포함해 입력 그대로 저장하며 서버는 앞뒤 공백을 제거하지 않는다. 같은 문단의
  문장들을 배열 순서로 연결하면 원래 문단 원문이 복원되어야 한다. 이미지는 데모 범위에서 제외한다
  (규격에 필드 없음)

서버 처리:
- 문단 순서(`passage.sequence`)는 **배열 등장 순서**로 서버가 책 전체 기준 1..N을 부여한다. 문장 순서(`sentence.sequence`)는 각 문단의 배열 등장 순서로 1..N을 부여한다. 두 순서 모두 dense 보장은 구성상 성립한다. 목차 순서도 등장 순서로 1..M
- `passageCount`는 서버가 자동 산출한다 (PRD 3.3)
- 제목·출판사·출판연도·작가 구성이 모두 동일한 **활성 도서**가 있으면 `400` (`DUPLICATE_BOOK`)
- 동일한 서지 정보의 삭제된 도서만 있으면 새 도서 ID로 업로드할 수 있다. 기존 모임·댓글은
  삭제된 이전 도서에 계속 연결된다.
- 업로드는 단일 트랜잭션 — 실패 시 아무것도 저장되지 않는다

응답 `201`:
```json
{
  "bookId": 3,
  "title": "운수 좋은 날",
  "coverImageUrl": "https://<public-base-url>/yeobaek/book-covers/550e8400-e29b-41d4-a716-446655440000.jpg",
  "passageCount": 30
}
```

표지 키를 생략하거나 `null`로 보내면 `coverImageUrl`도 `null`이다.

### 도서 표지 교체
`PUT /api/admin/books/{bookId}/cover`

요청:
```json
{ "coverImageKey": "yeobaek/book-covers/7b2a5027-65f5-4db8-b3b0-231e4663c90f.webp" }
```

- 먼저 표지 업로드 URL 발급 API와 S3 PUT을 성공시킨 뒤 새 키를 전달한다.
- 성공 응답: `204 No Content`.
- 존재하지 않는 도서: `400` (`BOOK_NOT_FOUND`). 삭제된 도서: `400` (`BOOK_NOT_AVAILABLE`).
- 교체된 이전 S3 객체는 즉시 삭제하지 않는다. 고아 객체 정리는 후속 작업이다.

### 도서 표지 제거
`DELETE /api/admin/books/{bookId}/cover`

- DB의 표지 키를 `null`로 바꾸며 S3 객체는 즉시 삭제하지 않는다.
- 성공 응답: `204 No Content`.
- 존재하지 않는 도서: `400` (`BOOK_NOT_FOUND`). 삭제된 도서: `400` (`BOOK_NOT_AVAILABLE`).

### 업로드된 도서 목록 조회
`GET /api/admin/books`

응답 `200` — 도서 ID 오름차순, 페이징·최대 건수 제한 없음:
```json
{
  "books": [
    {
      "bookId": 3,
      "title": "운수 좋은 날",
      "authors": [
        { "authorId": 12, "name": "현진건", "isni": "000000012345964X" }
      ],
      "publisher": "자체 제작",
      "publishedYear": 1924,
      "passageCount": 30,
      "coverImageUrl": null,
      "status": "ACTIVE"
    }
  ]
}
```

- 업로드된 도서를 한 권당 한 항목으로 반환한다. 공저자는 같은 도서의 `authors`에 함께 반환한다.
- 삭제된 도서도 포함하며 `status`는 `ACTIVE` 또는 `DELETED`다.
- `publisher`, `publishedYear`, `coverImageUrl`, 작가의 `isni`는 없으면 `null`이다.
- 도서가 없으면 `{ "books": [] }`를 반환한다.
- 기존 `GET /api/admin/authors` 계약은 유지한다.
- 미업로드 도서 관리, 보유 목록과의 자동 대조, 별도 CSV 다운로드 API는 제공하지 않는다.

### 작가 목록 조회 (업로드 전 기존 작가 확인용)
`GET /api/admin/authors`

응답 `200` — 등록순, 페이징 없음:
```json
{
  "authors": [
    {
      "authorId": 12,
      "name": "현진건",
      "isni": "000000012345964X",
      "books": [ { "bookId": 3, "title": "운수 좋은 날", "coverImageUrl": null, "status": "ACTIVE" } ]
    }
  ]
}
```
- `isni`: 정규화(공백·하이픈 제거)되어 저장된 값. 없으면 `null`.
- 삭제된 도서도 작가의 `books`에 남으며 `status: "DELETED"`로 구분한다.

### 도서 삭제
`DELETE /api/admin/books/{bookId}`

- 도서 행의 삭제 상태만 변경하는 소프트 삭제다. 목차·문단·문장·작가 연결·모임·진도·댓글과
  각 관계는 제거하지 않는다.
- 성공 응답: `204 No Content`.
- 존재하지 않는 도서: `400` (`BOOK_NOT_FOUND`).
- 이미 삭제된 도서: `400` (`BOOK_NOT_AVAILABLE`). 중복 삭제를 성공으로 간주하지 않는다.
- 삭제 복구 API는 제공하지 않는다.

### 관리자 페이지
`GET /admin` — 표지 파일 직접 업로드 + 도서 인제스트 폼 + 책 목록 조회·CSV 저장·표지 교체·제거·도서 삭제 UI (HTML, Thymeleaf). 페이지 접근 자체는 토큰이 불필요하며, 페이지 안에서 호출하는 관리자 API에 토큰을 입력해 사용한다.

- 책 목록은 한 권당 한 행이며 제목·작가명/ID·ISNI·출판사·출판연도·문단 수·상태와 표지를 표시한다.
- CSV는 화면에 조회된 목록으로 브라우저에서 생성한다. 다운로드 시 서버에 다시 조회하지 않는다.
- CSV 열은 `bookId,title,authorNames,authorIds,publisher,publishedYear,passageCount,status` 순서다.
  공저자 이름과 ID는 각각 같은 순서로 ` | `로 연결하며, 없는 출판 정보는 빈 셀로 저장한다.
- 파일명은 `book-upload-status.csv`다. UTF-8 BOM과 CRLF 행 구분을 사용하고, 텍스트 셀의
  쉼표·따옴표·줄바꿈을 이스케이프한다. 수식으로 해석될 수 있는 텍스트에는 작은따옴표를 앞에 붙인다.
  숫자 필드는 숫자로 보존한다. 조회 결과가 없으면 헤더만 저장한다.
- 조회 전·재조회 중·조회 실패 시에는 CSV 저장을 비활성화한다. 토큰 변경과 도서 변경 시 이전
  조회 결과를 무효화하고, 늦게 도착한 이전 조회 응답은 표시하지 않는다.

- 활성 작품에는 삭제 버튼을 표시한다.
- 표지가 있는 작품에는 표지 교체·제거를, 없는 작품에는 표지 추가를 제공한다.
- 삭제 전 도서명이 포함된 확인 창을 한 번 표시한다.
- 삭제된 작품은 “삭제됨” 상태를 표시하고 삭제 버튼을 비활성화한다.

## 7. Android 개발자 변경 안내

이번 변경은 공개방을 위한 **구현 예정 API 계약**이다. 신규 API와 기존 공통 API의 공개방 확장은
후속 서버 구현·배포 이후 사용할 수 있다. 상세 요청·응답과 호출 흐름은 10절을 따른다.

### 공개방 목록과 정렬

- 전체 목록은 `GET /api/public-rooms`, 방문 목록은 `GET /api/members/me/public-rooms`로 조회한다.
  두 목록 모두 전체 반환하며 페이지네이션은 사용하지 않는다.
- 전체 목록의 `sort`를 생략하면 서비스 기본 정렬을 사용한다. 현재는 `MOST_VISITED`이며,
  명시적으로 지정하면 중복을 제외한 누적 방문 회원 수 내림차순을 사용한다.
- 목록은 응답 순서대로 표시한다. 방문 목록은 최근 방문순이며 재방문하면 맨 앞으로 이동한다.
- 목록 항목의 `publicRoomId`로 공개방에 진입한다.
  공개방 인원수는 제공하지 않으며, 삭제된 도서의 공개방은 두 목록에서 제외된다.

### 공개방 독서와 방문·진도

- 공개방의 본문·진도·댓글 발견·확인·작성은 `/api/public-rooms/{publicRoomId}` 아래 API를 사용한다.
  같은 책이어도 공개방과 각 모임의 진도·댓글·확인 상태는 독립적이다.
- 독서 화면 진입 즉시 `POST /api/public-rooms/{publicRoomId}/visits`를 호출한다. 카드나 진입 확인창만
  보는 단계에서는 호출하지 않는다. 방문 기록은 진도나 최근 읽기를 갱신하지 않는다.
- 일반 뷰어 종료 시 `PUT /api/public-rooms/{publicRoomId}/progress`로 마지막 열람 문단을 저장한다.
  이때 통합 최근 읽기가 갱신된다. `myProgress=null`이면 첫 문단부터 시작한다.
- 목차는 `book.bookId`로 기존 `GET /api/books/{bookId}`를 조회한다. 본문·댓글의 응답 구조와
  새 댓글·가림·직접 확인 규칙은 기존 계약을 공개방 범위에 적용한다 (10.7~10.8절).

### 공개방·모임 통합 이어 읽기

- 신규 클라이언트는 `GET /api/members/me/recent-reading`을 사용한다. `space.type=PUBLIC_ROOM`이면
  `space.publicRoomId`, `space.type=CLUB`이면 `space.clubId`로 이동하고 `space.clubName`을 표시한다.
- 방문만 하고 진도를 저장하지 않은 공개방은 후보가 아니다. 읽기 기록이 없으면 `204 No Content`다.
- 기존 `GET /api/members/me/last-reading`은 구버전 지원을 위해 계속 모임만 반환한다.
- 공개방 정보와 통합 최근 읽기에는 삭제된 도서가 `book.status=DELETED`로 남을 수 있다.
  `ACTIVE`일 때만 이어 읽기를 허용하고, 그 외 상태는 이용 불가로 처리한다.

### 공통 API와 오류 처리

- 댓글 수정·삭제·신고는 기존 `/api/comments/{commentId}` 경로를 재사용한다. 사용자 차단·차단 해제와
  계정 삭제도 기존 API를 사용하며 공개방에 적용되는 범위는 10.9절을 따른다.
- 공개방을 찾을 수 없으면 신규 오류 `400 PUBLIC_ROOM_NOT_FOUND`, 삭제 도서의 읽기·변경 요청에는
  `400 BOOK_NOT_AVAILABLE`을 처리한다. 공개방에는 `NOT_CLUB_MEMBER`를 적용하지 않는다.
- 기존 도서·모임 API의 요청·응답은 변경하지 않는다. 이번 변경으로 기존 모임 응답 모델을
  교체하거나 구버전의 최근 읽기 호출을 변경할 필요는 없다.

## 8. 엔드포인트 요약

아래는 기존 API 목록이다. 구현 예정인 공개방 신규 API 11개는 [10.2절](#102-신규-api-목록),
기존 공통 API의 공개방 확장은 [10.9절](#109-공통-api의-공개방-적용)에 정리한다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /api/members | 회원 생성 |
| DELETE | /api/members/me | 계정 삭제 |
| GET | /api/members/me/blocks | 차단 목록 |
| PUT | /api/members/me/blocks/{memberId} | 사용자 차단 |
| DELETE | /api/members/me/blocks/{memberId} | 사용자 차단 해제 |
| GET | /api/books | 도서 목록 · 검색 (`keyword`) |
| GET | /api/books/{bookId} | 도서 상세 + 목차 |
| POST | /api/clubs | 모임 생성 |
| POST | /api/clubs/join | 참여 코드로 참여 |
| DELETE | /api/clubs/{clubId}/members/me | 모임 탈퇴 |
| GET | /api/clubs | 내 모임 목록 |
| GET | /api/clubs/{clubId} | 모임 상세 (초대 코드 · 참여자 목록) |
| GET | /api/clubs/{clubId}/passages | 본문 범위 조회 |
| PUT | /api/clubs/{clubId}/progress | 진도 갱신 |
| GET | /api/members/me/last-reading | 홈: 마지막 읽던 책 |
| GET | /api/clubs/{clubId}/comments/new-count?currentPassageId={passageId} | 탑바 새 댓글 개수 |
| GET | /api/clubs/{clubId}/commented-sentences?currentPassageId={passageId} | 댓글 문장 목록 전체 조회 |
| POST | /api/clubs/{clubId}/sentences/{sentenceId}/comment-detail-views | 댓글 상세 조회 + 직접 확인 |
| GET | /api/clubs/{clubId}/sentences/{sentenceId}/comments | 댓글 상세 조회 + 직접 확인 (deprecated 호환) |
| POST | /api/clubs/{clubId}/sentences/{sentenceId}/comments | 댓글 작성 |
| PUT | /api/comments/{commentId} | 댓글 수정 |
| DELETE | /api/comments/{commentId} | 댓글 삭제 |
| POST | /api/comments/{commentId}/reports | 댓글 신고 |
| POST | /api/admin/book-covers/upload-url | (관리자) S3 표지 업로드 URL 발급 |
| POST | /api/admin/books | (관리자) 도서 업로드 |
| GET | /api/admin/books | (관리자) 업로드된 전체 도서 목록 조회 |
| PUT | /api/admin/books/{bookId}/cover | (관리자) 기존 도서 표지 교체 |
| DELETE | /api/admin/books/{bookId}/cover | (관리자) 기존 도서 표지 제거 |
| DELETE | /api/admin/books/{bookId} | (관리자) 도서 소프트 삭제 |
| GET | /api/admin/authors | (관리자) 작가 목록 조회 |
| GET | /api/admin/dashboard/clubs | (관리자) 모임 목록·댓글 수 |
| GET | /api/admin/dashboard/books | (관리자) 책별 모임 수 |
| GET | /api/admin/dashboard/members | (관리자) 회원별 참여 수·평균·분포 |
| GET | /admin | (관리자) 관리자 페이지 (HTML) |
| GET | /admin/dashboard | (관리자) 통계 대시보드 (HTML) |

## 9. App Store 배포 전 후속 과제

이번 계약은 계정 삭제, 댓글 신고, 사용자 차단의 클라이언트 API만 정의한다. 다음 항목은 이번
API 계약에서 확정하지 않으며 App Store 배포 전에 별도 정책과 구현으로 준비한다.

- 부적절한 댓글이 게시되는 것을 막는 콘텐츠 필터링
- 사용자가 운영자에게 연락할 수 있는 공개 연락처
- 접수된 신고의 심사 기준, 적시 대응, 위반 댓글 제거와 사용자 제재 절차

계정 삭제 시 데이터를 즉시 제거하는 현재 계약의 법적 적합성과 삭제 대신 보존·익명화할 수 있는
범위는 일정 여유가 생기면 재검토한다. 후속 검토 전까지는 이 문서의 즉시 삭제 계약을 따른다.

참고:

- [Apple App Review Guidelines 1.2 — User-Generated Content](https://developer.apple.com/app-store/review/guidelines/)
- [Apple — Offering account deletion in your app](https://developer.apple.com/support/offering-account-deletion-in-your-app/)

## 10. 공개방 선행 계약 (구현 예정)

기능 범위는 [P-156 공개방](https://linear.app/yeobaek/issue/P-156/공개방)과 2026-09-23 API 계약
논의를 따른다. [P-269 구조 개편](https://linear.app/yeobaek/issue/P-269/스키마-and-코드-구조-개편)과
독립적인 HTTP 계약이며, 테이블·모듈·클래스·집계 저장 방식은 규정하지 않는다.

### 10.1 범위와 공통 규칙

- 이용 가능한 책마다 공개방을 하나 제공한다. 공개방에는 모임 이름·참여 코드·가입/탈퇴 상태가 없다.
  생성·삭제·가입/탈퇴 API는 제공하지 않는다.
- 0절의 `X-Member-Id`, JSON, 시각 형식, 오류 응답 규약을 적용한다. 여기서 자유로운 접근은
  유효한 회원이 모임 가입 없이 이용할 수 있다는 뜻이다. 비회원 접근은 추가하지 않는다.
- 공개방의 진도·댓글·댓글 확인 상태는 같은 책의 다른 모임과 분리한다. 사용자 차단은 서비스
  전체에 적용한다. 공개방의 방문 기록은 읽기·댓글 기능을 사용할 수 있는 권한의 조건이 아니다.
- 본문·목차, 이어 읽기·진도, 댓글 작성·수정·삭제, 새 댓글·직접 확인·미래 문장 가림,
  신고·차단을 제공한다. 리뷰와 공개방 인원수 표시는 포함하지 않는다.
- `publicRoomId`는 책·모임 ID와 별개의 식별자다. 클라이언트는 ID를 서로 변환하거나 값이 같다고
  가정하지 않는다. 신규 계약의 ID는 양의 64비트 정수이며, 순서·내부 저장 구조를 의미하지 않는다.
- 전체 공개방과 방문한 공개방은 모두 **전체 반환**한다. `page`, `size`, `cursor`, `nextCursor`를
  계약에 추가하지 않는다. 필요성이 확인되면 기존 전체 반환을 유지하면서 페이지네이션 계약을 별도로 추가한다.
- 인원수·누적 방문 회원 수 필드는 응답에 포함하지 않는다. 댓글의 `commentCount` 등 독서에
  필요한 기존 집계 필드는 유지한다.
- 이 절에서 별도로 바꾸지 않은 독서·댓글의 요청·응답·행동 규칙은 4·5절의 공개 계약을 따른다.
  기존 절의 DB·트랜잭션·저장 행 등에 대한 구현 설명은 공개방의 구현 제약으로 가져오지 않는다.

### 10.2 신규 API 목록

아래 API는 모두 구현 예정이다. 기존 모임 경로는 유지한다.

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET | `/api/public-rooms` | 전체 공개방 목록, 선택 쿼리 `sort` |
| GET | `/api/members/me/public-rooms` | 최근 방문순 공개방 목록 |
| GET | `/api/public-rooms/{publicRoomId}` | 공개방 정보·내 진도 |
| POST | `/api/public-rooms/{publicRoomId}/visits` | 방문 기록 |
| GET | `/api/public-rooms/{publicRoomId}/passages` | 본문 범위, 필수 쿼리 `from`, `to` |
| PUT | `/api/public-rooms/{publicRoomId}/progress` | 진도 저장 |
| GET | `/api/public-rooms/{publicRoomId}/comments/new-count` | 새 댓글 수, 필수 쿼리 `currentPassageId` |
| GET | `/api/public-rooms/{publicRoomId}/commented-sentences` | 댓글 문장 목록, 필수 쿼리 `currentPassageId` |
| POST | `/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views` | 댓글 상세·직접 확인 |
| POST | `/api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments` | 댓글 작성 |
| GET | `/api/members/me/recent-reading` | 공개방·모임 통합 최근 읽기 |

### 10.3 응답 데이터

아래 이름은 JSON 구조를 설명하기 위한 명칭이며 서버 내부 타입을 지정하지 않는다.
표의 필드는 모두 존재한다. `null` 허용을 명시한 필드만 `null`일 수 있다.

| 구조 | 필드 | 타입·의미 |
|---|---|---|
| 도서 요약 | `bookId` | 도서 ID |
| 도서 요약 | `title` | 문자열, 제목 |
| 도서 요약 | `authors` | 문자열 배열, 작가명 |
| 도서 요약 | `coverImageUrl` | 문자열 또는 `null`; 없으면 클라이언트 기본 표지 사용 |
| 도서 요약 | `passageCount` | 정수, 전체 문단 수 |
| 도서 요약 | `status` | 0절의 확장 가능한 도서 상태. `ACTIVE`일 때만 읽기 허용 |
| 진도 | `lastReadPassageSequence` | 정수, 최근 열람 문단의 도서 전체 순서 |
| 진도 | `progressRate` | 0~100 정수. `lastReadPassageSequence / passageCount * 100` 반올림 |
| 진도 | `lastReadAt` | 0절 시각 형식의 문자열, 마지막 진도 저장 시각 |
| 공개방 요약 | `publicRoomId` | 공개방 ID |
| 공개방 요약 | `book` | 도서 요약 |
| 공개방 요약 | `myProgress` | 진도 또는 `null`; 한 번도 진도를 저장하지 않았으면 `null` |

방문만 해서는 진도를 만들지 않는다. 공개방에 진입할 때 `myProgress=null`이면 첫 문단부터
시작하고, 값이 있으면 `lastReadPassageSequence`부터 이어 읽는다.

### 10.4 전체 공개방 목록과 정렬 확장

`GET /api/public-rooms?sort=MOST_VISITED`

| 쿼리 | 필수 | 계약 |
|---|---|---|
| `sort` | 아니오 | 생략하면 서비스 기본 정렬. 현재 명시적으로 지원하는 값은 `MOST_VISITED` |

- 현재 기본 정렬과 `MOST_VISITED`는 **중복을 제외한 누적 방문 회원 수 내림차순**이다.
  한 회원의 같은 방 재방문은 순위 계산에 사용하는 회원 수를 늘리지 않는다.
- 생략한 `sort`의 기본 기준은 서비스 정책에 따라 향후 바뀔 수 있다. 명시한 `MOST_VISITED`의
  의미를 주간 독서순이나 주간 댓글순으로 바꾸지는 않는다.
- 향후 정렬은 별도 `sort` 값으로 추가한다. 최근 7일 독서순·댓글순의 값 이름과 집계 정책은
  아직 지원 계약에 포함하지 않는다. 정렬이 추가되어도 같은 목록 경로와 응답 구조를 사용한다.
- 빈 문자열 또는 지원하지 않는 `sort`는 `400 INVALID_REQUEST`다. 무시하거나 기본값으로 대체하지 않는다.
- 모든 이용 가능한 책의 공개방을 반환한다. 방문한 공개방도 포함하며, 삭제 도서의 공개방은 제외한다.
- 같은 누적 방문 회원 수 사이의 상대 순서는 보장하지 않는다. 클라이언트는 자체 재정렬하지 않고
  서버가 반환한 배열 순서를 표시한다.

응답 `200`:

```json
{
  "publicRooms": [
    {
      "publicRoomId": 12,
      "book": {
        "bookId": 1,
        "title": "운수 좋은 날",
        "authors": ["현진건"],
        "coverImageUrl": null,
        "passageCount": 312,
        "status": "ACTIVE"
      },
      "myProgress": null
    }
  ]
}
```

향후 정렬 선택 UI에서는 서버가 지원한다고 명세된 `sort` 값만 요청한다.
결과가 없으면 `publicRooms: []`다.

목록 항목의 `publicRoomId`로 공개방에 진입한다.
기존 `GET /api/books`와 도서 상세 응답은 변경하지 않는다.

### 10.5 내가 방문한 공개방 목록

`GET /api/members/me/public-rooms`

요청 본문과 쿼리 파라미터는 없다. 응답 `200`:

```json
{
  "publicRooms": [
    {
      "publicRoomId": 12,
      "book": {
        "bookId": 1,
        "title": "운수 좋은 날",
        "authors": ["현진건"],
        "coverImageUrl": null,
        "passageCount": 312,
        "status": "ACTIVE"
      },
      "myProgress": null,
      "lastVisitedAt": "2026-09-23T14:30:00"
    }
  ]
}
```

- 각 항목은 공개방 요약과 `lastVisitedAt`(문자열, `null` 불가)이다. 방마다 최대 한 항목만 반환한다.
- `lastVisitedAt` 내림차순이며 재방문하면 맨 앞으로 이동한다. 같은 시각끼리의 상대 순서는 보장하지 않는다.
- 삭제 도서의 공개방은 숨긴다. 남은 항목이 없으면 `publicRooms: []`다.
- 조회는 방문 시각·진도·댓글 확인 상태를 변경하지 않는다.

### 10.6 공개방 정보와 방문 기록

#### 공개방 정보

`GET /api/public-rooms/{publicRoomId}`

응답 `200`은 공개방 요약에 `lastVisitedAt`을 추가한 객체다.

```json
{
  "publicRoomId": 12,
  "book": {
    "bookId": 1,
    "title": "운수 좋은 날",
    "authors": ["현진건"],
    "coverImageUrl": null,
    "passageCount": 312,
    "status": "ACTIVE"
  },
  "myProgress": null,
  "lastVisitedAt": null
}
```

- 방문 기록이 없으면 `lastVisitedAt=null`이다. 방문 여부와 관계없이 조회할 수 있다.
- 목차는 `book.bookId`로 기존 `GET /api/books/{bookId}`를 호출해 얻는다.
- 삭제된 책도 이미 알고 있는 공개방 ID로 조회하면 식별 정보·저장 진도와 `book.status=DELETED`를
  반환한다. 이 조회는 읽기 허용을 뜻하지 않는다.
- 조회는 방문 기록·진도·댓글 확인 상태를 변경하지 않는다.

#### 방문 기록

`POST /api/public-rooms/{publicRoomId}/visits`

요청 본문은 없다. 응답 `204 No Content`.

- 클라이언트는 공개방 독서 화면에 진입하면 즉시 호출한다. 목록 카드·진입 확인창을 보는 것만으로
  호출하지 않는다. 통합 최근 읽기를 통해 공개방에 다시 진입할 때도 호출한다.
- 성공하면 최초 방문은 방문 목록에 추가하고, 재방문은 `lastVisitedAt`을 이번 방문 시각으로 갱신한다.
- 재요청·동시 최초 요청으로 방문 목록 항목이나 누적 방문 회원 수가 중복 증가하지 않는다.
  다만 성공한 호출마다 방문 시각이 갱신되므로 전체 응답 상태가 불변인 멱등 요청으로 취급하지 않는다.
- 응답 유실로 재시도해도 방문자는 중복 집계되지 않는다. 이미 성공한 요청의 방문 기록은 응답
  유실만으로 취소되지 않는다.
- 방문은 진도·`lastReadAt`·댓글 확인 상태를 변경하지 않는다. 방문만 한 공개방은 통합 최근 읽기의 후보가 아니다.
- 삭제 도서에는 `400 BOOK_NOT_AVAILABLE`을 반환하며 방문 시각을 갱신하지 않는다.

### 10.7 본문과 진도

#### 본문 범위 조회

`GET /api/public-rooms/{publicRoomId}/passages?from={sequence}&to={sequence}`

`from`, `to`는 필수 정수다. `1 <= from <= to`, `to - from + 1 <= 100`을 만족해야 하며,
위반하면 `400 INVALID_REQUEST`다. 양 끝을 포함하는 범위에서 존재하는 문단을 순서대로 반환한다.
마지막 문단을 넘는 구간은 제외하고, 해당 범위에 문단이 없으면 `passages: []`다.

응답 `200`의 구조는 4절 본문 범위 조회와 같다. `passages[]`는 `passageId`, `sequence`,
`chapterId`, `sentences[]`를 포함하고, 각 문장은 `sentenceId`, 문단 내 `sequence`, `content`,
`commentCount`를 포함한다. 문자열의 공백·개행은 보존한다. `commentCount`는 **이 공개방에서
요청 회원에게 보이는 댓글 수**이며 모임 댓글이나 차단한 작성자의 댓글을 섞지 않는다.

본문 조회는 방문·진도·댓글 확인 상태를 변경하지 않는다.

#### 진도 갱신

`PUT /api/public-rooms/{publicRoomId}/progress`

요청:

```json
{ "passageId": 1042 }
```

응답 `200`:

```json
{
  "lastReadPassageSequence": 42,
  "progressRate": 13,
  "lastReadAt": "2026-09-23T14:35:00"
}
```

- 기존 모임처럼 일반 뷰어 종료 시 마지막으로 화면에 표시한 문단으로 호출한다. 최근 열람 위치로
  덮어쓰며 앞부분 재열람을 저장하면 진도율이 후퇴할 수 있다.
- 성공하면 이 공개방의 내 진도와 `lastReadAt`만 갱신하고 통합 최근 읽기 선정에 반영한다.
  모임 진도·방문 시각·댓글 확인 상태는 변경하지 않는다.
- 본문 이동·목차 이동·댓글 문장의 “보러 가기”와 원래 위치로 돌아가기도 4절의 일반 뷰어 규칙을
  따른다. 서버에 이전 진도를 보관·복원하는 별도 API를 추가하지 않는다.
- `passageId` 누락·형식 오류는 `400 INVALID_REQUEST`, 없거나 이 책에 속하지 않는 문단은
  `400 PASSAGE_NOT_FOUND`다.

### 10.8 댓글 발견·확인·작성

모든 집계·목록·직접 확인은 해당 공개방의 댓글 중 요청 회원에게 보이는 댓글만 대상으로 한다.
진도 경계는 저장 진도가 아니라 요청의 `currentPassageId`다. 이 파라미터는 진도와 방문 시각을 변경하지 않는다.
아래 네 API 모두 없는 공개방에는 `400 PUBLIC_ROOM_NOT_FOUND`, 삭제 도서에는
`400 BOOK_NOT_AVAILABLE`을 반환한다. 모임 소속 조건은 적용하지 않는다.

| 기능과 경로 | 요청 | 성공 응답 및 5절과 공유하는 규칙 |
|---|---|---|
| `GET /api/public-rooms/{publicRoomId}/comments/new-count` | 필수 쿼리 `currentPassageId` | `200 {"newCommentCount": 3}`. 현재 문단까지의 보이는 `NEW` 댓글 수. 상태 변경 없음 |
| `GET /api/public-rooms/{publicRoomId}/commented-sentences` | 필수 쿼리 `currentPassageId` | `200 {"commentedSentences": [...]}`. 5절 댓글 문장 목록의 모든 필드·정렬·가림·전체 반환 규칙 적용 |
| `POST /api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comment-detail-views` | 본문 없음 | `200 {"comments": [...]}`. 작성일 오름차순의 댓글 객체와 직접 확인 처리 |
| `POST /api/public-rooms/{publicRoomId}/sentences/{sentenceId}/comments` | `{"content": "이 문장에서 멈칫했어요."}`. `content` 1~1000자 | `201`, 생성한 댓글 객체 하나. 작성자에게 즉시 `VIEWED` |

댓글 객체는 아래와 같다. `updatedAt`만 `null`을 허용한다.

```json
{
  "commentId": 7,
  "memberId": 2,
  "nickname": "지수",
  "content": "이 문장에서 멈칫했어요.",
  "createdAt": "2026-09-23T14:30:00",
  "updatedAt": null,
  "mine": false
}
```

- 댓글 문장 항목은 5절과 동일하게 `sentenceId`, `content`, `passageId`, `passageSequence`,
  `sentenceSequence`, `future`, `commentCount`, `unreadCommentCount`, `contentVisibility`,
  `latestCommentCreatedAt`을 모두 포함한다.
- 댓글 문장 목록은 보이는 댓글이 있는 책 전체의 문장을 반환한다. 새 댓글 문장 → 미래 문장 →
  확인한 문장 순이며, 그룹 안에서는 `latestCommentCreatedAt` 내림차순, 동률이면 `sentenceId`
  내림차순이다. 받은 목록을 탐색 동안 유지하고 새 조회에서 이후 변경을 반영한다.
- `future=true`이고 `unreadCommentCount>0`일 때 `contentVisibility=REVEAL_REQUIRED`, 그 외에는
  `VISIBLE`이다. 가림 상태여도 `content`는 포함한다. 클라이언트는 이 정책 값을 따르며, 화면에서
  가림을 해제하는 것만으로 서버의 댓글 확인 상태는 바뀌지 않는다.
- 회원이 직접 확인하지 않은 과거 댓글도 `NEW`다. 상세 조회는 응답 대상 댓글을 `VIEWED`로
  전환한다. 상세 확인과 응답 대상 결정은 하나의 성공 결과로 처리하며, 실패 시 확인 상태를 변경하지 않는다.
  처리가 성공한 뒤 응답만 유실되면 확인 상태는 유지된다. 댓글 수정은 확인 상태와 `createdAt`을 바꾸지 않는다.
- 목록에 댓글이 없으면 해당 배열은 빈 배열이다. 모든 작성자를 차단했거나 목록 조회 후 댓글이
  삭제된 경우에도 문장 상세는 `200 {"comments": []}`를 반환할 수 있다.
- `currentPassageId` 누락·형식 오류·없거나 다른 책의 문단이면 `400 INVALID_REQUEST`다.
  경로의 `sentenceId`가 없거나 다른 책 소속이면 `400 SENTENCE_NOT_FOUND`다.
- 공개방에는 구버전 호환용 `GET .../sentences/{sentenceId}/comments`를 새로 추가하지 않는다.

### 10.9 공통 API의 공개방 적용

아래 확장은 공개방 구현 이후 적용한다. 기존 모임 댓글의 요청·응답·권한은 유지한다.

| 기존 API | 공개방에 적용할 계약 |
|---|---|
| `PUT /api/comments/{commentId}` | 요청 `{"content": "수정된 내용"}`, 1~1000자. 본인 댓글만 수정, `200` 댓글 객체 |
| `DELETE /api/comments/{commentId}` | 본인 댓글만 삭제, `204 No Content` |
| `POST /api/comments/{commentId}/reports` | 본문 없음. 보이는 타인 댓글 신고, 반복 신고도 `204 No Content` |
| `GET /api/members/me/blocks` | 기존 차단 목록 계약 그대로 사용 |
| `PUT /api/members/me/blocks/{memberId}` | 기존 서비스 전체 단방향 차단을 공개방 댓글에도 적용 |
| `DELETE /api/members/me/blocks/{memberId}` | 기존 차단 해제 계약 그대로 사용 |
| `DELETE /api/members/me` | 공개방 방문·진도·작성 댓글·확인 상태도 기존 계정 삭제 완료 시점에 함께 제거 |

- `commentId`는 모임·공개방 전체에서 댓글 하나를 유일하게 식별한다. 이는 단일 테이블이나
  공통 내부 모델을 요구하지 않는다. 수정·삭제·신고에 공간 종류나 ID를 추가로 보내지 않는다.
- 공개방 댓글에는 모임 소속 조건을 적용하지 않는다. 본인 댓글의 수정·삭제는 회원과 도서가
  유효하면 가능하다. 모임 댓글에는 기존 모임 소속 조건을 계속 적용한다.
- 없는 댓글의 수정·삭제·신고는 `400 COMMENT_NOT_FOUND`, 남의 댓글 수정·삭제는
  `403 NOT_COMMENT_OWNER`, 본인 댓글 신고는 `400 CANNOT_REPORT_OWN_COMMENT`다.
- 차단한 작성자의 댓글은 목록·집계에서 제외하고, 해당 댓글의 신고에는 `400 COMMENT_NOT_FOUND`를
  반환한다. 차단은 단방향이며 상대방의 댓글 작성·수정·삭제 권한을 바꾸지 않는다.
- 신고만으로 댓글을 숨기지 않는다. 같은 회원의 같은 댓글 재신고는 중복 접수하지 않는다.
- 계정 삭제 후 해당 회원의 방문은 인기순 집계에서도 제외한다. 공개방 자체와 다른 회원의
  데이터는 유지한다. 즉시 삭제의 범위와 완료 시점은 기존 계정 삭제 계약을 확장한 것이다.

### 10.10 공개방·모임 통합 최근 읽기

`GET /api/members/me/recent-reading`

공개방과 현재 참여 중인 모임에서 저장한 진도 중 `lastReadAt`이 가장 최근인 기록 하나를 반환한다.
방문만 하고 진도를 저장하지 않은 공개방은 후보가 아니며, 후보가 없으면 `204 No Content`다.
동일한 최신 시각의 후보가 여러 개면 그중 하나를 반환하며 동률 선택 기준에 의존하지 않는다.

공개방 응답 `200`:

```json
{
  "space": { "type": "PUBLIC_ROOM", "publicRoomId": 12 },
  "book": {
    "bookId": 1,
    "title": "운수 좋은 날",
    "authors": ["현진건"],
    "coverImageUrl": null,
    "passageCount": 312,
    "status": "ACTIVE"
  },
  "lastReadPassageSequence": 42,
  "progressRate": 13,
  "lastReadAt": "2026-09-23T14:35:00"
}
```

모임인 경우 같은 응답 구조의 `space`가 다음 객체다.

```json
{ "type": "CLUB", "clubId": 1, "clubName": "교환독서 1기" }
```

| `space.type` | 필수 필드 | 포함하지 않는 필드 |
|---|---|---|
| `PUBLIC_ROOM` | `publicRoomId` | `clubId`, `clubName` |
| `CLUB` | `clubId`, `clubName` | `publicRoomId` |

- `space`, `book`, 진도 필드는 모두 필수이며 `null`이 아니다. `book.coverImageUrl`만 `null`일 수 있다.
  Android는 `space.type`을 먼저 판별하여 해당 공간의 API·화면으로 이동한다.
- 탈퇴한 모임의 기록은 제외한다. 가장 최근 기록의 도서가 삭제됐더라도 건너뛰지 않고
  `book.status=DELETED`와 저장된 진도를 반환한다. Android는 이어 읽기를 막는다.
- 기존 `GET /api/members/me/last-reading`은 **참여 중인 모임만** 대상으로 기존 응답을 반환한다.
  공개방 진도를 저장해도 기존 API에 공개방 기록이나 신규 필드를 섞지 않는다.

### 10.11 삭제 도서와 오류

| 영역 | 도서 삭제 후 동작 |
|---|---|
| 전체·방문 공개방 목록 | 해당 공개방을 숨김 |
| 이미 아는 ID로 공개방 정보 조회 | 식별 정보·저장 진도와 `book.status=DELETED` 반환 |
| 통합 최근 읽기 | 가장 최근이면 기록과 `DELETED` 유지, 다른 책으로 대체하지 않음 |
| 방문 기록 | `400 BOOK_NOT_AVAILABLE`, 방문 시각 갱신 없음 |
| 목차·본문·진도·댓글 조회/작성/수정/삭제/신고 | `400 BOOK_NOT_AVAILABLE` |

도서 삭제만으로 기존 댓글·진도·방문 기록을 지우지는 않는다. 삭제 도서에서 읽기·댓글 기능을
다시 제공하는 계약은 이번 범위에 포함하지 않는다.

| 조건 | HTTP | `code` |
|---|---|---|
| 회원 헤더 누락·형식 오류, 입력 필드·쿼리·경로 ID 형식 오류 | 400 | `INVALID_REQUEST` |
| 존재하지 않는 요청 회원 | 400 | `MEMBER_NOT_FOUND` |
| 존재하지 않는 공개방 | 400 | `PUBLIC_ROOM_NOT_FOUND` |
| 삭제 도서에서 위 표의 읽기·변경 동작 시도 | 400 | `BOOK_NOT_AVAILABLE` |
| 진도 요청의 문단이 없거나 다른 책 소속 | 400 | `PASSAGE_NOT_FOUND` |
| 댓글 발견 요청의 현재 문단이 없거나 다른 책 소속 | 400 | `INVALID_REQUEST` |
| 댓글 상세·작성 요청의 문장이 없거나 다른 책 소속 | 400 | `SENTENCE_NOT_FOUND` |
| 없는 댓글 또는 신고자에게 보이지 않는 댓글 | 400 | `COMMENT_NOT_FOUND` |
| 본인 댓글 신고 | 400 | `CANNOT_REPORT_OWN_COMMENT` |
| 타인 댓글 수정·삭제 | 403 | `NOT_COMMENT_OWNER` |

공개방 경로에는 `NOT_CLUB_MEMBER`를 적용하지 않는다. 오류 본문은 0절의 `{code, message}`이며
`message`는 클라이언트 분기 기준이 아니다. 여러 오류 조건을 동시에 만족할 때 코드의 우선순위는
별도 보장하지 않는다.

### 10.12 Android 호출 흐름

1. 홈에서 통합 최근 읽기를 조회한다. 공개방 탭에서는 방문 목록과 전체 목록을 조회한다.
   세 조회는 서로 독립적이며 병렬 호출할 수 있다.
2. 목록에서 선택한 항목의 `publicRoomId`로 공개방에 진입한다.
3. 공개방 독서 화면에 진입하면 방문을 기록한다. 공개방 정보의 내 진도와 도서 상세의 목차,
   필요한 범위의 본문을 조회한다. 방문 기록은 읽기 권한이나 진도 저장을 대신하지 않는다.
4. 독서 중 현재 문단을 댓글 발견 API에 전달한다. 댓글 문장을 선택하면 상세 확인 API를 호출한다.
5. 일반 뷰어 종료 시 마지막으로 표시한 문단으로 공개방 진도를 저장한다. 이후 통합 최근 읽기에 반영된다.

모든 신규 `GET`은 방문·진도·댓글 확인 상태를 변경하지 않는다. 방문 직후 본문 조회가 실패해도
이미 성공한 방문 기록은 유지된다. 방문 요청의 응답을 받지 못하면 기록 반영 여부가 불확실할 수
있으며, 재요청은 같은 회원을 중복 방문자로 집계하지 않는다.
