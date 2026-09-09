# 2단계: 영화별 Cinewith 평점 통계

## 1. 구현한 기능

영화별 Cinewith 평균 평점, 리뷰 수와 0.5 단위 10개 평점 분포를 조회하고 영화 상세 화면에 표시한다. TMDB 평점이나 외부 추천과 분리된 Review 데이터의 통계다. 로딩·실패/재시도·리뷰 없음·정상 상태를 구분하며 평균 없는 영화를 0.0점으로 표시하지 않는다.

리뷰가 0개면 평균은 null, 전체 개수와 10개 분포는 모두 0이다. 로컬 Movie가 없는 TMDB ID도 같은 응답이다. 통계 요청은 TMDB의 영화 존재 확인이나 Movie 저장을 수행하지 않는다. 3단계 리뷰 피드는 구현하지 않는다.

## 2. 주요 변경 파일

- `review/repository/ReviewRepository.java`: 집계 JPQL 두 개.
- `review/repository/RatingSummary.java`, `RatingCount.java`: 집계 숫자 projection.
- `review/service/RatingStatisticsService.java`, `RatingStatisticsView.java`: 두 조회의 트랜잭션 경계와 API에 필요한 결과 구성.
- `review/controller/RatingStatisticsController.java`, `RatingStatisticsResponse.java`: Public HTTP 계약과 별도 Response DTO.
- `api/ApiExceptionHandler.java`: 숫자가 아닌 경로 인자도 공통 INVALID_REQUEST 응답으로 처리.
- `cinewith-frontend/src/components/MovieRatingStatistics.vue`: 평균/분포 표시 및 상태별 UI.
- `cinewith-frontend/src/composables/useRatingStatistics.js`: 조회/재시도와 영화 이동 시 오래된 응답 무시.
- `cinewith-frontend/src/data/apiProvider.js`, `views/MovieDetailView.vue`: 새 API 및 화면 연결.
- `src/test/java/com/community/board/review/integration/RatingStatisticsTests.java`, `cinewith-frontend/tests/RatingStatisticsTests.js`: PostgreSQL 통합 및 UI 상태 테스트.

## 3. API 구조

```http
GET /api/movies/550/rating-statistics
```

기존 영화 API는 Movie 내부 PK가 아닌 TMDB의 `tmdbId`를 URL에 사용한다. 이 계약을 유지한다. 양의 Long은 조회 가능하고 0/음수/타입 불일치는 400 INVALID_REQUEST다. 누구나 조회할 수 있다.

4.0점과 4.5점 리뷰 각 1개인 경우:

```json
{
  "averageRating": 4.25,
  "reviewCount": 2,
  "ratingDistribution": {
    "0.5": 0, "1.0": 0, "1.5": 0, "2.0": 0, "2.5": 0,
    "3.0": 0, "3.5": 0, "4.0": 1, "4.5": 1, "5.0": 0
  }
}
```

API 평균은 소수 둘째 자리 HALF_UP이고 화면은 소수 첫째 자리(4.3)로 표시한다. 분포 키는 소수 1자리의 고정 문자열이고 개수는 long이다. JSON 객체 키 순서에 의미를 의존하지 않으며 화면은 숫자로 정렬해 높은 평점부터 보여준다.

기존 `GET /api/movies/{tmdbId}`는 MovieClient를 통해 TMDB를 호출한다. 통계를 포함시키면 외부 영화 API 실패와 로컬 집계가 결합되므로 별도 엔드포인트를 선택했다. Review의 집계이므로 review 패키지의 조회 전용 Controller/Service를 사용하며 기존 CRUD Service에 통계 로직을 섞지 않는다.

## 4. Repository / 쿼리 설계

실제 `Review.rating`은 `BigDecimal`이고 `@Column(precision = 2, scale = 1)`로 PostgreSQL `numeric(2,1)`에 저장한다. 0.5~5.0 값을 그대로 사용하며 문서와 코드의 차이는 없다. 기존 Repository 스키마 테스트도 이를 확인한다.

방법 A를 선택했다.

