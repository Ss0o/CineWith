# 테스트 전략

## 현재 기준

- Gradle은 JUnit Platform에서 테스트를 실행한다.
- `CommunityServiceApplicationTests`는 `@SpringBootTest`로 Context 시작을 확인한다.
- JPA, Security, OAuth2 Client, Validation, Web MVC용 Spring Boot Test Starter를 사용할 수 있다.
- V1 운영 DB 엔진은 PostgreSQL을 사용하며, Repository / DB Integration Test는 Testcontainers PostgreSQL을 기본으로 한다.
- Spring Boot Testcontainers, Testcontainers JUnit Jupiter와 PostgreSQL 모듈을 사용하며 PostgreSQL JDBC Driver로 연결한다.
- Coverage 도구와 최소 기준은 아직 설정하지 않았다.

현재 의존성은 사용 가능한 테스트 수단일 뿐 모든 테스트에서 Spring Context나 Container를 사용해야 한다는 의미가 아니다.

## 테스트 원칙

- 구현 편의보다 테스트 가능한 요구사항을 우선한다.
- 구현 세부사항보다 외부에서 관찰할 수 있는 동작과 도메인 규칙을 검증한다.
- 하나의 테스트에서 너무 많은 기능을 동시에 검증하지 않는다.
- 실패 사례를 정상 사례만큼 중요하게 테스트한다.
- 일반 자동 테스트에서 실제 TMDB API를 직접 호출하지 않는다.
- 테스트는 실행 순서나 개발자 환경에 의존하지 않고 반복 실행해도 같은 결과를 내야 한다.
- 기능 테스트와 성능 테스트를 분리한다.
- 가능한 가장 좁은 테스트 계층으로 규칙을 검증하고, 여러 계층의 협력이 필요한 핵심 흐름만 통합 테스트로 검증한다.

## 테스트 계층

### Unit / Domain Test

Spring Context와 DB 없이 실행할 수 있는 순수 규칙을 우선 검증한다.

대상:

- 평점의 범위, 단위, API 표현과 내부 정수 표현 사이의 변환
- 닉네임, 리뷰 제목·본문, 댓글 본문 등 확정된 값 검증 규칙
- 작성자 소유권 판단처럼 외부 의존성이 없는 도메인 규칙
- 평균 평점 계산 규칙

아직 길이나 문자 정책이 확정되지 않은 닉네임·content 검증은 임의의 기준으로 테스트하지 않는다.

### Service Test

Use Case의 흐름과 Repository·외부 연동 사이의 협력을 검증한다. Repository와 Movie API Client는 테스트 목적에 따라 Mock 또는 Fake 사용을 고려한다.

대상:

- 가입 Identity를 사용한 Member 가입과 중복 방지
- 영화 검색·상세·추천 위임
- 리뷰 작성 시 Movie 조회·재사용·최초 저장
- 리뷰 생성·수정·삭제와 중복 리뷰 방지
- 댓글 생성·수정·삭제
- 현재 Member를 기준으로 한 소유권 검증
- 리뷰 삭제 시 소속 댓글 삭제

### Controller / Security Test

HTTP 계약과 인증·인가 경계를 검증한다. 전체 애플리케이션보다 좁은 Web/Security Test 구성을 우선 고려한다.

대상:

- HTTP Method, Path, Request, Response 계약
- Request Validation과 상태 코드
- `ANONYMOUS`, `SIGNUP_REQUIRED`, `MEMBER` 인증 상태
- 작성자와 비작성자의 인가 결과
- 클라이언트의 `memberId`를 신뢰하지 않는 동작
- Session 로그아웃과 CSRF 보호
- 일관된 Error Response의 `code`, `message`

대표 사례:

- `ANONYMOUS → Review 작성 = 401 Unauthorized`
- `SIGNUP_REQUIRED → Review 작성 = 403 Forbidden`
- `Member A → Member B의 Review 삭제 = 403 Forbidden`
- `Member A → Member B의 Comment 삭제 = 403 Forbidden`

### Repository / DB Integration Test

Testcontainers PostgreSQL에서 실제 Repository, Entity Mapping, DB Constraint, Transaction 등 여러 구성요소의 협력이 필요한 핵심 흐름만 선택적으로 검증한다. H2의 SQL 문법이나 제약 동작을 PostgreSQL과 동일하다고 가정하지 않는다.

대상:

