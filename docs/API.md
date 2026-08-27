# 여백 API 명세 (계약)

> 기반 문서: [`PRD.md`](PRD.md) · 상태: **구현 및 연동 기준**
> 현재 구현은 Swagger(springdoc)가 런타임에 생성하는 OpenAPI 스펙(`/v3/api-docs`)과 Swagger UI(`/docs`)에서 확인한다.
> 이 문서는 안드로이드와 합의한 API 계약의 기준이다. 계약 변경이 필요하면 반드시 이 문서를 먼저 갱신하고 합의한 뒤 구현과 Swagger 문서를 함께 맞춘다.

## 0. 공통 규약

- Base path: `/api` (호스트는 배포 후 공유)
- 요청/응답 본문: JSON (UTF-8), 필드는 camelCase
- **회원 식별**: 회원 생성과 공개 사전신청을 제외한 모든 API는 `X-Member-Id: {회원ID}` 헤더 필수.
  클라이언트는 회원 생성 시 받은 ID를 기기에 저장하고 인터셉터로 전 요청에 첨부한다.
- 시각 필드: ISO-8601 (`2026-08-05T14:30:00`)
- 에러 응답: 상태 코드 + `{ "code": "CLUB_NOT_FOUND", "message": "사람이 읽을 수 있는 설명" }`
  - 일반 앱 API는 기존 3개 상태를 유지하고, 공개 사전신청에 `409`와 `429`를 추가한다:
    - `401` 인증 실패 (관리자 토큰 누락·불일치)
    - `403` 권한 없음 (모임 미소속, 남의 댓글 등)
    - `400` 그 외 모든 클라이언트 오류. 상세 원인은 바디의 `code`로 구분한다
    - `409` 이미 등록된 사전신청 이메일
    - `429` IP별 사전신청 요청 제한 초과
  - `X-Member-Id` 누락 또는 존재하지 않는 회원: `400`
  - `message`는 표시용이며 계약이 아니다. 클라이언트 분기는 `code`로만 한다

### 에러 코드

| code | 상태 | 의미 |
|---|---|---|
| `INVALID_REQUEST` | 400 | 본문·파라미터·헤더 형식 오류, 필드 검증 실패 전반 |
| `MEMBER_NOT_FOUND` | 400 | `X-Member-Id`가 가리키는 회원 없음 |
| `BOOK_NOT_FOUND` | 400 | 대상 도서 없음 |
| `BOOK_NOT_AVAILABLE` | 400 | 대상 도서가 삭제되어 더 이상 이용할 수 없음 |
| `CLUB_NOT_FOUND` | 400 | 대상 모임 없음 |
| `JOIN_CODE_NOT_FOUND` | 400 | 참여 코드에 해당하는 모임 없음 |
| `PASSAGE_NOT_FOUND` | 400 | 대상 본문 없음 |
| `COMMENT_NOT_FOUND` | 400 | 대상 댓글 없음 |
| `AUTHOR_NOT_FOUND` | 400 | (관리자) `authorId`가 가리키는 작가 없음 |
| `DUPLICATE_AUTHOR` | 400 | (관리자) 한 업로드 안에 같은 작가 중복 기재 |
| `AUTHOR_NAME_MISMATCH` | 400 | (관리자) ISNI로 찾은 기존 작가와 요청의 이름 불일치 |
| `DUPLICATE_BOOK` | 400 | (관리자) 제목·출판사·출판연도·작가 구성이 동일한 도서 존재 |
| `PRE_REGISTRATION_ALREADY_EXISTS` | 409 | 정규화한 이메일로 이미 사전신청이 등록됨 |
| `RATE_LIMIT_EXCEEDED` | 429 | 같은 IP에서 1분 동안 허용된 사전신청 요청 횟수 초과 |
| `NOT_CLUB_MEMBER` | 403 | 모임에 참여하지 않은 회원의 접근 |
| `NOT_COMMENT_OWNER` | 403 | 본인 댓글이 아닌 수정·삭제 시도 |
| `UNAUTHORIZED` | 401 | 관리자 토큰 누락·불일치 (서버에 토큰 미설정 시 관리자 API 전부 이 응답) |

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
| 댓글 조회·작성·수정·삭제 | 가능 | 데이터를 보존하고 `BOOK_NOT_AVAILABLE` |
| 관리자 작가·작품 목록 | `status=ACTIVE`로 표시 | 삭제 도서도 `status=DELETED`로 표시 |
| 동일 도서 업로드 | 활성 중복이면 차단 | 삭제된 중복만 있으면 새 ID로 허용 |

