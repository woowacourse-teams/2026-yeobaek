# 닉네임 유일성 제약 적용

`V7__add_unique_member_nickname.sql`은 `members.nickname`에 `uk_members_nickname` 유일 인덱스를 추가한다.
기존 V1 마이그레이션은 수정하지 않는다. 제약 충돌은 회원 생성과 닉네임 변경 API 모두
`400 INVALID_REQUEST`와 `이미 사용 중인 닉네임입니다.` 메시지로 응답한다.

## 적용 전 확인

배포 대상 DB에서 아래 읽기 전용 쿼리로 기존 중복을 확인한다.

```sql
SELECT nickname, COUNT(*) AS duplicate_count, GROUP_CONCAT(id ORDER BY id) AS member_ids
FROM members
GROUP BY nickname
HAVING COUNT(*) > 1;
```

`GROUP BY`와 유일 인덱스는 기존 컬럼의 `utf8mb4_0900_ai_ci` 정렬 규칙을 사용한다.
영문 대소문자와 악센트만 다른 닉네임도 같은 값으로 취급한다.
현재 컬럼 정의와 인덱스는 아래 쿼리로 확인한다.

```sql
SHOW FULL COLUMNS FROM members LIKE 'nickname';
SHOW INDEX FROM members;
```

중복이 있으면 해당 회원들의 닉네임 처리 방침을 정한 뒤 배포한다. 마이그레이션은 기존 회원을 삭제하거나
닉네임을 임의로 변경하지 않으며, 중복이 남아 있으면 제약 추가가 실패한다.
사전 조회 이후에도 제약이 적용되기 전까지 새 중복이 생길 수 있다. 실제 제약 추가가 성공했는지 확인해야 한다.

## 적용 후 확인

`SHOW INDEX FROM members`에서 `uk_members_nickname`의 `Non_unique`가 `0`인지 확인한다.
닉네임 유일성 제약에 따라 인덱스가 생성되므로 같은 컬럼의 일반 인덱스는 별도로 추가하지 않는다.
이 문서는 적용 절차를 설명하며, 운영 DB의 중복 조회나 마이그레이션 실행 결과를 기록한 문서는 아니다.
