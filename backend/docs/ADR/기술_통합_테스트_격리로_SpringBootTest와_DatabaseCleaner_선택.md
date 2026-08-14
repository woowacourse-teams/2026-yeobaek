# ADR: 통합 테스트 격리로 SpringBootTest + DatabaseCleaner 선택

## 상태

확정. 결정 근거 기록 2026-08-14 (개발자).

구현 커밋: `32c8b9b`

## 문제점 (2026-08-14 개발자 기록)

@DataJpaTest + @Import(테스트할_서비스.class) 방식이라, Import가 다른 테스트 클래스마다 컨텍스트가 달라져서 SpringContext 캐싱이 부분적으로 적용되지 않는다.
→ 서비스 테스트마다 Spring Context가 새로 만들어져서 테스트 시간이 오래 걸린다.

## 해결안 (2026-08-14 개발자 기록)

### 1. @SpringBootTest로 컨텍스트를 통일해, 컨텍스트를 캐싱한다.

이점

운영과 동일한 DBMS로 테스트한다 → 테스트 신뢰성이 향상된다.

문제점

H2에 비해 느리다.

모든 Bean을 등록하고 난 다음에야 테스트하니, 무관한 Bean 쪽에서 문제가 발생하더라도 현재 테스트가 깨진다
→ 테스트가 실패했을 때 확인해야 할 범위(이유들)가 늘어난다.

테스트를 위해 Docker 환경이 필요하다.

테스트 격리를 위해, 모든 테스트에 @Transactional을 사용해야 한다 → 실제 애플리케이션의 트랜잭션 범위와 테스트의 트랜잭션 범위가 달라진다

DB 제약조건이 커밋 시점에야 검증될 경우를 테스트할 수 없다.

실제 애플리케이션의 트랜잭션 범위를 테스트할 수 없다.

### 2. SpringBootTest + DatabaseCleaner로 테스트를 격리한다.

이점

운영과 동일한 DBMS로 테스트한다 → 테스트 신뢰성이 향상된다.

@Transactional을 사용하지 않아도 돼서, 실제 DB에 커밋하는 과정까지를 테스트할 수 있다.

문제점

H2에 비해 느리다.

모든 Bean을 등록하고 난 다음에야 테스트하니, 무관한 Bean 쪽에서 문제가 발생하더라도 현재 테스트가 깨진다
→ 테스트가 실패했을 때 확인해야 할 범위(이유들)가 늘어난다.

테스트를 위해 Docker 환경이 필요하다.

유틸을 구현하고, 모든 통합 테스트에서 이 유틸을 사용해야 한다.

트랜잭션 커밋 이후에 DB를 복구하니, 병렬 테스트가 어렵다. (Worker별 별도의 DB/Schema를 제공하는 등의 작업 필요)

### 3. H2로 테스트해서, TestContainers 자체를 사용할 필요 없게 만든다.

이점

테스트 속도가 매우 빠르다.

테스트 환경에 Docker가 필요 없다.

문제점

운영환경(DBMS)과 다른 환경으로 테스트한다

운영환경의 DBMS와 H2간 기능 차이로 인해 테스트하지 못하는 케이스가 발생할 수 있다.

### 4. 지금의 방식을 유지한다

이점

운영과 동일한 DBMS로 테스트한다 → 테스트 신뢰성이 향상된다.

필요한 Bean들만 가져오니, 무관한 Bean에서 문제가 발생하더라도 이 테스트는 문제 없이 통과한다
→ 테스트 실패시 확인해야 할 범위(이유들)가 적다

문제점

서비스 테스트마다 새로운 Spring Context를 띄우니 속도가 매우 느리다.

테스트를 위해 Docker 환경이 필요하다.

테스트 격리를 위해, 모든 테스트에 @Transactional가 자동 적용된다 → 실제 애플리케이션의 트랜잭션 범위와 테스트의 트랜잭션 범위가 달라진다

### 5. DataJpaTest로 슬라이스 유지 + 싱글톤 Docker 컨테이너

이점

운영 DB와 동일한 환경

파일별로 필요한 Bean만 등록하므로 테스트 실패 시 확인 범위가 작다 (분리)

Docker를 여러번 띄우지 않아도 돼서(1번만 띄우면 돼서) 비용 절감 효과

문제점

@DataJpaTest 내부에 @Transactional 존재 → 테스트에서 커밋까지 발생해야 정확한 확인이 필요한 경우는 이것만으로 커버가 안됨 → 해당 케이스들만 특수한 설정을 추가해줘야 함 → 테스트 구조의 일관성이 깨짐

테스트용 Docker 컨테이너의 생명주기가 Spring에서 관리되지 않고 개발자가 계속 신경써야 함

테스트를 위해 Docker 환경이 필요

## 결정 (2026-08-14 개발자 기록)

SpringBootTest + DatabaseCleaner로 테스트를 격리한다

H2로 테스트하기에는, 운영 DBMS와 사양이 다른 요소들이 많아서, 테스트 신뢰성이 많이 하락한다.

@Transactional을 붙이면 애플리케이션의 트랜잭션과 테스트의 트랜잭션 범위가 서로 달라진다.
→ 테스트 신뢰성 하락 뿐 아니라, 테스트가 불가능한 지점이 생긴다.

## 구현

- 공통 통합 테스트 구성은 [IntegrationTest.java](../../src/test/java/yeobaek/backend/support/IntegrationTest.java)에 있다.
- 데이터베이스 정리 유틸은 [DatabaseCleaner.java](../../src/test/java/yeobaek/backend/support/DatabaseCleaner.java)에 있다.
- 데이터와 자동 증가 값 초기화 검증은 [DatabaseCleanerTest.java](../../src/test/java/yeobaek/backend/support/DatabaseCleanerTest.java)에 있다.
- 정리 및 외래 키 설정 복구 실패 시 동작 검증은 [DatabaseCleanerFailureTest.java](../../src/test/java/yeobaek/backend/support/DatabaseCleanerFailureTest.java)에 있다.
- MySQL Testcontainers와 DatabaseCleaner 빈 구성은 [TestcontainersConfiguration.java](../../src/test/java/yeobaek/backend/support/TestcontainersConfiguration.java)에 있다.