1. `summarizeRatingsByTmdbId`: AVG와 COUNT를 DB에서 계산해 1행을 반환한다.
2. `countRatingsByTmdbId`: 평점별 GROUP BY와 COUNT를 DB에서 계산해 최대 10행을 반환한다.

SELECT 별칭과 interface projection getter를 맞춰 Entity 대신 집계 숫자를 받는다. `r.movie.tmdbId`는 객체 경로지만 SQL에서는 Movie와의 일반 JOIN으로 번역된다. 이것은 Entity 관계를 미리 로딩하는 Fetch Join이 아니다.

한 쿼리로 줄이기 위해 window function/native SQL을 도입하지 않았다. GROUP BY 결과 10행으로 Java에서 가중 평균을 구하는 선택지도 있지만, 이번 구현은 AVG까지 DB에서 수행하는 명확한 두 쿼리를 선택했다. 두 스캔/왕복 비용은 추후 측정할 항목이다.

JPQL AVG 반환 타입은 원본 필드가 BigDecimal이어도 Double이고 COUNT는 Long이다. 따라서 평균을 Java에서 `BigDecimal.valueOf`로 변환한 뒤 응답 정밀도에 맞춰 반올림한다. 이것은 평균 계산이 아니라 결과 표현 처리다. Double 변환은 임의 정밀도 십진 결과를 그대로 보존하는 계약이 아니므로, 향후 더 높은 숫자 정밀도가 필요한 도메인에는 그대로 일반화하지 않는다. [Jakarta Persistence 3.2, aggregate functions](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2).

GROUP BY는 4.0점 리뷰들을 하나의 그룹으로, 4.5점 리뷰들을 다른 그룹으로 나누어 각 그룹의 개수를 구한다. 리뷰가 없는 구간은 DB 결과에 행이 없으므로 Service가 고정 10개 구간을 0으로 초기화한 뒤 실제 count를 채운다.

## 5. 실제 SQL 확인

2026-09-07 PostgreSQL 17 Testcontainers에서 `StatementInspector`로 수집한 SQL이다. 읽기 편하게 줄바꿈만 추가했다.

```sql
select avg(r1_0.rating),count(r1_0.id)
from review r1_0
join movie m1_0 on m1_0.id=r1_0.movie_id
where m1_0.tmdb_id=?;

select r1_0.rating,count(r1_0.id)
from review r1_0
join movie m1_0 on m1_0.id=r1_0.movie_id
where m1_0.tmdb_id=?
group by r1_0.rating
order by r1_0.rating;
```

AVG, COUNT, GROUP BY 모두 PostgreSQL에서 수행된다. 요약 쿼리는 조건에 맞는 리뷰가 없어도 AVG=null, COUNT=0의 1행을 반환한다. 분포 쿼리는 리뷰가 없으면 0행이다. count(r.id)는 NOT NULL PK를 세므로 해당 리뷰 행 수와 같다.

`executesOnlyTwoAggregateSelectsWithoutHydratingReviewEntities` 테스트는 fixture 저장 후 SQL 수집 목록을 비우고 통계 호출만 측정한다. SQL이 정확히 2개이고 content/nickname 선택이 없음을 확인한다. 로그 위치는 `build/test-results/test/TEST-com.community.board.review.integration.RatingStatisticsTests.xml`의 `RATING_STATISTICS_SQL:`이다. 테스트 중 확인한 결과이며 운영 부하 성능 측정치는 아니다.

리뷰 10개와 100만 개의 차이:

| 항목 | DB 집계 | Review 전체 조회 후 Java 집계 |
| --- | --- | --- |
| DB에서 처리하는 데이터 | 대상 평점 행을 읽고 집계. 데이터가 늘면 비용 증가 | 대상 리뷰 전체 행을 읽음 |
| 애플리케이션에 전달 | 요약 1행 + 분포 최대 10행 | 10개 또는 100만 개의 리뷰 행 |
| 애플리케이션 메모리 | 이 집계 결과에 대해 입력 리뷰 수와 무관하게 작음 | Entity 생성·영속성 컨텍스트·본문 전송 등에 비례해 증가 |
| 추가 Entity 로딩 | 통계 조회 자체에는 없음 | 접근한 LAZY 관계에 따라 추가될 수 있음 |

