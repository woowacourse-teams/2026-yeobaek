# 닉네임 유일성 제약 적용

`V7__add_unique_member_nickname.sql`은 `members.nickname`에 `uk_members_nickname` 유일 인덱스를 추가해요.
기존 V1 마이그레이션은 수정하지 않아요. 제약 충돌은 회원 생성과 닉네임 변경 API 모두
`400 INVALID_REQUEST`와 `이미 사용 중인 닉네임입니다.` 메시지로 응답해요.

## 적용 전 확인

배포 대상 DB에서 아래 읽기 전용 쿼리로 기존 중복을 확인해요.

```sql
SELECT nickname, COUNT(*) AS duplicate_count, GROUP_CONCAT(id ORDER BY id) AS member_ids
FROM members
GROUP BY nickname
HAVING COUNT(*) > 1;
```

`GROUP BY`와 유일 인덱스는 기존 컬럼의 `utf8mb4_0900_ai_ci` 정렬 규칙을 사용해요.
영문 대소문자와 악센트만 다른 닉네임도 같은 값으로 취급해요.
현재 컬럼 정의와 인덱스는 아래 쿼리로 확인해요.

```sql
SHOW FULL COLUMNS FROM members LIKE 'nickname';
SHOW INDEX FROM members;
```

중복이 있으면 해당 회원들의 닉네임 처리 방침을 정한 뒤 배포해요. 마이그레이션은 기존 회원을 삭제하거나
닉네임을 임의로 변경하지 않으며, 중복이 남아 있으면 제약 추가가 실패해요.
사전 조회 이후에도 제약이 적용되기 전까지 새 중복이 생길 수 있어요. 실제 제약 추가가 성공했는지 확인해야 해요.

## 적용 후 확인

`SHOW INDEX FROM members`에서 `uk_members_nickname`의 `Non_unique`가 `0`인지 확인해요.
이 인덱스가 닉네임 중복 방지와 닉네임 동등 조건 조회에 사용되므로 같은 컬럼의 일반 인덱스는 별도로 추가하지 않아요.
이 문서는 적용 절차를 설명하며, 운영 DB의 중복 조회나 마이그레이션 실행 결과를 기록한 문서는 아니에요.
