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

## 성능 개선 실측 (2026-08-14)

### 측정 환경

| 항목 | 값 |
| --- | --- |
| CPU | 13th Gen Intel Core i7-1360P 2.20GHz |
| RAM | 32.0GB |
| 디스크 | 954GB NVMe SSD |
| Java | 21.0.4 |
| Gradle | 9.5.1 |
| Docker Engine | 28.0.1 |
| Docker VM 메모리 | 16,624,467,968 bytes (약 15.5GiB) |
| MySQL | 8.4 |

CPU, RAM, 디스크 정보는 사용자가 제공했다.

### 비교 대상

- 변경 전: `b20452a42a71b6f4855ebe5361f48e85a4a0656a` (`32c8b9b^`)
- 변경 후: `32c8b9b39b13f249bc5458aaca839e88e0214cfd`

### 측정 방법

- 두 버전을 별도의 detached worktree에서 측정했다.
- 각 버전에서 `testClasses --no-daemon`으로 사전 워밍업했다. 변경 전은 57초,
  변경 후는 55초였으며 측정 결과에서 제외했다.
- Docker 이미지 레이어는 캐시된 상태였지만, 매 실행마다 새 컨테이너를 생성했다.
- 동일 64개 테스트의 측정 명령은 다음과 같다.

```powershell
.\gradlew.bat cleanTest test --no-daemon --console=plain `
  --tests 'yeobaek.backend.admin.service.AdminAuthorServiceTest' `
  --tests 'yeobaek.backend.admin.service.BookIngestServiceTest' `
  --tests 'yeobaek.backend.book.repository.BookMappingTest' `
  --tests 'yeobaek.backend.book.service.BookServiceTest' `
  --tests 'yeobaek.backend.book.service.PassageServiceTest' `
  --tests 'yeobaek.backend.club.repository.ClubMappingTest' `
  --tests 'yeobaek.backend.club.service.ClubJoinCodeCollisionTest' `
  --tests 'yeobaek.backend.club.service.ClubServiceTest' `
  --tests 'yeobaek.backend.club.service.ProgressServiceTest' `
  --tests 'yeobaek.backend.comment.repository.CommentRepositoryTest' `
  --tests 'yeobaek.backend.comment.service.CommentServiceTest' `
  --tests 'yeobaek.backend.member.repository.MemberRepositoryTest' `
  --tests 'yeobaek.backend.support.DevDataSeederTest'