쿼리 수가 항상 2개라고 실행 시간이 일정한 것은 아니다. 100만 개 규모에서 어떤 스캔/집계 계획과 시간이 나오는지는 EXPLAIN ANALYZE와 부하 측정이 필요하다. 이번 단계에서 100만 건 성능을 측정했다고 주장하지 않는다.

## 6. 요청 흐름

```text
MovieDetailView → MovieRatingStatistics
  → useRatingStatistics / apiProvider.movies.ratingStatistics
  → GET /api/movies/{tmdbId}/rating-statistics
  → RatingStatisticsController.getByMovie
  → RatingStatisticsService.getByMovie
  → ReviewRepository의 집계 JPQL 2개
  → PostgreSQL AVG / COUNT / GROUP BY
  → RatingSummary / RatingCount projection
  → RatingStatisticsView → RatingStatisticsResponse → Vue 평균/막대 표시
```

Controller는 입력 검증과 DTO 매핑만 하고 DB에 접근하지 않는다. Service가 읽기 작업의 경계와 빈 분포 정책을 결정하고 Repository가 조회식을 소유한다.

## 7. DB 변경사항

테이블, 컬럼, FK, UNIQUE, 인덱스 및 Migration 변경 없음. Movie에 averageRating/reviewCount/ratingDistribution을 추가하지 않았다. 통계 테이블/캐시/Materialized View도 없다.

## 8. 트랜잭션과 데이터 정합성

```java
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public RatingStatisticsView getByMovie(Long tmdbId)
```

기존 조회 Service와 같은 readOnly 정책을 유지한다. 이 작업은 집계 projection만 읽으며 수정할 관리 상태 Review Entity가 없으므로 Dirty Checking을 이용하지 않는다. readOnly 자체가 동일 스냅샷을 보장하는 것은 아니다.

