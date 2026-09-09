# 성능

## 원칙

- 성능 최적화 전에 기능 정확성을 먼저 확보한다.
- 재현 가능한 부하를 측정한 후에 성능을 개선한다.
- 검증하지 않은 최적화를 요구사항이나 완료된 개선으로 취급하지 않는다.
- 외부 영화 API 성능과 우리 서비스의 성능을 분리해서 측정한다.

## 측정 대상

다음 항목을 측정 대상으로 삼을 수 있다.

- API 응답시간
- 데이터베이스 Query 수
- 초당 처리 건수(TPS)
- 오류율

Comment 목록 Response에서 LAZY `Member`를 통해 작성자 닉네임을 읽으므로 N+1 가능성이 있다. 현재는 Review 목록과 마찬가지로 query 수를 실제 측정하지 않았으므로 Fetch Join이나 EntityGraph를 선행 적용하지 않는다.

Review와 Comment 목록의 `size` 최대값 100은 비정상적으로 큰 단일 DB 조회와 JSON Response를 제한하기 위한 입력 방어다. 100건 조회의 성능 안전성을 보장하는 값은 아니며, 측정 결과에 따라 더 낮게 조정할 수 있다.

측정 결과에는 부하 조건, 환경, 데이터 규모, 동시성, 측정 방법, 관련 의존성을 함께 기록해야 한다.

## 개선 기록

각 성능 변경은 다음 내용을 포함하는 Before / After 형식으로 기록한다.

- 재현 가능한 테스트 시나리오와 변경 전 결과
- 확인한 병목 지점과 근거
- 적용한 변경
- 같은 조건에서 측정한 변경 후 결과
- Trade-off와 운영 영향

## 부하 테스트

향후 부하 테스트를 통해 목표 TPS와 시스템 한계를 설정하고 검증한다. 현재 단계에서는 목표 TPS나 달성한 처리 용량이 확정되지 않았다.

## 미결정 사항

- TODO: 대표 부하 시나리오와 테스트 데이터 규모를 정의한다.
- TODO: 백분위 응답시간과 오류율 목표를 정의한다.
- TODO: 예상 사용 형태와 테스트 환경을 합의한 후 목표 TPS를 정의한다.
- TODO: 부하 테스트 및 관측 도구를 선정한다.

## 영화 평점 통계 조회 확인 (2단계, 2026-09-07)

PostgreSQL 17 Testcontainers와 실제 Hibernate StatementInspector로 `RatingStatisticsService.getByMovie` 호출을 확인했다. fixture 저장 SQL을 제외한 집계 SELECT는 2개(AVG/COUNT 1개, GROUP BY/COUNT 1개)이며 Review 본문이나 작성자 Entity를 조회하지 않는다. 요약 1행과 분포 최대 10행만 반환한다. 실제 SQL과 재현 테스트는 `docs/RATING-STATISTICS.md`에 기록한다.

이는 쿼리 형태/개수 검증이며 성능 개선 전후 비교나 100만 건 부하 측정은 아니다. 데이터 증가에 따른 두 스캔 비용, movie JOIN과 review 접근 계획, 데이터 편중, 긴 읽기 스냅샷 유지 비용이 측정 후보다. 새 인덱스나 캐시/집계 저장은 추가하지 않았다.

## 전체 리뷰 피드와 검색 SQL 확인 (3단계, 2026-09-09)

PostgreSQL 17 Testcontainers와 StatementInspector로 `ReviewService.getFeed("title", 0, 2)`를 확인했다. content query 1개와 count query 1개, 총 2개가 실행된다. content query가 Review·Movie·Member를 일반 JOIN하고 필요한 scalar 컬럼을 DTO로 반환하므로 페이지 크기에 따라 Member·Movie별 추가 SELECT가 생기지 않는다.

현재 offset page가 뒤로 갈수록 건너뛰는 행이 늘고, Page의 정확한 전체 건수에는 COUNT 비용이 든다. `lower(column) LIKE '%keyword%'`는 일반 B-tree 인덱스를 활용하기 어려워 데이터가 커지면 title/content/movie.title을 스캔할 수 있다. 실제 실행 계획과 데이터 규모, 분포, 응답 시간을 측정한 뒤 필요한 인덱스·검색 방식을 비교한다. Cursor Pagination, Full Text Search, pg_trgm, Redis와 검색 전용 인덱스는 아직 도입하지 않았다.
