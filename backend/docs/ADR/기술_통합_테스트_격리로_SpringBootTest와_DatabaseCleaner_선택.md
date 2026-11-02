# ADR: 통합 테스트 격리로 SpringBootTest + DatabaseCleaner 선택

## 상태

확정. 결정 근거 기록 2026-08-14 (개발자).

구현 커밋: `32c8b9b`

## 문제점 (2026-08-14 개발자 기록)

- `@DataJpaTest` + `@Import(테스트할_서비스.class)` 방식이라, Import가 다른 테스트 클래스마다 컨텍스트가 달라져서 SpringContext 캐싱이 부분적으로 적용되지 않는다.
  - → 서비스 테스트마다 Spring Context가 새로 만들어져서 테스트 시간이 오래 걸린다.

## 해결안 (2026-08-14 개발자 기록)

### 1. `@SpringBootTest`로 컨텍스트를 통일해, 컨텍스트를 캐싱한다.

#### 이점

- 운영과 동일한 DBMS로 테스트한다 → 테스트 신뢰성이 향상된다.

#### 문제점

- H2에 비해 느리다.

- 모든 Bean을 등록하고 난 다음에야 테스트하니, 무관한 Bean 쪽에서 문제가 발생하더라도 현재 테스트가 깨진다
  - → 테스트가 실패했을 때 확인해야 할 범위(이유들)가 늘어난다.

- 테스트를 위해 Docker 환경이 필요하다.

- 테스트 격리를 위해, 모든 테스트에 `@Transactional`을 사용해야 한다 → 실제 애플리케이션의 트랜잭션 범위와 테스트의 트랜잭션 범위가 달라진다

- DB 제약조건이 커밋 시점에야 검증될 경우를 테스트할 수 없다.

- 실제 애플리케이션의 트랜잭션 범위를 테스트할 수 없다.

### 2. `SpringBootTest + DatabaseCleaner`로 테스트를 격리한다.

#### 이점

- 운영과 동일한 DBMS로 테스트한다 → 테스트 신뢰성이 향상된다.

- `@Transactional`을 사용하지 않아도 돼서, 실제 DB에 커밋하는 과정까지를 테스트할 수 있다.

#### 문제점

- H2에 비해 느리다.

- 모든 Bean을 등록하고 난 다음에야 테스트하니, 무관한 Bean 쪽에서 문제가 발생하더라도 현재 테스트가 깨진다
  - → 테스트가 실패했을 때 확인해야 할 범위(이유들)가 늘어난다.

- 테스트를 위해 Docker 환경이 필요하다.

- 유틸을 구현하고, 모든 통합 테스트에서 이 유틸을 사용해야 한다.

- 트랜잭션 커밋 이후에 DB를 복구하니, 병렬 테스트가 어렵다. (Worker별 별도의 DB/Schema를 제공하는 등의 작업 필요)

### 3. H2로 테스트해서, TestContainers 자체를 사용할 필요 없게 만든다.

#### 이점

- 테스트 속도가 매우 빠르다.

- 테스트 환경에 Docker가 필요 없다.

#### 문제점

- 운영환경(DBMS)과 다른 환경으로 테스트한다

- 운영환경의 DBMS와 H2간 기능 차이로 인해 테스트하지 못하는 케이스가 발생할 수 있다.

### 4. 지금의 방식을 유지한다

#### 이점

- 운영과 동일한 DBMS로 테스트한다 → 테스트 신뢰성이 향상된다.

- 필요한 Bean들만 가져오니, 무관한 Bean에서 문제가 발생하더라도 이 테스트는 문제 없이 통과한다
  - → 테스트 실패시 확인해야 할 범위(이유들)가 적다

#### 문제점

- 서비스 테스트마다 새로운 Spring Context를 띄우니 속도가 매우 느리다.

- 테스트를 위해 Docker 환경이 필요하다.

- 테스트 격리를 위해, 모든 테스트에 `@Transactional`가 자동 적용된다 → 실제 애플리케이션의 트랜잭션 범위와 테스트의 트랜잭션 범위가 달라진다

### 5. `DataJpaTest`로 슬라이스 유지 + 싱글톤 Docker 컨테이너

#### 이점

- 운영 DB와 동일한 환경

