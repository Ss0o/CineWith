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

Member, Movie, and Review use `GenerationType.IDENTITY` for their internal `Long` primary keys. Movie keeps the external `tmdbId` as a separate UNIQUE business identifier, and Review enforces one row per Member and Movie. The migration strategy remains to be selected.

## Architecture Decisions Required

- PostgreSQL datasource and migration strategy
- Package/module boundaries and domain layering
- Configuration profile strategy
- Logging, metrics, tracing, and deployment model
- Whether Lombok remains part of the coding standard
