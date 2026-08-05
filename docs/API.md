# 여백 API 명세 (계약 초안)

> 기반 문서: [`PRD.md`](PRD.md) · 상태: **구현 전 계약 초안**
> 구현이 진행되면 RestDocs + Swagger가 이 문서를 살아있는 문서로 대체한다. 그 전까지는 이 문서가 계약의 진실 소스다.
> 계약 변경이 필요하면 반드시 이 문서를 먼저 갱신하고 안드로이드와 합의한다.

## 0. 공통 규약

- Base path: `/api` (호스트는 배포 후 공유)
- 요청/응답 본문: JSON (UTF-8), 필드는 camelCase
- **회원 식별**: 회원 생성을 제외한 모든 API는 `X-Member-Id: {회원ID}` 헤더 필수.
  클라이언트는 회원 생성 시 받은 ID를 기기에 저장하고 인터셉터로 전 요청에 첨부한다.
- 시각 필드: ISO-8601 (`2026-08-05T14:30:00`)
- 에러 응답: 상태 코드 + `{ "message": "사람이 읽을 수 있는 설명" }`
  - `400` 잘못된 요청(검증 실패 포함) · `403` 권한 없음(모임 미소속, 남의 댓글 등) · `404` 대상 없음
  - `X-Member-Id` 누락 또는 존재하지 않는 회원: `400`

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

### 도서 목록 (모임 생성 시 선택용)
`GET /api/books`

응답 `200`:
```json
{
  "books": [
    {
      "bookId": 1,
      "title": "운수 좋은 날",
      "authors": ["현진건"],
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
  "publisher": "자체 제작",
  "publishedYear": 1924,
  "passageCount": 312,
  "chapters": [
    { "chapterId": 1, "title": "1장", "sequence": 1, "startPassageSequence": 1, "endPassageSequence": 105 }
  ]
}
```
- `startPassageSequence`/`endPassageSequence`: 해당 챕터에 속한 본문의 전체 순서 범위. 리더의 챕터 이동·범위 조회에 사용.

## 3. 모임

### 모임 생성
`POST /api/clubs`

요청:
```json
{ "name": "교환독서 1기", "bookId": 1 }
```

응답 `201` — 생성자는 자동으로 모임에 참여된다:
```json
{
  "clubId": 1,
  "name": "교환독서 1기",
  "joinCode": "A3F9KQ",
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "passageCount": 312 }
}
```
- `joinCode`: 서버 발급, 전역 unique, 영구 고정.

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
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "passageCount": 312 }
}
```
- 존재하지 않는 코드: `404`. 이미 참여한 모임: `200`과 동일 응답 (멱등).

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
      "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "passageCount": 312 },
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
      "imageUrl": null,
      "commentCount": 3
    }
  ]
}
```
- `content`와 `imageUrl` 중 하나 이상 존재. `commentCount`는 이 모임의 댓글 수.

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

### 홈 — 마지막으로 읽던 책
`GET /api/members/me/last-reading`

응답 `200` — 전 모임 중 `lastReadAt`이 가장 최근인 것:
```json
{
  "clubId": 1,
  "clubName": "교환독서 1기",
  "book": { "bookId": 1, "title": "운수 좋은 날", "authors": ["현진건"], "passageCount": 312 },
  "lastReadPassageSequence": 42,
  "progressRate": 13,
  "lastReadAt": "2026-08-05T14:30:00"
}
```
- 어떤 모임에서도 읽기 기록이 없으면 `204 No Content`.

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

### 댓글 작성
`POST /api/clubs/{clubId}/passages/{passageId}/comments`

요청:
```json
{ "content": "이 문장에서 멈칫했어요." }
```
- `content`: 1~1000자.

응답 `201`: 댓글 목록의 원소와 동일 형태 (`mine: true`).

### 댓글 수정
`PUT /api/comments/{commentId}`

요청:
```json
{ "content": "수정된 내용" }
```

응답 `200`: 댓글 목록의 원소와 동일 형태. 본인 댓글이 아니면 `403`.

### 댓글 삭제
`DELETE /api/comments/{commentId}`

응답 `204`. 본인 댓글이 아니면 `403`. 하드 삭제 (PRD 3.5).

## 6. 관리자 (안드로이드 비대상)

### 도서 업로드
`POST /api/admin/books` — `X-Admin-Token: {고정 토큰}` 헤더 필수. 앱은 사용하지 않는다.

- 요청 본문(규격 JSON)의 상세 포맷은 인제스트 마일스톤(M7)에서 확정한다.

## 7. 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /api/members | 회원 생성 |
| GET | /api/books | 도서 목록 |
| GET | /api/books/{bookId} | 도서 상세 + 목차 |
| POST | /api/clubs | 모임 생성 |
| POST | /api/clubs/join | 참여 코드로 참여 |
| GET | /api/clubs | 내 모임 목록 |
| GET | /api/clubs/{clubId}/passages | 본문 범위 조회 |
| PUT | /api/clubs/{clubId}/progress | 진도 갱신 |
| GET | /api/members/me/last-reading | 홈: 마지막 읽던 책 |
| GET | /api/clubs/{clubId}/passages/{passageId}/comments | 댓글 목록 |
| POST | /api/clubs/{clubId}/passages/{passageId}/comments | 댓글 작성 |
| PUT | /api/comments/{commentId} | 댓글 수정 |
| DELETE | /api/comments/{commentId} | 댓글 삭제 |
| POST | /api/admin/books | (관리자) 도서 업로드 |
