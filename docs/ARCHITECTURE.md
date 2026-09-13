# Architecture

## Observed Baseline

- Java 21 toolchain; Gradle wrapper 9.5.1; Spring Boot 4.0.7.
- Root package: `com.community.board`.
- One application entry point: `CommunityServiceApplication`.
- 설정은 Google OIDC 환경변수와 TMDB Client 환경변수를 사용하며 실제 Secret을 저장하지 않는다.
- Member, Movie, Review, Comment의 Entity, Repository, Application Service와 HTTP Controller가 구현되어 있다. Google OIDC 로그인과 회원 기능, `MovieClient`/`TmdbMovieClient`가 구현되어 있으며 Migration은 아직 없다.

## Available Technical Capabilities

`cinewith-frontend/`는 Vue 3와 Vue Router를 사용하는 독립 SPA다. Vue는 화면과 상태 관리를, Vue Router는 화면 이동을 담당하며 Vite와 Vue Plugin은 개발 서버 및 정적 번들 빌드를 제공한다. 프론트엔드 의존성은 npm lockfile로 관리하고 Gradle 의존성에는 추가하지 않는다. 검증은 해당 디렉터리의 `npm run build`로 수행하며 백엔드 테스트와 별도로 실행한다.

개발 서버는 `/api`, `/oauth2`, `/login`을 로컬 백엔드에 프록시한다. 운영 정적 파일 제공과 reverse proxy 구성은 TODO이며 현재 Gradle 및 Docker 이미지 빌드에는 프론트엔드 번들을 포함하지 않는다.

The build includes Spring Web MVC, Validation, Data JPA, Security, OAuth2 Client, PostgreSQL, Lombok, Springdoc OpenAPI, and Testcontainers support. H2 is no longer used.

## Initial Package Decision

Member, Movie, and Review use feature-first packages with `domain` and `repository` below the existing `com.community.board` root. This keeps domain changes focused while preserving the architecture-test boundaries. Broader module boundaries and dependency direction remain to be decided as more features are introduced.

## Persistence and Integrations

PostgreSQL is the confirmed V1 database engine. The PostgreSQL JDBC Driver is the runtime driver, and Repository / DB Integration Test uses a PostgreSQL Testcontainer connected through Spring Boot Service Connections. The shared `@PostgresRepositoryTest` explicitly disables test DataSource replacement, so an embedded database cannot silently replace PostgreSQL. Tests use Hibernate `create-drop` until a migration tool is selected; no migration tool or production datasource configuration exists yet.

로컬 개발환경은 `compose.yaml`의 backend와 `postgres:17-alpine`을 사용한다. backend는 Compose Service 이름인 `postgres`로 DB에 연결하고 healthcheck 통과 후 시작한다. Migration 도구가 아직 없어 Compose에서만 임시로 Hibernate `update`를 사용하며 production 정책이 아니다. Repository Integration Test는 Compose DB가 아니라 기존 PostgreSQL Testcontainers를 계속 사용한다.

TMDB 연동은 `MovieClient` Application 경계와 `TmdbMovieClient` Adapter로 분리한다. 외부 JSON DTO와 내부 조회 모델, 영속 Movie Entity는 서로 다른 모델이며 TMDB 조회만으로 Repository를 호출하지 않는다. 영화 상세는 기본 상세와 `credits`를 한 외부 요청으로 받아 조회 모델에만 담고, 출연진은 표시 순서 상위 12명, 제작진은 연출·각본 부서로 한정한다.
Service와 Controller는 TMDB 구현체인 `TmdbMovieClient`에 직접 의존하지 않고 `MovieClient` 경계를 사용하며, 이 의존 방향은 Architecture Test로 검증한다.

KOFIC 보조 정보는 `KoficClient` 경계와 `KoficRestClient` Adapter로 분리한다. `MovieController`는 기존 `MovieClient`로 TMDB 기본 정보를 확보한 후에만 KOFIC 경계에 전달한다. KOFIC Adapter는 제목의 공백·대소문자를 정규화해 단일 후보를 선택한다. 동명 후보는 TMDB 개봉일과 완전히 일치하는 후보를 우선하며, 전 세계 최초 개봉일과 한국 개봉일이 다를 수 있으므로 같은 연도의 후보가 하나일 때만 보조로 선택한다. KOFIC 영화코드·영화상세·전일 전국 박스오피스는 조회 중에만 사용하며, `Movie` Entity나 Repository에 저장하지 않는다. KOFIC 결과는 보조 정보이므로 키 미설정·통신 오류는 `UNAVAILABLE` 상태로 응답해 TMDB 영화 상세 조회를 가리지 않는다.

Member, Movie, and Review use `GenerationType.IDENTITY` for their internal `Long` primary keys. Movie keeps the external `tmdbId` as a separate UNIQUE business identifier, and Review enforces one row per Member and Movie. The migration strategy remains to be selected.
Review rating은 API·도메인·PostgreSQL에서 같은 `BigDecimal` 값을 사용하며 `numeric(2,1)`로 저장한다. 범위와 `0.5` 단위는 Review 도메인이 보장하고, DB CHECK 제약은 Migration 도구 선정 시 검토한다.

Review Application Use Case는 `ReviewService`의 트랜잭션 안에서 Member 확인, 필요 시 `MovieClient` 조회와 Movie 저장, 중복 확인, Review 저장을 수행한다. Review 저장이 실패하면 이 과정에서 생성한 Movie도 Rollback된다. Review 삭제는 양방향 컬렉션이나 Cascade를 추가하지 않고 `CommentRepository`로 소속 Comment를 먼저 명시적으로 삭제한 뒤 Review를 삭제한다.

