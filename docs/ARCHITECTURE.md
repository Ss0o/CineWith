# Architecture

## Observed Baseline

- Java 21 toolchain; Gradle wrapper 9.5.1; Spring Boot 4.0.7.
- Root package: `com.community.board`.
- One application entry point: `CommunityServiceApplication`.
- 설정은 Google OIDC 환경변수와 TMDB Client 환경변수를 사용하며 실제 Secret을 저장하지 않는다.
- Member, Movie, Review, and Comment entities and repositories가 구현되어 있다. Google OIDC 로그인과 회원 기능, `MovieClient`/`TmdbMovieClient`가 구현되어 있으며 콘텐츠 Service/Controller와 Migration은 아직 없다.

## Available Technical Capabilities

The build includes Spring Web MVC, Validation, Data JPA, Security, OAuth2 Client, PostgreSQL, Lombok, Springdoc OpenAPI, and Testcontainers support. H2 is no longer used.

## Initial Package Decision

Member, Movie, and Review use feature-first packages with `domain` and `repository` below the existing `com.community.board` root. This keeps domain changes focused while preserving the architecture-test boundaries. Broader module boundaries and dependency direction remain to be decided as more features are introduced.

## Persistence and Integrations

PostgreSQL is the confirmed V1 database engine. The PostgreSQL JDBC Driver is the runtime driver, and Repository / DB Integration Test uses a PostgreSQL Testcontainer connected through Spring Boot Service Connections. The shared `@PostgresRepositoryTest` explicitly disables test DataSource replacement, so an embedded database cannot silently replace PostgreSQL. Tests use Hibernate `create-drop` until a migration tool is selected; no migration tool or production datasource configuration exists yet.

로컬 개발환경은 `compose.yaml`의 backend와 `postgres:17-alpine`을 사용한다. backend는 Compose Service 이름인 `postgres`로 DB에 연결하고 healthcheck 통과 후 시작한다. Migration 도구가 아직 없어 Compose에서만 임시로 Hibernate `update`를 사용하며 production 정책이 아니다. Repository Integration Test는 Compose DB가 아니라 기존 PostgreSQL Testcontainers를 계속 사용한다.

TMDB 연동은 `MovieClient` Application 경계와 `TmdbMovieClient` Adapter로 분리한다. 외부 JSON DTO와 내부 조회 모델, 영속 Movie Entity는 서로 다른 모델이며 TMDB 조회만으로 Repository를 호출하지 않는다.
Service와 Controller는 TMDB 구현체인 `TmdbMovieClient`에 직접 의존하지 않고 `MovieClient` 경계를 사용하며, 이 의존 방향은 Architecture Test로 검증한다.

Member, Movie, and Review use `GenerationType.IDENTITY` for their internal `Long` primary keys. Movie keeps the external `tmdbId` as a separate UNIQUE business identifier, and Review enforces one row per Member and Movie. The migration strategy remains to be selected.
Review rating은 API·도메인·PostgreSQL에서 같은 `BigDecimal` 값을 사용하며 `numeric(2,1)`로 저장한다. 범위와 `0.5` 단위는 Review 도메인이 보장하고, DB CHECK 제약은 Migration 도구 선정 시 검토한다.

Review Application Use Case는 `ReviewService`의 트랜잭션 안에서 Member 확인, 필요 시 `MovieClient` 조회와 Movie 저장, 중복 확인, Review 저장을 수행한다. Review 저장이 실패하면 이 과정에서 생성한 Movie도 Rollback된다. Review 삭제는 양방향 컬렉션이나 Cascade를 추가하지 않고 `CommentRepository`로 소속 Comment를 먼저 명시적으로 삭제한 뒤 Review를 삭제한다.

## Architecture Decisions Required

- PostgreSQL datasource and migration strategy
- Package/module boundaries and domain layering
- Configuration profile strategy
- Logging, metrics, tracing, and deployment model
- Whether Lombok remains part of the coding standard