## 0.1 공개 사전신청

### 이메일 사전신청
`POST /api/pre-registrations` — `X-Member-Id` 헤더 불필요

요청:
```json
{ "email": "reader@example.com" }
```

- `email`: 필수, 앞뒤 공백 제거 후 소문자로 저장한다. 정규화 후 최대 254자이며 일반적인
  이메일 형식이어야 한다.
- 운영 랜딩 `https://yeobaek-landing-hypothesis.vercel.app`에서만 CORS 요청을 허용한다.
  운영 환경변수 `LANDING_CORS_ALLOWED_ORIGIN`으로 허용 출처를 교체할 수 있다.

응답 `201`, 본문 없음.

- 같은 정규화 이메일이 이미 있으면 저장하지 않고 `409`
  (`PRE_REGISTRATION_ALREADY_EXISTS`)를 반환한다.
- IP별 고정 1분 창에서 `POST` 요청을 5회까지 허용한다. 6번째 요청부터 창이 끝날 때까지
  `429` (`RATE_LIMIT_EXCEEDED`)를 반환하며, CORS `OPTIONS` preflight는 횟수에 포함하지 않는다.
- 형식이 올바르지 않으면 `400` (`INVALID_REQUEST`)을 반환한다.
- 이메일은 여백 앱 출시 및 사전신청 안내에만 사용하며, 출시 안내 발송 후 30일 또는
  동의 철회 시까지 보관한다.

## 1. 회원

### 회원 생성
`POST /api/members` — 헤더 불필요 (최초 진입)

요청:
```json
{ "nickname": "민서" }
```
- `nickname`: 1~20자, 공백만은 불가. 중복 허용.

응답 `201`:
```json
{ "memberId": 1, "nickname": "민서" }
```

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
    { "memberId": 1, "nickname": "민서", "mine": true },
    { "memberId": 2, "nickname": "지수", "mine": false }
  ]
}
```
- `myProgress`: 내 모임 목록과 동일 형태. 아직 읽기 시작 전이면 `null`.
- `members`: 참여 중인 회원만 참여 시각 오름차순으로 반환한다. `mine`은 요청자(`X-Member-Id`) 본인 여부 (댓글의 `mine`과 동일 규약).
- 모임 미소속 회원: `403` (`NOT_CLUB_MEMBER`). 존재하지 않는 모임: `400` (`CLUB_NOT_FOUND`).
- 삭제된 도서의 기존 모임도 상세 정보를 반환한다. 모임·참여자·초대 코드·저장된 진도는
  유지하고 `book.status`를 `DELETED`로 반환하지만, 해당 코드로 신규 참여하는 요청은 차단한다.

## 4. 읽기 · 진도

읽기는 항상 모임 맥락에서 이루어진다 (진도·댓글이 모임 단위이므로).

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
      "content": "새침하게 흐린 품이 눈이 올 듯하더니...",
      "commentCount": 3
    }
  ]
}
```
- `content`는 항상 존재한다 (2026-08-07 개정 — 이미지 미제공 결정으로 `imageUrl` 필드 제거). `commentCount`는 이 모임의 댓글 수.
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
- 클라이언트는 문단이 화면에 노출되는 시점에 호출한다 (배치·디바운스는 클라이언트 재량).
- 모임 도서가 삭제된 경우 저장된 진도를 변경하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 홈 — 마지막으로 읽던 책
`GET /api/members/me/last-reading`

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

## 5. 댓글

### 문단의 댓글 목록
`GET /api/clubs/{clubId}/passages/{passageId}/comments`

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
- `mine`: 요청자(`X-Member-Id`) 본인 작성 여부. `updatedAt`: 수정된 적 없으면 `null`.
- 탈퇴한 작성자의 댓글도 닉네임과 내용을 변경하지 않고 일반 댓글과 동일하게 반환한다.
- 모임 도서가 삭제된 경우 보존된 댓글을 반환하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 댓글 작성
`POST /api/clubs/{clubId}/passages/{passageId}/comments`