```
- 저장소에 병렬 실행 설정이 없고, 측정 명령에도 `--parallel`을 사용하지 않았다.
- 동일한 13개 테스트 클래스와 64개 테스트 케이스를
  변경 전 → 변경 후 → 변경 후 → 변경 전 → 변경 전 → 변경 후 순서로 실행해
  각 버전을 3회씩 측정했다.
- Gradle wall time은 `BUILD SUCCESSFUL`에 표시된 초 단위 값을 기록했다.
- suite time, 컨테이너 시간, 컨텍스트 시간은 XML 로그 값을 합산했다.
- 컨테이너 시작은 `Container mysql:8.4 started in`, Spring Context 시작은
  `Started <TestClass> in`, Hikari 시작은 `Start completed` 문자열로 집계했다.

### 동일 64개 테스트의 원시 측정값

아래 값은 측정 사실이다. 여섯 번의 실행 모두 13개 suite, 64개 test였고
failure, error, skipped는 각각 0이었다.

| 실행 순서 | 버전 | 회차 | Wall time | Suite time | 컨테이너 수 | 컨테이너 누적 시간 | Context/Hikari 수 | Context 누적 시간 |
| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 변경 전 | 1 | 312s | 261.110s | 9 | 167.607s | 9 | 245.529s |
| 2 | 변경 후 | 1 | 152s | 117.763s | 1 | 17.499s | 1 | 49.112s |
| 3 | 변경 후 | 2 | 119s | 98.903s | 1 | 14.544s | 1 | 34.795s |
| 4 | 변경 전 | 2 | 220s | 189.139s | 9 | 141.500s | 9 | 181.539s |
| 5 | 변경 전 | 3 | 248s | 205.660s | 9 | 137.149s | 9 | 193.707s |
| 6 | 변경 후 | 3 | 58s | 45.013s | 1 | 7.877s | 1 | 16.568s |

### 동일 64개 테스트의 중앙값 비교

아래 차이와 비율은 원시 측정값으로 계산한 결과다.

| 지표 | 변경 전 중앙값 | 변경 후 중앙값 | 차이 | 변화율 |
| --- | ---: | ---: | ---: | ---: |
| Wall time | 248s (범위 220..312s) | 119s (범위 58..152s) | -129s | -52.0% |
| Suite time | 205.660s | 98.903s | -106.757s | -51.9% |
| 컨테이너 수 | 9 | 1 | -8 | -88.9% |
| 컨테이너 누적 시간 | 141.500s | 14.544s | -126.956s | -89.7% |
| Context/Hikari 수 | 9 | 1 | -8 | -88.9% |
| Context 누적 시간 | 193.707s | 34.795s | -158.912s | -82.0% |
| Suite time - Context 누적 시간 단순 차감 근사 | 11.953s | 64.108s | +52.155s | +436.3% |

**추론:** `Suite time - Context 누적 시간`의 단순 차감 근사값 증가는
DatabaseCleaner가 매 테스트마다 수행하는 `TRUNCATE`와 실제 커밋 등 Context 외 비용의
증가로 해석할 수 있다. 이 비용은 분리 측정하지 않았으므로 원인을 확정할 수 없다.

### 전체 테스트 suite 1회 측정

아래 변경 전과 변경 후 값은 각 버전을 1회씩 실행한 측정 사실이고, 차이와 변화율은
그 값으로 계산한 결과다. 두 실행 모두 failure, error, skipped는 각각 0이었다.

| 지표 | 변경 전 | 변경 후 | 차이 | 변화율 |
| --- | ---: | ---: | ---: | ---: |
| 테스트 수 | 253 | 255 | +2 | - |
| Suite 수 | 35 | 37 | +2 | - |
| Wall time | 193s | 81s | -112s | -58.0% |
| Suite time | 168.205s | 67.719s | -100.486s | -59.7% |
| 컨테이너 수 | 11 | 1 | -10 | -90.9% |
| 컨테이너 누적 시간 | 119.494s | 11.122s | -108.372s | -90.7% |
| Hikari 수 | 11 | 1 | -10 | -90.9% |
| 전체 Context 수 | 20 | 10 | -10 | -50.0% |
| Context 누적 시간 | 156.029s | 27.519s | -128.510s | -82.4% |

전체 Context 중 나머지 9개는 양쪽에 동일한 MVC slice Context다. DB Context가
11개에서 1개로 줄어든 결과가 전체 Context 20개에서 10개로 나타났다.

### 측정된 Context 구성

변경 전 동일 64개 테스트에서는 공통 DataJpa Context 1개와 서로 다른 서비스
`@Import` Context 8개로 DB Context가 9개였다. Context마다
TestcontainersConfiguration 빈을 구성하므로 MySQL 컨테이너 시작, Hikari 시작,
스키마 create-drop이 반복됐다.

전체 suite에서는 BackendApplicationTests와 ApiDocsServingTest가 DB Context 2개를
추가해 변경 전 DB Context가 11개였다.

변경 후에는 IntegrationTest의 공통 Context와 AutoConfigureMockMvc 구성을 사용해
DB Context가 1개였다. 충돌 테스트 2개는 Mockito 단위 테스트로 분리했다.

### 비교 시 주의 사항

- 동일 64개 테스트 비교에서 ClubJoin 테스트 2개는 케이스 수는 같지만 성격이
  통합 테스트에서 단위 테스트로 변경됐다.
- 측정은 로컬 PC 1대에서 하루 동안 수행했고, 각 버전을 3회만 반복했다.
- Docker 및 OS 캐시와 백그라운드 부하로 실행 간 편차가 크다.
- 전체 suite 측정은 각 버전 1회만 실행한 보조 지표다.
- Wall time은 초 단위로 반올림된 값이다.
- Docker 이미지 레이어가 캐시된 상태에서 측정했다.
- 결과를 CI나 다른 PC의 성능으로 일반화할 수 없다.
- 컨테이너 시간은 Context 시간에 포함되므로 두 값을 합산하면 안 된다.
- DatabaseCleaner의 순수 비용을 분리하는 microbenchmark는 수행하지 않았다.

## 장기적 의미와 트레이드오프

### 유지되는 장점

- 공통 IntegrationTest가 Spring Context 캐시 경계를 제공한다. 공통 Context의
  cache key가 유지되는 동안 서비스 테스트 수가 늘어나도 Context 수가 서비스
  테스트 수와 함께 증가하지 않는다.
- 운영과 동일한 DBMS인 MySQL에서 실제 커밋까지 검증할 수 있다.
- 통합 테스트가 공통 Context와 DatabaseCleaner를 사용하므로 테스트 격리 구조가
  일관되게 유지된다.

### 함께 유지되는 비용

- 전체 Bean을 등록하므로 테스트 대상과 무관한 Bean의 문제도 통합 테스트 실패로
  이어질 수 있다.
- Docker와 운영 DBMS 기반 컨테이너를 실행하는 시간 및 환경 비용이 필요하다.
- DatabaseCleaner가 각 테스트 케이스 전에 테이블을 `TRUNCATE`하는 비용이 발생한다.
- 하나의 DB를 공유하고 커밋 후 정리하므로 현재 구조에서는 병렬 실행이 제한된다.
- 공통 Context의 annotation이나 mock override가 달라지면 cache key가 달라져 새
  Context가 만들어질 수 있으므로 해당 구성을 관리해야 한다.
- 스키마와 제약조건이 변하면 DatabaseCleaner가 계속 정상적으로 정리하고 복구하는지
  유지보수해야 한다.

### 실측 결과의 적용 범위

이번 실측은 현재 테스트 구성에서 Context 시작 감소분이 DatabaseCleaner를 포함한
Context 외 비용 증가분보다 컸다는 증거다. 모든 테스트 규모와 실행 환경에서 이
구조가 항상 더 빠르다는 의미는 아니다.

### 미래 판단 신호

- 실행 시간이 다시 증가하면 Context cache miss와 DatabaseCleaner 비용을 분리해
  계측한다.
- 병렬 실행이 필요해지면 worker별 DB 또는 schema 제공 방식에 대한 새 결정이
  필요하다.
- 이 결정은 빠른 피드백을 위한 단위 테스트나 slice 테스트를 폐기하는 결정이 아니다.