Comment Application Use Case는 `CommentService`가 현재 Principal의 `memberId`로 Member를 다시 조회하고 Review·Comment 존재 여부와 Comment 작성자 소유권을 검증한다. 변경 Use Case는 Transaction 안에서 처리하고 목록은 단순 derived query와 Offset Pagination을 사용한다. Member·Review 역방향 컬렉션, Cascade, Fetch Join과 EntityGraph는 추가하지 않는다.

## Architecture Decisions Required

- PostgreSQL datasource and migration strategy
- Package/module boundaries and domain layering
- Configuration profile strategy
- Logging, metrics, tracing, and deployment model
- Whether Lombok remains part of the coding standard

## 프론트엔드 페이지 상태 (1단계)

`apiProvider`는 기존 HTTP 응답을 화면 모델로 변환하며 페이지 메타데이터를 유지한다. `usePagedList`는 Vue 반응성 상태로 로딩/오류/페이지와 오래된 비동기 응답을 관리한다. `PaginationControls`는 페이지 번호와 이동 이벤트만 담당한다. 영화 상세는 TMDB의 배경·줄거리·출연·제작진을 기본 콘텐츠로, Cinewith 통계와 KOFIC 보조 정보 및 추천·리뷰 목록은 독립 상태로 표현한다. 백엔드 Controller/Service/Repository 및 DB 구조, 의존성은 변경하지 않는다.

## 영화별 리뷰 평점 집계 (2단계)

집계 원천이 Review이므로 `review.controller.RatingStatisticsController`와 `review.service.RatingStatisticsService`를 둔다. 기존 Review CRUD Service와 분리해 조회 책임을 명확히 한다. URL은 기존 영화별 리뷰 목록과 같이 외부 `tmdbId`를 사용한다.

`ReviewRepository`의 JPQL AVG/COUNT 요약과 GROUP BY/COUNT 분포 쿼리 2개를 사용한다. 집계 projection은 `RatingSummary`, `RatingCount`이며 Review Entity를 로드하지 않는다. Service의 View와 Controller의 Response DTO를 구분한다. JPQL AVG 결과는 Double이므로 API의 BigDecimal로 변환 후 소수 둘째 자리 HALF_UP 반올림한다.

이 Service 조회에만 `@Transactional(readOnly = true, isolation = REPEATABLE_READ)`를 적용해 두 쿼리가 PostgreSQL에서 동일 스냅샷을 읽게 한다. 기존 변경 트랜잭션 정책과 전역 isolation은 바꾸지 않는다. 현재 Controller가 트랜잭션 없이 Service를 호출하므로 이 경계에서 새 트랜잭션이 시작된다. 향후 다른 트랜잭션에서 재사용할 때는 REQUIRED 전파가 기존 isolation을 상속한다는 점을 검토해야 한다.

TMDB 상세 API에 합치지 않고 별도 통계 API로 제공하여 외부 장애와 집계를 분리한다. Vue `MovieRatingStatistics`가 통계 조회와 그래프를 담당하며 영화 상세에 포함된다. 통계 실패가 영화/리뷰 목록을 가리지 않고 다른 영화로 이동할 때 오래된 응답을 무시한다. 캐시·통계 저장·새 인덱스·의존성은 추가하지 않는다.

## 전체 리뷰 피드와 검색 (3단계)

`GET /api/reviews`는 Review의 전체 피드 전용 조회다. `ReviewRepository`가 JPQL constructor expression으로 `ReviewFeedItem`만 선택하고, `ReviewService.getFeed`가 `Page<ReviewFeedItem>`을 Service/View 및 Controller Response로 변환한다. Controller는 Repository projection에 직접 의존하지 않는다.

Review의 `member`, `movie`는 LAZY이므로 Entity `ReviewView.from`으로 목록을 변환하면 Member/Movie별 추가 조회(N+1)가 발생할 수 있다. 전체 피드는 필요한 scalar 컬럼을 content query의 일반 JOIN으로 함께 선택해 이를 피한다. ToMany 연관관계가 없으므로 Fetch Join은 가능할 수 있지만, 페이징 목록에 맞춘 DTO projection이 필요한 데이터와 SQL 형태를 가장 명확히 제한한다.

검색어가 없을 때와 있을 때는 각각 별도 JPQL을 사용한다. null 파라미터와 `lower`를 한 SQL에 혼합해 PostgreSQL의 타입 추론에 의존하지 않는다. 검색은 Service에서 trim하며 빈 값은 일반 피드로, 그 외에는 bound parameter로 `title`, `content`, `movie.title`의 lower/LIKE 조건을 실행한다. 기본 정렬은 `createdAt DESC, id DESC`이고 PageRequest는 offset/limit만 적용한다. Vue `ReviewFeedView`는 URL query `q`를 검색 상태로 사용하므로 페이지 이동에는 검색어가 유지되고 `usePagedList`의 request version이 이전 응답을 무시한다.

## 영화 탐색 홈 (3.5단계)

`MovieDiscoveryController → MovieDiscoveryService → MovieClient` 경계로 외부 호출을 분리한다. 현재 상영·개봉 예정은 `MoviePopularityPolicy`가 TMDB 인기도를 우선하고 개봉일과 TMDB ID로 안정적으로 정렬한다. 홈 API는 외부 영화 섹션만 집계하고 최신 리뷰는 기존 API로 독립 조회한다. Caffeine cache는 완전한 홈 결과만 저장하며 TMDB 섹션 실패가 포함된 응답은 캐시하지 않는다.