요청:
```json
{ "content": "이 문장에서 멈칫했어요." }
```
- `content`: 1~1000자.

응답 `201`: 댓글 목록의 원소와 동일 형태 (`mine: true`).

- 모임 도서가 삭제된 경우 댓글을 저장하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 댓글 수정
`PUT /api/comments/{commentId}`

요청:
```json
{ "content": "수정된 내용" }
```

응답 `200`: 댓글 목록의 원소와 동일 형태. 본인 댓글이 아니거나 작성자가 해당 모임에서 탈퇴한 상태면 `403`.

- 댓글이 연결된 도서가 삭제된 경우 기존 내용을 변경하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.

### 댓글 삭제
`DELETE /api/comments/{commentId}`

응답 `204`. 본인 댓글이 아니거나 작성자가 해당 모임에서 탈퇴한 상태면 `403`. 하드 삭제 (PRD 3.5).

- 댓글이 연결된 도서가 삭제된 경우 댓글을 제거하지 않고 `400` (`BOOK_NOT_AVAILABLE`)을 반환한다.
- 삭제된 도서의 댓글 데이터와 모임·도서·본문 연결은 보존한다. 이를 다시 보여주는 API와
  탐색 방식은 후속 기능에서 결정한다.

## 6. 관리자 (안드로이드 비대상)

모든 `/api/admin/**` API는 `X-Admin-Token: {고정 토큰}` 헤더가 필수다. 앱은 사용하지 않는다.
토큰 누락·불일치는 `401` (`UNAUTHORIZED`). 서버에 토큰이 설정되지 않은 경우에도 전부 `401`이다 (기동은 정상).

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

### 도서 업로드 (인제스트 규격 JSON — 2026-08-06 M7에서 확정)
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
        { "content": "새침하게 흐린 품이 눈이 올 듯하더니..." }
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
  - `{ "name", "isni"? }` — `name` 필수 1~100자. `isni`는 선택: 공백·하이픈 제거 후 16자리(끝자리 `X` 허용) 형식 검증(체크섬 검증 없음). ISNI가 기존 작가와 일치하면 재사용하되 이름이 다르면 `400` (`AUTHOR_NAME_MISMATCH`). 일치하는 작가가 없으면 신규 생성. ISNI 없이 이름만 주면 항상 신규 생성
  - `{ "authorId" }` — 기존 작가 참조 (관리자가 작가 조회로 확인 후 기재). 미존재 시 `400` (`AUTHOR_NOT_FOUND`)
  - 같은 작가가 한 업로드에 중복 기재되면(같은 ISNI·같은 `authorId`·상호 동일 인물) `400` (`DUPLICATE_AUTHOR`)
  - 작자를 알 수 없는 저작물은 이름 `"작자 미상"`으로 등록한다 (인제스트 가이드 규칙)
- `chapters`: **최소 1개.** `title` 필수 1~100자. 각 장의 `passages`도 **최소 1개**
- `passages[].content`: 필수 (공백만 불가), 저장 한도 65,535바이트(TEXT) — 초과 시 `400`. 이미지는 데모 범위에서 제외 (규격에 필드 없음)

서버 처리:
- 본문 순서(`sequence`)는 **배열 등장 순서**로 서버가 책 전체 기준 1..N을 부여한다 (dense 보장은 구성상 성립). 목차 순서도 등장 순서로 1..M
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

- 도서 행의 삭제 상태만 변경하는 소프트 삭제다. 목차·본문·작가 연결·모임·진도·댓글과
  각 관계는 제거하지 않는다.
- 성공 응답: `204 No Content`.
- 존재하지 않는 도서: `400` (`BOOK_NOT_FOUND`).
- 이미 삭제된 도서: `400` (`BOOK_NOT_AVAILABLE`). 중복 삭제를 성공으로 간주하지 않는다.
- 삭제 복구 API는 제공하지 않는다.

### 관리자 페이지
`GET /admin` — 표지 파일 직접 업로드 + 도서 인제스트 폼 + 작가·작품 조회·표지 교체·제거·도서 삭제 UI (HTML, Thymeleaf). 페이지 접근 자체는 토큰이 불필요하며, 페이지 안에서 호출하는 관리자 API에 토큰을 입력해 사용한다.