- Repository 저장과 조회
- 핵심 PK, FK, UNIQUE 제약과 필요성이 확인된 대표 NOT NULL 제약
- `Review → Member`, `Review → Movie`, `Comment → Member`, `Comment → Review` 관계
- 트랜잭션 성공과 Rollback
- 리뷰 삭제 시 소속 댓글 삭제
- 동시 또는 우회 요청에서도 지켜져야 하는 최종 데이터 무결성

리뷰 삭제 구현 전략이 확정되기 전에는 특정 Cascade 동작을 전제로 테스트를 작성하지 않는다.

공통 PostgreSQL Container는 테스트 전용 `TestcontainersConfiguration`의 Bean으로 선언하고 Spring Boot `@ServiceConnection`으로 연결한다. Repository Test는 공통 `@PostgresRepositoryTest`를 사용한다. 이 Annotation은 `@DataJpaTest`, `AutoConfigureTestDatabase.Replace.NONE`, 공통 Container 설정 Import를 묶어 H2 등 Embedded DB가 Classpath에 추가되더라도 DataSource를 교체하지 못하게 한다. Member, Movie와 향후 Review, Comment Repository Test가 같은 규칙을 재사용한다. Spring이 Container 생명주기를 관리하며 각 Repository Test의 Transaction Rollback으로 테스트 데이터를 격리한다.

### Architecture Test

ArchUnit JUnit 5 지원을 테스트 의존성으로 사용해 패키지 간 의존 방향을 자동 검증한다. `ArchitectureTests`는 현재 패키지가 비어 있어도 실행되며, 향후 클래스가 추가되면 다음 규칙을 강제한다.

- Controller는 Repository에 직접 의존하지 않는다.
- Repository는 Controller에 의존하지 않는다.
- Service는 Controller에 의존하지 않는다.
- Domain 및 Entity 계층은 Controller에 의존하지 않는다.

Entity의 API Response 직접 반환, DTO 명명 규칙, Transaction Annotation 위치, Service별 테스트 존재 여부, LAZY Fetch 여부는 구현 구조가 생긴 뒤 검증 가능성을 다시 판단하며 현재 ArchUnit 규칙으로 강제하지 않는다.

## 요구사항 추적성

각 요구사항 ID는 최소 하나 이상의 테스트 계층과 시나리오에 연결한다. 실제 구현 시 요구사항이 변경되면 이 표와 관련 테스트를 함께 갱신한다.

### 인증 및 회원

| 요구사항 ID | 테스트 계층 | 핵심 검증 시나리오 |
| --- | --- | --- |
| `AUTH-001` | Controller / Security | Google OIDC 인증 성공 Identity에서 `sub`를 `providerId`로 사용하고, 인증 실패·취소 시 인증 완료 상태가 되지 않는다. 실제 Google은 호출하지 않는다. |
| `AUTH-002` | Controller / Security | Member가 없으면 `SIGNUP_REQUIRED`, 존재하면 `MEMBER`로 구분하며 `SIGNUP_REQUIRED`는 닉네임 설정만 가능하고 Member 전용 기능은 403이다. |
| `AUTH-003` | Controller / Security | 유효한 HTTP Session은 인증 상태를 유지하고, 세션이 없거나 만료·무효화되면 인증 상태를 유지하지 않는다. |
| `AUTH-004` | Controller / Security | 로그아웃 성공 후 기존 Session으로 Member 전용 API를 요청하면 더 이상 인증되지 않는다. |
| `MEMBER-001` | Service, Controller / Security | `SIGNUP_REQUIRED`가 유효한 닉네임으로 가입하면 신뢰된 Session Identity로 Member가 생성되고 `MEMBER` 상태로 전환되며 201을 반환한다. |
| `MEMBER-002` | Service, Integration | 같은 `provider + providerId`로 Member를 중복 생성하지 않으며 DB UNIQUE 제약도 중복을 거부한다. |
| `MEMBER-003` | Controller / Security | `MEMBER`는 `/api/members/me`로 자신의 정보를 조회하고, `ANONYMOUS`는 401, `SIGNUP_REQUIRED`는 403을 받는다. |
| `MEMBER-004` | Unit / Domain, Service | 가입 완료 Member는 닉네임을 보유하며 리뷰·댓글 작성자 표시에서 사용할 수 있는 정보로 유지된다. 정확한 API Response 필드는 확정 후 추가 검증한다. |
| `MEMBER-005` | Service, Controller, Integration | 중복 닉네임 가입은 409이며 DB UNIQUE 제약도 중복 저장을 거부한다. |