- 파일별로 필요한 Bean만 등록하므로 테스트 실패 시 확인 범위가 작다 (분리)

- Docker를 여러번 띄우지 않아도 돼서(1번만 띄우면 돼서) 비용 절감 효과

#### 문제점

- `@DataJpaTest` 내부에 `@Transactional` 존재 → 테스트에서 커밋까지 발생해야 정확한 확인이 필요한 경우는 이것만으로 커버가 안됨 → 해당 케이스들만 특수한 설정을 추가해줘야 함 → 테스트 구조의 일관성이 깨짐

- 테스트용 Docker 컨테이너의 생명주기가 Spring에서 관리되지 않고 개발자가 계속 신경써야 함

- 테스트를 위해 Docker 환경이 필요

## 결정 (2026-08-14 개발자 기록)

> **`SpringBootTest + DatabaseCleaner`로 테스트를 격리한다**

- H2로 테스트하기에는, 운영 DBMS와 사양이 다른 요소들이 많아서, 테스트 신뢰성이 많이 하락한다.

- `@Transactional`을 붙이면 애플리케이션의 트랜잭션과 테스트의 트랜잭션 범위가 서로 달라진다.
  - → 테스트 신뢰성 하락 뿐 아니라, 테스트가 불가능한 지점이 생긴다.

## 구현

- 공통 통합 테스트 구성은 [IntegrationTest.java](../../src/test/java/yeobaek/backend/support/IntegrationTest.java)에 있다.
- 데이터베이스 정리 유틸은 [DatabaseCleaner.java](../../src/test/java/yeobaek/backend/support/DatabaseCleaner.java)에 있다.
- 데이터와 자동 증가 값 초기화 검증은 [DatabaseCleanerTest.java](../../src/test/java/yeobaek/backend/support/DatabaseCleanerTest.java)에 있다.
- 정리 및 외래 키 설정 복구 실패 시 동작 검증은 [DatabaseCleanerFailureTest.java](../../src/test/java/yeobaek/backend/support/DatabaseCleanerFailureTest.java)에 있다.
- MySQL Testcontainers와 DatabaseCleaner 빈 구성은 [TestcontainersConfiguration.java](../../src/test/java/yeobaek/backend/support/TestcontainersConfiguration.java)에 있다.

## 성능 개선 실측: `@SpringBootTest` 구성 통일 (2026-11-01)

### 워밍업 후 비교 결과

각 버전의 64개 테스트를 한 번씩 실행해 워밍업한 뒤, 새로 측정한 버전별 10회 결과만 비교했다. 전체 실행 시간의 중앙값은 **약 144.3초에서 50.9초로, 64.7% 감소**했다. 워밍업 실행과 이전 측정값은 집계에 포함하지 않았다.

전체 실행 시간의 분포는 다음과 같다. 시간 단위는 초다.

| 버전 | 측정 횟수 | 중앙값 | Q1~Q3 | IQR | 최솟값~최댓값 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 변경 전 | 10 | 144.323 | 142.356~148.406 | 6.050 | 137.859~153.669 |
| 변경 후 | 10 | 50.921 | 48.403~53.738 | 5.336 | 46.246~71.727 |

Q1~Q3는 측정값 가운데 50%가 위치한 구간이고, IQR은 그 구간의 폭이다. 변경 후에도 46.246~71.727초의 편차가 남아 있어 대표값과 범위를 함께 제시했다.

| 보조 지표 | 변경 전 | 변경 후 | 변화 |
| --- | ---: | ---: | ---: |
| 테스트 시간 합계의 중앙값 | 121.035초 | 34.887초 | 71.2% 감소 |
| 실행당 DB 컨텍스트 수 | 9 | 2 | 77.8% 감소 |
| 실행당 MySQL 컨테이너 수 | 9 | 2 | 77.8% 감소 |

컨텍스트·컨테이너 수는 각 버전의 10회 실행에서 모두 동일했다. 20회 측정에서 같은 13개 클래스의 64개 테스트가 매번 통과했으며, 총 1,280회 테스트 실행의 실패·오류·건너뜀은 모두 0이었다.

변경 전에는 공통 JPA 구성 1개와 서비스별 구성 8개로 DB 컨텍스트가 9개 생성됐다. 변경 후에는 공통 컨텍스트 1개와 `@MockitoBean`으로 구성이 다른 충돌 테스트용 컨텍스트 1개로 줄었다. 각 컨텍스트에서 MySQL 컨테이너도 새로 준비되므로 컨테이너 실행 횟수 역시 9회에서 2회로 줄었다.

