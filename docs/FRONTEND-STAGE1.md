# 1단계: 기존 화면 보완

## 구현 범위

- 리뷰/댓글 수정·삭제는 로그인한 작성자에게만 표시한다.
- 리뷰 수정 폼은 제목·본문·평점을 편집하고 실패 시 입력을 보존한다. 평점은 0.5~5.0, 0.5 단위다.
- 리뷰/댓글의 이전·다음 페이지, 전체 건수, 로딩/빈 결과/오류/재시도를 제공한다.
- 댓글 등록 후 오래된 순 정렬의 마지막 페이지로 이동한다. 삭제로 현재 페이지가 없어지면 마지막 유효 페이지를 다시 조회한다.
- 미제공 평균 평점, 상영 시간, 좋아요 수, 회원 리뷰 수 등의 가짜 0을 제거한다. 한줄평처럼 미지원 탭도 제거한다.
- TMDB 영화 부가 정보가 실패해도 저장된 리뷰 제목·영화명·본문은 표시한다. 영화 상세의 리뷰와 추천도 독립 상태로 관리한다.
- 중복 제출을 억제하고, 상세 화면 이동 시 이전 비동기 응답이 현재 데이터를 덮어쓰지 않게 한다.

2~7단계 기능은 이 단계의 구현에 포함하지 않는다.

## 주요 파일

| 파일 | 책임 |
| --- | --- |
| `cinewith-frontend/src/views/ReviewDetailView.vue` | 작성자 버튼, 리뷰/댓글 편집·삭제, 댓글 페이지 상태와 제출 처리 |
| `cinewith-frontend/src/views/MovieDetailView.vue` | 영화별 리뷰 페이지, 영화/추천 독립 조회 상태 |
| `cinewith-frontend/src/views/ReviewWriteView.vue` | 작성/검색 로딩, 빈 결과, 입력 보존, 중복 제출 방지 |
| `cinewith-frontend/src/views/ReviewBoardView.vue` | 검색 실패 재시도, 오래된 검색 응답 무시 |
| `cinewith-frontend/src/composables/usePagedList.js` | 페이지 메타데이터, 재조회, 마지막 페이지 보정, 응답 순서 보호 |
| `cinewith-frontend/src/components/PaginationControls.vue` | 페이지 번호 표시와 이전/다음 이벤트 |
| `cinewith-frontend/src/utils/ownership.js` | UX용 작성자 표시 조건 |
| `cinewith-frontend/src/data/apiProvider.js` | HTTP 응답을 화면 모델로 변환하며 페이지 정보 보존 |
| `cinewith-frontend/tests/ScreenBehaviorTests.js` | 외부 통신 없는 화면 상태/API 계층 테스트 |

## 구조를 유지한 이유

백엔드가 이미 필요한 평점 수정, 페이지 조회, 소유권 검사를 제공하므로 새 Entity, Repository, Service 또는 API를 만들 필요가 없다. 화면은 DTO를 변환한 모델만 사용하고 Entity는 기존처럼 서버 안에 남는다.

현재 `Member.nickname`에는 UNIQUE 제약이 있고 변경 API가 없다. 따라서 `/api/members/me`의 닉네임과 콘텐츠 작성자 닉네임 비교로 버튼을 표시한다. 이것은 인증/인가가 아니다. 실제 수정·삭제는 기존 Service가 Session Principal에서 전달된 회원 ID를 DB의 작성자 ID와 비교한다. 닉네임 변경 도입 시에는 서버 계산 `editable` 등으로 표시 계약을 재검토한다.

`usePagedList`는 특정 API를 직접 알지 못하고 전달된 조회 함수를 호출한다. Vue 화면이 조회 대상 ID를 제공하고, 페이지 컴포넌트는 이동 이벤트만 발생시킨다. 별도의 상태 관리 라이브러리는 추가하지 않는다.

## 요청 흐름과 DB 동작

### 리뷰 페이지 이동

`MovieDetailView` → `usePagedList.load(page)` → `apiProvider.reviews.listByMovie` → `GET /api/movies/{tmdbId}/reviews?page=1&size=20` → `ReviewController` → `ReviewService.getByMovie` → `ReviewRepository.findByMovieTmdbId` → PostgreSQL 순서다.

Service는 `PageRequest`를 만들고 Entity를 트랜잭션 안에서 `ReviewView`로 변환한다. Controller는 Response DTO로 변환한다. 프론트엔드는 content와 함께 page/size/totalElements/totalPages를 유지한다. UI의 2페이지가 API의 page=1이다.

다음 SQL은 기존 JPA 조회의 의미를 표현한 형태이며 별칭, 선택 컬럼 및 실제 문법은 Hibernate에 따라 달라진다.

```sql
select r.*
from review r join movie m on m.id = r.movie_id
where m.tmdb_id = ?
order by r.created_at desc
offset ? rows fetch first ? rows only;

select count(r.id)
from review r join movie m on m.id = r.movie_id
where m.tmdb_id = ?;
```

Spring Data가 content 크기로 전체 건수를 알 수 있는 경우 count 쿼리를 생략할 수 있다. `ReviewView`가 작성자 닉네임과 영화명을 읽을 때 LAZY 관계 초기화 SELECT가 추가될 수 있다. 이 단계에서 Fetch Join이나 캐시는 도입하지 않았다.