### 영화

| 요구사항 ID | 테스트 계층 | 핵심 검증 시나리오 |
| --- | --- | --- |
| `MOVIE-001` | Service, Controller | 영화 검색 요청의 `query`가 Movie API Client로 전달되고 Fake/Mock TMDB 결과가 200으로 반환된다. 검색만으로 Movie를 저장하지 않는다. |
| `MOVIE-002` | Service, Controller | `tmdbId` 상세 요청이 Movie API Client로 전달되고 조회 결과 또는 대상 없음 응답이 HTTP 계약에 맞게 반환된다. |
| `MOVIE-003` | Service, Controller | `tmdbId` 추천 요청이 Movie API Client로 전달되고 추천 결과가 반환된다. |
| `MOVIE-004` | Service, Integration | 검색·상세·추천 조회는 Movie를 저장하지 않는다. 리뷰 작성 시 Movie가 있으면 재사용하고, 없으면 TMDB 최소 정보로 한 번만 저장하며 같은 `tmdbId` 중복은 거부된다. |

### 리뷰

| 요구사항 ID | 테스트 계층 | 핵심 검증 시나리오 |
| --- | --- | --- |
| `REVIEW-001` | Service, Controller / Security | `MEMBER`는 리뷰를 작성하고 작성자로 기록된다. `ANONYMOUS`는 401, `SIGNUP_REQUIRED`는 403이다. |
| `REVIEW-002` | Unit / Domain, Controller | `0.5~5.0`의 `0.5` 단위 값만 허용하고 내부 `1~10` 정수로 정확히 변환한다. 범위 밖 값과 잘못된 단위는 400이다. |
| `REVIEW-003` | Service, Controller | 인증 없이 영화별 리뷰 목록을 조회하고 기본 `page=0`, `size=20`, `createdAt DESC`가 적용된다. |
| `REVIEW-004` | Service, Controller | 인증 없이 존재하는 리뷰 상세를 조회하면 200, 존재하지 않으면 404다. |
| `REVIEW-005` | Service, Controller / Security | 작성자는 제목·본문·평점을 수정할 수 있고 작성자·영화는 바꿀 수 없다. 다른 Member는 403이며 없는 Review는 404다. |
| `REVIEW-006` | Service, Controller / Security, Integration | 작성자만 삭제할 수 있고 성공 시 204다. 다른 Member는 403, 없는 Review는 404이며 삭제 Transaction의 원자성을 검증한다. |
| `REVIEW-007` | Service, Controller, Integration | 같은 `Member + Movie`로 두 번째 리뷰를 만들면 409이며 DB UNIQUE 제약도 중복 저장을 거부한다. |
| `REVIEW-008` | Unit / Domain, Service | 영화 평균 평점은 Review 데이터로 계산하고 Movie에 평균 평점이나 리뷰 개수 파생 값을 저장하지 않는다. |

### 댓글

| 요구사항 ID | 테스트 계층 | 핵심 검증 시나리오 |
| --- | --- | --- |
| `COMMENT-001` | Service, Integration | Comment는 존재하는 하나의 Review와 Member에 연결되고 Review 없이 생성할 수 없으며 FK 무결성을 지킨다. |
| `COMMENT-002` | Service, Controller | 인증 없이 특정 Review의 Comment 목록을 조회할 수 있고 없는 Review는 404다. |
| `COMMENT-003` | Service, Controller / Security | `MEMBER`는 Comment를 작성하고 작성자로 기록된다. `ANONYMOUS`는 401, `SIGNUP_REQUIRED`는 403이며 없는 Review에는 작성할 수 없다. |
| `COMMENT-004` | Service, Controller / Security | 작성자만 Comment를 수정할 수 있다. 다른 Member와 Review 작성자는 403이며 없는 Comment는 404다. |
| `COMMENT-005` | Service, Controller / Security | 작성자만 Comment를 삭제할 수 있다. 다른 Member와 Review 작성자는 403이며 없는 Comment는 404다. |
| `COMMENT-006` | Service, Integration | 삭제된 Comment가 DB에서 제거되어 다시 조회되지 않으며 삭제 상태로 남지 않는다. Review 삭제 시 소속 Comment도 함께 제거된다. |