<sub>중앙값은 정렬한 10개 값 중 5번째와 6번째 값의 평균이다. Q1·중앙값·Q3는 R7 방식(위치 `1 + (n - 1) × p`를 인접 값 사이에서 선형 보간, `p` = 0.25·0.5·0.75)으로 계산했고, IQR은 `Q3 - Q1`이다. 통계는 소수점 셋째 자리로 표시했으며 변화율은 반올림 전 값으로 계산했다.</sub>

### 비교 범위

서비스마다 다른 컨텍스트를 준비하던 방식을 `@SpringBootTest`로 통일했을 때의 차이만 확인했다. `DatabaseCleaner` 도입이나 일부 테스트의 단위 테스트 전환은 포함하지 않았다.

두 버전 모두 `b20452a42a71b6f4855ebe5361f48e85a4a0656a`에서 시작했다. 변경 전은 해당 커밋 그대로이며, 변경 후는 같은 커밋에 테스트 설정 파일 9개만 수정한 비교용 버전이다. 구현 커밋 `32c8b9b`와 비교한 결과가 아니다.

- 공통 부모 `RepositoryTest`의 `@DataJpaTest`를 `@SpringBootTest`로 바꾸고, 서비스 테스트 8개의 개별 `@Import`를 제거했다.
- 기존 롤백 방식을 유지하도록 `@Transactional`을 명시했다.
- 기존 테스트가 사용하는 `TestEntityManager`를 유지하도록 `@AutoConfigureTestEntityManager`를 추가했다.
- SQL 출력 차이가 결과에 섞이지 않도록 `@DataJpaTest`의 기본값과 같은 `spring.jpa.show-sql=true`를 지정했다.

비교용 공통 설정은 다음과 같다.

```java
@SpringBootTest(properties = "spring.jpa.show-sql=true")
@Transactional
@AutoConfigureTestEntityManager
@Import(TestcontainersConfiguration.class)
public abstract class RepositoryTest {
}
```

13개 클래스의 64개 테스트 본문과 검증, mock, 운영 코드, DB 프로퍼티, 컨테이너 설정은 바꾸지 않았다. `ClubJoinCodeCollisionTest`의 `@MockitoBean`도 유지했으며, 공통 컨테이너 설정을 가져오는 `@Import`는 양쪽에서 동일하다.

### 측정 환경과 방법

- 워밍업: 2026-11-01 22:44~22:48 (KST). 변경 전·후 각각 64개 테스트 전체를 1회 실행했다. 두 실행 모두 통과했고, 실행 시간 154.373초와 69.576초는 집계에서 제외했다.
- 본 측정: 같은 날 22:48~23:22 (KST), 로컬 PC 1대에서 버전별 10회, 총 20회 실행했다.
- CPU: Intel Core i7-1360P, RAM: 약 32GB. Java 21.0.4, Gradle 9.5.1, Spring Boot 4.1.0, Docker Engine 29.7.2, MySQL 8.4를 사용했다.
- 변경 전(A) → 변경 후(B) → 변경 후(B) → 변경 전(A)의 ABBA 순서를 5회 반복했다. 두 작업 공간의 소스 해시를 실행 전후에 확인했다.
- 매번 `cleanTest`로 이전 결과를 지웠고, Gradle이 테스트를 실제로 실행했는지 확인했다. 캐시에서 가져오거나 건너뛴 테스트 작업은 측정으로 인정하지 않았다.
- 매 실행의 XML에서 같은 13개 클래스·64개 테스트 이름과 통과 여부를 대조했다. 병렬 실행은 사용하지 않았다.
- 전체 실행 시간은 Gradle 명령 호출 직전부터 종료까지 스톱워치로 측정했다. 테스트 시간 합계는 XML에 기록된 13개 클래스 실행 시간의 합이다.
- DB 컨텍스트와 MySQL 컨테이너 수는 XML에 저장된 `Started <TestClass> in`, `Container mysql:8.4 started in` 로그로 집계했다.