두 쿼리가 기본 READ COMMITTED로 실행되면 요약을 읽은 후 다른 요청이 평점을 수정해 분포와 평균이 서로 다른 시점을 나타낼 수 있다. 이 조회에만 REPEATABLE READ를 적용해 첫 조회에서 정해진 스냅샷으로 둘 다 읽는다. PostgreSQL의 연속 SELECT 스냅샷 정책에 따른 선택이며 전역 격리 수준을 바꾸지 않는다. [PostgreSQL 17 transaction isolation](https://www.postgresql.org/docs/17/transaction-iso.html#XACT-REPEATABLE-READ).

테스트는 첫 AVG/COUNT 이후 두 번째 GROUP BY 직전에 다른 연결의 REQUIRES_NEW 트랜잭션으로 3.0→5.0을 수정하고 커밋한다. 진행 중인 통계는 평균 3.0/3.0 구간 1개를 유지하고 다음 통계 호출은 평균 5.0/5.0 구간 1개를 반환한다. sleep이나 스레드 실행 속도에 의존하지 않는 두 연결의 결정적 순서 검증이다.

현재 Controller는 외부 트랜잭션 없이 이 Service를 호출하므로 새 readOnly REPEATABLE READ 트랜잭션이 시작된다. 향후 다른 Service 트랜잭션에서 호출하면 기본 REQUIRED 전파로 바깥 트랜잭션의 격리 수준을 상속하므로 호출 경계를 다시 검토해야 한다.

기존 ReviewService.create/update/delete는 각 쓰기 트랜잭션에서 Review만 변경한다. commit된 후 시작하는 새 통계 조회가 그 데이터를 계산한다. Movie 통계를 별도 수정하거나 갱신 실패를 보상할 작업이 없으므로 이중 저장의 불일치가 없다. 단, 이미 시작한 통계 조회는 자신의 스냅샷을 유지하며 실시간 push로 화면이 갱신되는 기능은 없다. 영화 상세로 돌아오면 새 조회가 실행된다.

## 9. 테스트 결과

- PostgreSQL 통계 테스트 9개: 0개/로컬 Movie 없음, 1개, 여러 평점, 영화 독립성, 커밋된 생성/수정/삭제, 10개 구간, 반올림, SQL 2개, 조회 사이 동시 커밋, 잘못된 ID의 계약을 검증.
- 관련 테스트 통과 후 `./gradlew test --no-daemon` 실행: 전체 159개 통과, 실패/오류/건너뜀 0. 아키텍처 테스트 5개 포함.
- Frontend 통계 테스트 5개 추가, 기존 포함 전체 14개 통과. `npm run build` 통과.
- 격리된 Headless Chrome/모의 API로 로딩, 실패/재시도, 4.25→4.3 화면 표시, 10개 막대, 다른 영화 이동 후 리뷰 없음 확인. 런타임 예외 없음.
- 첫 실패 테스트에서 잘못된 타입 ID가 빈 Body의 400을 반환함을 발견해 기존 공통 ExceptionHandler에 타입 불일치 처리를 추가했다. 최종 테스트에서는 INVALID_REQUEST JSON을 검증한다.
- 실제 Google/TMDB 네트워크는 호출하지 않는다. 운영 부하 검증과 실제 외부 서비스 연결 검증은 수행하지 않았다.

## 10. 직접 읽어볼 코드 5개

| 파일·메서드 | 역할 / 없으면 생기는 문제 | DB에서 일어나는 일 |
| --- | --- | --- |
| `ReviewRepository.summarizeRatingsByTmdbId` | 평균·개수를 숫자 projection으로 조회. 없으면 별도 조회 구현이나 전체 Entity 집계가 필요 | Movie ID 조건으로 DB AVG/COUNT 실행 |
| `ReviewRepository.countRatingsByTmdbId` | 평점별 빈도 조회. 전체 count만으로는 분포를 알 수 없음 | GROUP BY rating으로 각 평점 행을 묶어 COUNT |
| `RatingStatisticsService.getByMovie` | 동일 스냅샷, 빈 구간 보정, 평균 반올림. 없으면 응답 정책과 트랜잭션이 흩어짐 | 하나의 읽기 트랜잭션에서 집계 SELECT 2개, DML 없음 |
| `RatingStatisticsController.getByMovie` | 양의 tmdbId 검증, Service 호출, Response 매핑. 없으면 HTTP 계약과 집계 경계가 연결되지 않음 | 직접 SQL을 실행하지 않으며 잘못된 인자는 Service 진입 전에 거절 |
| `RatingStatisticsTests.keepsSummaryAndDistributionConsistentWhenAnotherTransactionCommitsBetweenQueries` | 단순 readOnly와 동일 스냅샷의 차이를 검증. 없으면 동시 수정 시 혼합된 통계를 테스트로 발견하기 어려움 | 읽기 SELECT 사이에 다른 연결 UPDATE/commit 후 이전/다음 스냅샷 비교 |

## 11. 추후 성능 개선 후보

- 한 영화의 리뷰가 많으면 두 집계의 스캔 비용이 증가한다. 실제 실행계획/버퍼/응답시간과 데이터 편중을 먼저 측정한다.
- movie.tmdb_id 조건으로 JOIN한 뒤 review를 읽는 비용과 기존 인덱스 사용 여부를 확인한다. FK가 있다고 PostgreSQL이 참조 쪽 인덱스를 자동 생성한다고 가정하지 않는다.
- DB 처리량이 병목이면 한 번의 집계, 필요한 인덱스 등 후보의 전후 효과를 비교한다. 현재 구현에는 추가하지 않았다.
- 긴 REPEATABLE READ 조회가 오래된 MVCC 버전 유지에 주는 영향을 실제 부하에서 관찰한다. 외부 호출을 트랜잭션 안에 추가하지 않는다.
- 리뷰 목록과 통계는 별도 HTTP 요청/트랜잭션이므로 동시 변경 중 화면의 두 전체 건수가 잠시 다를 수 있다. 한 통계 응답 내부의 평균/개수/분포는 일관된다.
- Redis, Movie 집계 컬럼, 별도 통계 테이블, Materialized View, 비동기/배치 집계는 도입하지 않는다.