## Member 테스트 시나리오

- `SIGNUP_REQUIRED` 사용자는 유효하고 중복되지 않은 닉네임으로 가입할 수 있다.
- Session의 `provider + providerId`와 email을 사용하며 Request의 Identity 값은 받거나 신뢰하지 않는다.
- 닉네임이 중복되면 가입에 실패하고 409를 반환한다.
- 같은 `provider + providerId`로 이미 가입한 Member를 다시 생성하지 않는다.
- `ANONYMOUS`는 가입 완료 API를 사용할 수 없고 401을 받는다.
- 가입 성공 전에는 DB에 `PENDING Member`가 생성되지 않는다.
- `MEMBER`가 가입 API를 다시 호출하면 `SIGNUP_REQUIRED` 권한이 없으므로 403을 받는다.
- 가입 성공 후 같은 HTTP Session의 Principal이 `ROLE_MEMBER`와 생성된 `memberId`를 가진 상태로 교체된다.
- Request에 `provider`, `providerId`, `email`, `memberId`를 추가해도 Member Identity에는 Session Principal의 신뢰된 값만 사용한다.
- Application의 닉네임 중복 사전 검사와 별개로 PostgreSQL UNIQUE 위반도 가입 충돌로 변환하고 Transaction을 Rollback한다.

## Movie 테스트 시나리오

- 영화 검색 요청과 검색어가 Movie API Client에 전달된다.
- 영화 상세와 추천 요청의 `tmdbId`가 Movie API Client에 전달된다.
- 검색·상세·추천만으로 Movie가 DB에 저장되지 않는다.
- 리뷰 작성 시 같은 `tmdbId`의 Movie가 이미 존재하면 재사용한다.
- Movie가 없으면 Fake/Mock TMDB 응답의 최소 정보로 저장한다.
- 동일한 `tmdbId`의 Movie를 중복 생성하지 않는다.
- 일반 자동 테스트에서는 실제 TMDB를 호출하지 않는다.

## Review 테스트 시나리오

- `MEMBER`는 리뷰를 작성할 수 있다.
- `ANONYMOUS`는 리뷰 작성 시 401을 받는다.
- `SIGNUP_REQUIRED`는 리뷰 작성 시 403을 받는다.
- 한 Member는 같은 Movie에 리뷰를 두 번 작성할 수 없으며 두 번째 요청은 409다.
- 사용자 평점은 `0.5~5.0` 범위의 `0.5` 단위만 허용하고 내부 값 `1~10`으로 변환한다.
- 리뷰 작성자만 제목·본문·평점을 수정할 수 있고 작성자와 Movie는 변경할 수 없다.
- 리뷰 작성자만 리뷰를 삭제할 수 있다.
- 다른 Member의 수정·삭제 요청은 403이다.
- 존재하지 않는 Review의 조회·수정·삭제는 404다.
- Review 삭제 시 관련 Comment가 같은 Transaction의 정책에 따라 함께 삭제된다.
- 기본 목록 조회는 0페이지, 20개, 최신 작성순이다.

## Comment 테스트 시나리오

- `MEMBER`는 존재하는 Review에 Comment를 작성할 수 있다.
- `ANONYMOUS`는 작성 시 401, `SIGNUP_REQUIRED`는 403을 받는다.
- 존재하지 않는 Review에는 Comment를 작성할 수 없다.
- Comment 작성자만 자신의 Comment를 수정하거나 삭제할 수 있다.
- Review 작성자라도 다른 Member의 Comment를 수정하거나 삭제할 수 없고 403을 받는다.
- 존재하지 않는 Comment의 수정·삭제는 404다.
- Comment 삭제는 Hard Delete이며 삭제 후 DB에 남지 않는다.
- **Pending Test Scenario:** Review 삭제 시 소속 Comment도 함께 삭제되는지는 삭제 책임을 DB/JPA/Service 중 어디에 둘지 확정한 뒤 PostgreSQL Integration Test로 반드시 추가한다. 현재 Comment Entity/Repository 단계에서는 자동 Cascade를 전제하거나 성공 조건으로 강제하지 않는다.

## DB Constraint 테스트

Application Validation과 DB Constraint는 서로 다른 방어 계층이다. Service의 사전 검증이 있더라도 중요한 DB 무결성 규칙은 PostgreSQL 기반 Integration Test에서 검증한다.