Docker 이미지는 캐시된 상태였지만, 각 명령은 `--no-daemon`으로 새로 실행하고 컨테이너도 새로 생성했다. **컨텍스트 재사용은 각 실행 안의 테스트 사이에서만 발생하며, 이전 명령의 컨텍스트를 다음 명령에서 재사용한 것은 아니다.** 워밍업은 초기 캐시 상태를 준비하기 위한 절차이며, 워밍업 자체의 효과를 따로 측정한 결과는 아니다.

각 작업 공간의 `backend/`에서 실행한 명령은 다음과 같다.

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

### 원시 측정값

<details>
<summary>워밍업을 제외한 20회 측정값</summary>

시간 단위는 초다. 아래 행에는 워밍업 두 실행과 이전 측정값을 포함하지 않았다.

| 순서 | 버전 | 회차 | 전체 실행 시간 | 테스트 시간 합계 | DB 컨텍스트 수 | MySQL 컨테이너 수 |
| ---: | --- | ---: | ---: | ---: | ---: | ---: |
| 1 | 변경 전 | 1 | 141.840 | 119.987 | 9 | 9 |
| 2 | 변경 후 | 1 | 47.963 | 32.876 | 2 | 2 |
| 3 | 변경 후 | 2 | 50.553 | 34.969 | 2 | 2 |
| 4 | 변경 전 | 2 | 144.907 | 118.884 | 9 | 9 |
| 5 | 변경 전 | 3 | 137.859 | 115.707 | 9 | 9 |
| 6 | 변경 후 | 3 | 71.727 | 55.065 | 2 | 2 |
| 7 | 변경 후 | 4 | 51.288 | 34.805 | 2 | 2 |
| 8 | 변경 전 | 4 | 153.251 | 124.371 | 9 | 9 |
| 9 | 변경 전 | 5 | 144.056 | 116.929 | 9 | 9 |
| 10 | 변경 후 | 5 | 46.246 | 30.957 | 2 | 2 |
| 11 | 변경 후 | 6 | 47.134 | 31.620 | 2 | 2 |
| 12 | 변경 전 | 6 | 149.572 | 126.774 | 9 | 9 |
| 13 | 변경 전 | 7 | 143.905 | 122.082 | 9 | 9 |
| 14 | 변경 후 | 7 | 64.281 | 47.624 | 2 | 2 |
| 15 | 변경 후 | 8 | 49.721 | 32.512 | 2 | 2 |
| 16 | 변경 전 | 8 | 144.589 | 122.408 | 9 | 9 |
| 17 | 변경 전 | 9 | 139.558 | 117.634 | 9 | 9 |
| 18 | 변경 후 | 9 | 52.791 | 35.100 | 2 | 2 |
| 19 | 변경 후 | 10 | 54.054 | 37.116 | 2 | 2 |
| 20 | 변경 전 | 10 | 153.669 | 128.494 | 9 | 9 |

</details>

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

이번 실측은 같은 과거 코드와 테스트 본문을 유지한 상태에서, 개별 JPA 테스트 구성을
`@SpringBootTest`로 통일한 효과를 비교했다. 컨텍스트 재사용의 이점과 더 넓은 범위의
빈을 로드하는 비용이 함께 반영되므로, 캐시 자체의 비용만 따로 측정한 결과는 아니다.

양쪽 모두 롤백 방식으로 데이터를 정리했으며 `DatabaseCleaner`는 사용하지 않았다.
따라서 이 결과로 `DatabaseCleaner`의 비용이나 실제 커밋을 검증하는 현재 전체 통합
테스트의 성능을 설명할 수는 없다. 워밍업을 제외한 버전별 10회 관측 결과이며,
OS·Docker 캐시와 백그라운드 부하의 영향을 받을 수 있다. 표는 이번 실행의 분포를
설명하며 통계적 유의성을 검증한 결과가 아니다. 다른 PC나 CI에서도 같은 비율로
빨라진다고 일반화하지 않는다.

### 미래 판단 신호

- 실행 시간이 다시 증가하면 Context cache miss와 DatabaseCleaner 비용을 분리해
  계측한다.
- 병렬 실행이 필요해지면 worker별 DB 또는 schema 제공 방식에 대한 새 결정이
  필요하다.
- 이 결정은 빠른 피드백을 위한 단위 테스트나 slice 테스트를 폐기하는 결정이 아니다.