### 댓글 조회·등록·삭제 후 페이지 보정

`ReviewDetailView` → `apiProvider.comments` → `CommentController` → `CommentService` → `CommentRepository`로 흐른다. 목록은 먼저 대상 리뷰 존재를 확인한 뒤 오래된 순으로 읽는다.

```sql
select ... from review where id = ?;
select ... from comment where review_id = ?
order by created_at asc offset ? rows fetch first ? rows only;
select count(*) from comment where review_id = ?;
```

등록은 현재 회원과 리뷰를 조회한 뒤 `insert into comment (...) values (...)`를 실행한다. 성공 후 화면은 목록을 다시 조회해 실제 전체 건수를 얻고 마지막 페이지로 이동한다. 화면에서 배열에 단순 append하면 서버 페이지 크기와 순서를 깨뜨릴 수 있으므로 재조회한다.

삭제는 현재 회원/댓글 조회와 작성자 검사를 거친 후 `delete from comment where id = ?`를 실행한다. 이후 현재 페이지를 다시 읽고 요청 페이지가 totalPages 범위를 벗어나면 `max(0, totalPages - 1)`을 조회한다. 빈 목록은 totalElements=0, totalPages=0으로 표현한다.

### 리뷰 수정

수정 폼 → 기존 CSRF 처리 `apiRequest` → `PATCH /api/reviews/{id}` → `ReviewController` → `ReviewService.update` → 회원/리뷰 조회 → 작성자 검사 → `Review.update` → 트랜잭션 flush 순서다.

```json
{ "title": "수정 제목", "content": "수정 본문", "rating": 4.5 }
```

관리 상태 Entity를 변경하기 때문에 별도의 `save` 호출 없이 dirty checking으로 `update review set ... rating=?, updated_at=? ... where id=?`가 발생한다. 실제 UPDATE 컬럼은 Hibernate 매핑에 따라 달라진다. 작성자가 아니면 변경 전에 예외가 발생하고 403으로 응답한다. 실패 시 화면의 수정 폼과 입력을 유지한다.

리뷰 삭제는 `ReviewService.delete`에서 소속 댓글을 먼저 제거하고 리뷰를 삭제한다. 기존 `deleteByReview`는 파생 삭제 메서드이므로 댓글을 조회한 뒤 엔티티별 DELETE가 발생할 수 있다. 단일 bulk DELETE라고 가정하지 않는다.

## DB 변경과 트랜잭션

DB 테이블·컬럼·UNIQUE/FK 변경 없음. 마이그레이션 및 새 의존성 없음.

| 기존 메서드 | 트랜잭션 | 이유 |
| --- | --- | --- |
| `ReviewService.get/getByMovie` | `@Transactional(readOnly = true)` | Entity 조회와 LAZY 관계를 사용하는 View 변환을 하나의 범위에서 수행 |
| `CommentService.getByReview` | `@Transactional(readOnly = true)` | 리뷰 존재 확인, 목록 조회, View 변환 |
| `ReviewService.create/update/delete` | `@Transactional` | 저장/dirty checking, 리뷰와 댓글 삭제의 원자성 |
| `CommentService.create/update/delete` | `@Transactional` | 작성자 검사와 저장·수정·삭제를 하나의 작업으로 처리 |

목록 HTTP 재조회는 앞선 저장 요청과 별도 트랜잭션이다. 전체 건수와 내용은 다른 사용자의 동시 작업에 따라 변할 수 있다. readOnly는 최적화 힌트이며 여러 쿼리에 동일 스냅샷을 자동 보장한다는 뜻이 아니다.

## 검증과 남은 주의점

- Node 내장 runner의 9개 테스트: 작성자/타인/익명/가입 미완료 표시 조건, 페이지 전달과 전체 건수, 삭제 후 보정, 빈 결과, 실패/재시도, 오래된 응답 무시, 미제공 통계 제외, 평점·CSRF 전송, 401/403 전파.
- `npm run build`로 Vue 템플릿과 번들 검증.
- `./gradlew test --no-daemon --rerun-tasks`로 기존 서버 전체 150개 테스트 통과(ArchUnit 5개 포함). 실제 Google/TMDB 호출 없이 PostgreSQL Testcontainers와 Mock 사용.
- N+1 가능성은 기존 LAZY View 변환에 남아 있다. 쿼리 수와 응답 시간을 측정한 뒤 개선한다.
- offset이 큰 페이지와 count 비용, 동시 등록/삭제 중 목록 이동, createdAt이 같은 항목의 정렬 안정성은 후속 측정/설계 대상이다.
- 중복 클릭 방지는 UX이며 서버의 UNIQUE와 소유권 검사를 대체하지 않는다.
- 격리된 Headless Chrome과 모의 API로 작성자/타인 버튼, 평점 수정, 수정 실패 입력 보존, 리뷰/댓글 페이지 이동, 추천 오류/재시도/빈 결과를 확인했다. 런타임 예외는 없었다.
- 실제 Google OAuth 로그인 전체 흐름과 실 TMDB 연결은 별도 수동 검증 대상이다.