현재 구현된 Member, Movie, Review, Comment에서는 다음 제약을 필수 DB Constraint 테스트 대상으로 유지한다.

| 대상 | 제약 | 검증 |
| --- | --- | --- |
| `member` | `UNIQUE(provider, provider_id)` | 같은 OAuth Identity의 두 번째 저장을 DB가 거부한다. |
| `member` | `UNIQUE(nickname)` | 같은 닉네임의 두 번째 저장을 DB가 거부한다. |
| `movie` | `UNIQUE(tmdb_id)` | 같은 TMDB 영화의 두 번째 저장을 DB가 거부한다. |
| `review` | `UNIQUE(member_id, movie_id)` | 같은 Member·Movie 리뷰의 두 번째 저장을 DB가 거부한다. |

향후 해당 도메인을 구현할 때 다음 핵심 제약도 PostgreSQL Integration Test 대상으로 추가한다.

| 대상 | 제약 | 검증 |
| --- | --- | --- |
| 핵심 FK | 참조 무결성 | Member·Movie 없는 Review와 Member·Review 없는 Comment 저장을 DB가 거부한다. |

모든 NOT NULL 컬럼마다 Reflection이나 Native Query로 Entity 경계를 우회하는 테스트를 작성하지 않는다. NOT NULL은 문서화된 Schema와 실제 PostgreSQL Schema 생성 결과로 보장하고, 장애 위험이나 회귀 가능성 때문에 필요성이 확인된 경우에 대표 테스트를 추가한다. Reflection 등의 우회 방법은 기본 테스트 패턴으로 사용하지 않는다. DB Constraint는 동시 요청과 우회 저장에 대한 최종 데이터 무결성 방어선이며, 테스트는 실패 후 잘못된 데이터가 남지 않았는지도 확인한다.

## Member Repository Test의 PostgreSQL 환경

`MemberRepositoryTests`와 `MovieRepositoryTests`는 `@PostgresRepositoryTest`를 사용해 실제 PostgreSQL Container에서 실행한다. DataSource 교체를 명시적으로 금지하며 H2는 테스트 Runtime Classpath에 없다. Migration 도구가 없는 현재 테스트 환경에서는 테스트 전용 설정으로 Hibernate `create-drop`을 사용해 Schema를 준비한다.

Member의 두 UNIQUE 제약은 실제 PostgreSQL에서 중복 저장을 거부하는지 검증한다. PostgreSQL Schema Metadata를 조회해 `created_at`이 `timestamptz`인지와 Member의 필수 컬럼이 NOT NULL인지 확인한다. 이는 Entity 값을 Reflection으로 우회하는 테스트가 아니라 생성된 Schema 자체를 검증하는 대표 테스트다.

Movie Repository Test도 같은 PostgreSQL 환경을 재사용한다. `tmdb_id` UNIQUE 위반과 nullable 규칙을 검증하고, `information_schema`를 조회해 `created_at`의 실제 타입이 `timestamptz`인지 확인한다.

Review는 DB 없는 Domain Test에서 내부 평점 `1~10` 경계를 검증한다. Repository Test에서는 Member·Movie 관계, 조합 UNIQUE, 실제 PostgreSQL FK·nullable 및 `created_at`·`updated_at`의 `timestamptz` 타입을 검증한다.

## 외부 TMDB API 테스트

`TmdbMovieClientTests`는 `MockRestServiceServer`를 사용해 실제 TMDB 네트워크와 Token 없이 실행한다.

- 검색 JSON을 `MovieSummary`로 매핑한다.
- 상세 JSON을 별도 `MovieDetail`로 매핑한다.
- 추천 JSON을 `MovieSummary` 목록으로 매핑한다.
- `Authorization: Bearer fake-test-token` Header를 검증한다.
- 빈 `release_date`를 `null`로 매핑한다.
- 404를 영화 없음 오류로 변환한다.
- 401/403을 TMDB 인증 오류로 변환한다.
- 5xx를 TMDB 서비스 장애 오류로 변환한다.

일반 자동 테스트는 `TMDB_ACCESS_TOKEN`이나 `api.themoviedb.org`의 가용성에 의존하지 않는다. 실제 TMDB 계약 Smoke Test는 수동 검증으로 분리한다.

## Docker Compose와 Testcontainers