- 활성 작품에는 삭제 버튼을 표시한다.
- 표지가 있는 작품에는 표지 교체·제거를, 없는 작품에는 표지 추가를 제공한다.
- 삭제 전 도서명이 포함된 확인 창을 한 번 표시한다.
- 삭제된 작품은 “삭제됨” 상태를 표시하고 삭제 버튼을 비활성화한다.

## 7. Android 개발자 변경 안내

이번 변경에서 Android 코드 자체는 수정하지 않는다. 다음 후속 연동이 필요하다.

### 응답 모델 변경

`ClubBookResponse` 형태를 사용하는 다음 응답의 `book` 객체에 필수 enum 필드 `status`가
추가됐다.

- `POST /api/clubs`
- `POST /api/clubs/join`
- `GET /api/clubs`
- `GET /api/clubs/{clubId}`
- `GET /api/members/me/last-reading`

```json
{
  "bookId": 1,
  "title": "운수 좋은 날",
  "authors": ["현진건"],
  "coverImageUrl": null,
  "passageCount": 312,
  "status": "DELETED"
}
```

- `ACTIVE`: 기존 UI와 읽기 동작을 유지한다.
- `DELETED`: 모임과 도서 식별 정보는 표시하되 “더 이상 읽을 수 없는 책이에요”를 안내하고
  읽기 진입·진도 변경·댓글 동작을 제공하지 않는다.
- 마지막 읽기 응답에도 `DELETED`가 올 수 있다. 이 경우 응답을 버리거나 다른 활성 도서로
  대체하지 않는다.
- enum은 향후 확장될 수 있다. 클라이언트가 아는 상태 중 `ACTIVE`만 읽기 가능 상태로
  취급하고, 지원하지 않는 값 때문에 앱이 종료되지 않도록 역직렬화·기본 분기를 설계한다.

### 오류 처리 변경

`BOOK_NOT_AVAILABLE`은 “ID가 잘못됨”이 아니라 “도서는 존재하지만 삭제되어 이용할 수 없음”을
뜻한다. 다음 기존 요청에서 새로 발생할 수 있다.

| 요청 | Android 처리 |
|---|---|
| 도서 상세 | 상세·목차 화면에 진입하지 않고 이용 불가 안내 |
| 모임 생성 | 도서 선택 정보를 새로고침하고 생성 중단 |
| 모임 참여 | 참여 중단 후 이용 불가 안내 |
| 본문 조회 | 본문을 표시하지 않고 이용 불가 안내 |
| 진도 갱신 | 저장된 화면 상태를 성공으로 오인하지 않음 |
| 댓글 조회·작성·수정·삭제 | 댓글을 표시하거나 변경한 것으로 처리하지 않음 |

도서 목록·검색에서는 삭제된 도서가 사라지지만, 이미 가입한 모임 목록·상세와 마지막 읽기에는
삭제된 도서가 `status=DELETED`로 남는다는 차이를 유의한다.

## 8. 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /api/pre-registrations | 공개 이메일 사전신청 |
| POST | /api/members | 회원 생성 |
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
| GET | /api/clubs/{clubId}/passages/{passageId}/comments | 댓글 목록 |
| POST | /api/clubs/{clubId}/passages/{passageId}/comments | 댓글 작성 |
| PUT | /api/comments/{commentId} | 댓글 수정 |
| DELETE | /api/comments/{commentId} | 댓글 삭제 |
| POST | /api/admin/book-covers/upload-url | (관리자) S3 표지 업로드 URL 발급 |
| POST | /api/admin/books | (관리자) 도서 업로드 |
| PUT | /api/admin/books/{bookId}/cover | (관리자) 기존 도서 표지 교체 |
| DELETE | /api/admin/books/{bookId}/cover | (관리자) 기존 도서 표지 제거 |
| DELETE | /api/admin/books/{bookId} | (관리자) 도서 소프트 삭제 |
| GET | /api/admin/authors | (관리자) 작가 목록 조회 |
| GET | /admin | (관리자) 관리자 페이지 (HTML) |