- 로컬 개발 애플리케이션은 Docker Compose의 PostgreSQL을 사용한다.
- Repository / DB Integration Test는 기존 `postgres:17-alpine` Testcontainer를 사용한다.
- 자동 테스트를 실행하기 위해 Compose DB를 미리 띄우지 않는다.
- 두 환경 모두 PostgreSQL 17 계열을 사용하지만 생명주기와 데이터는 공유하지 않는다.

## 성능 테스트

- 기능 테스트와 성능 테스트를 분리한다.
- 성능 테스트 원칙과 결과는 `docs/PERFORMANCE.md`에서 관리한다.
- 향후 k6 등을 이용한 Smoke Test, Load Test, Stress Test를 별도 구성할 수 있다.
- 현재 기능 테스트 설계에서 목표 TPS 달성이나 성능 최적화를 전제하지 않는다.
- Query 수나 응답 성능 문제는 재현 가능한 측정 후 개선한다.

## 테스트 작성 규칙

- 운영 패키지 구조를 `src/test/java` 아래에 대응시킨다.
- 테스트 클래스 이름은 `*Tests`로 작성한다.
- 테스트 메서드는 `rejectsDuplicateNickname()`처럼 관찰 가능한 동작을 표현한다.
- 테스트끼리 실행 순서, 공유된 변경 가능 상태, 개발자 장비 설정에 의존하지 않는다.
- 실제 외부 서비스와 개발자 개인 자격 증명을 사용하지 않는다.
- 서버, Container 또는 포트를 여는 프로세스를 시작했다면 성공 여부와 관계없이 종료하고 포트가 해제됐는지 확인한다.

## 테스트 완료 규칙

향후 기능 구현은 다음 순서를 작업 완료 조건으로 사용한다.

1. 관련 `REQUIREMENTS.md` ID를 확인한다.
2. 해당 요구사항을 검증할 테스트를 작성한다.
3. 기능을 구현한다.
4. 관련 테스트를 실행한다.
5. `./gradlew test`로 전체 테스트를 실행한다.
6. 실패한 테스트가 있다면 원인을 분석하고 수정한다.
7. 모든 테스트가 통과한 뒤 작업 완료를 보고한다.

테스트를 생략하고 기능만 구현한 상태는 완료로 간주하지 않는다.

## 명령어

- `./gradlew test`: 전체 JUnit Platform 테스트 실행
- `./gradlew build`: 패키징 과정에서 전체 테스트 실행

## Google OIDC 테스트

- 실제 Google 서버와 개발자 자격 증명을 자동 테스트에서 사용하지 않는다.
- `OidcIdToken` 기반 Fixture와 위임 `OidcUserService` Mock을 사용한다.
- 동일 `sub`의 기존 Member는 `MEMBER`, 신규 사용자는 Member 생성 없이 `SIGNUP_REQUIRED`가 되는지 검증한다.
- email이 바뀌어도 같은 `sub`이면 기존 Member로 식별되는지 검증한다.
- 상태별 로그인 성공 Redirect가 설정된 Frontend URL을 사용하는지 검증한다.
- OIDC 인증 실패 응답은 고정된 401 오류이며 원래 예외 메시지나 Stack Trace를 포함하지 않는지 검증한다.

## 인증 완료 기능 테스트

- `ANONYMOUS`의 `/api/members/me` 요청은 401이다.
- `MEMBER`의 `/api/members/me` 요청은 200이며 Principal의 `memberId`에 대응하는 Member DTO를 반환한다.
- `SIGNUP_REQUIRED`의 `/api/members/me`, Review 작성, Comment 작성 요청은 403이다.
- 서비스 로그아웃은 현재 HTTP Session을 무효화하며 이후 Member API 요청은 401이다.
- 로그아웃은 Google 계정 로그아웃이나 Token revoke를 호출하지 않는다.

## Open Questions

- 운영 PostgreSQL Schema를 향후 어떤 Migration 도구로 관리할지
- TMDB Fake Fixture의 형식, 위치, 유지 관리 방식
- 테스트 데이터 Builder 또는 Factory 도입 여부와 범위
- Controller Test와 Integration Test의 경계
- Review 삭제 시 Comment 삭제 구현별 통합 테스트 구성
- 실제 TMDB 계약 테스트의 필요성, 실행 주기, 격리 환경
- Coverage 측정 도구와 최소 기준
- CI 품질 기준, 병렬 실행, 테스트 결과 보고 방식
