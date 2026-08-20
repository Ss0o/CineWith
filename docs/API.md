# API

## 목적과 범위

이 문서는 community-service V1의 HTTP API 계약을 정의한다. 구체적인 Controller, Request·Response DTO 클래스, Service 구현은 이 문서의 범위에 포함하지 않는다.

## 기본 원칙

- REST 스타일의 HTTP API를 사용한다.
- 모든 애플리케이션 API는 `/api` Prefix를 사용한다.
- Entity를 API Response로 직접 노출하지 않는다.
- 요청과 응답에는 별도의 Request·Response DTO를 사용한다.
- 클라이언트가 보낸 `memberId`를 인증이나 인가 근거로 사용하지 않는다.
- 현재 `Member`는 Spring Security 인증 컨텍스트를 기준으로 식별한다.
- Session과 Cookie 기반 인증을 사용하며, 상태 변경 요청에는 `SECURITY.md`의 CSRF 정책을 적용한다.

## Endpoint 목록

| Method | Path | 목적 | 접근 권한 | 성공 상태 |
| --- | --- | --- | --- | --- |
| `POST` | `/api/members/signup` | 닉네임 설정 및 회원가입 완료 | `SIGNUP_REQUIRED` | `201 Created` |
| `GET` | `/api/members/me` | 현재 회원 정보 조회 | `MEMBER` | `200 OK` |
| `POST` | `/api/logout` | 현재 서비스 Session 로그아웃 | 인증된 사용자 | `204 No Content` |
| `GET` | `/api/movies/search?query={query}` | TMDB 영화 검색 | Public | `200 OK` |
| `GET` | `/api/movies/{tmdbId}` | TMDB 영화 상세 조회 | Public | `200 OK` |
| `GET` | `/api/movies/{tmdbId}/recommendations` | TMDB 추천 영화 조회 | Public | `200 OK` |
| `GET` | `/api/movies/{tmdbId}/reviews` | 특정 영화의 리뷰 목록 조회 | Public | `200 OK` |
| `GET` | `/api/reviews/{reviewId}` | 리뷰 상세 조회 | Public | `200 OK` |
| `POST` | `/api/reviews` | 리뷰 작성 | `MEMBER` | `201 Created` |
| `PATCH` | `/api/reviews/{reviewId}` | 리뷰 수정 | 작성자 `MEMBER` | `200 OK` |
| `DELETE` | `/api/reviews/{reviewId}` | 리뷰 삭제 | 작성자 `MEMBER` | `204 No Content` |
| `GET` | `/api/reviews/{reviewId}/comments` | 특정 리뷰의 댓글 목록 조회 | Public | `200 OK` |
| `POST` | `/api/reviews/{reviewId}/comments` | 댓글 작성 | `MEMBER` | `201 Created` |
| `PATCH` | `/api/comments/{commentId}` | 댓글 수정 | 작성자 `MEMBER` | `200 OK` |
| `DELETE` | `/api/comments/{commentId}` | 댓글 삭제 | 작성자 `MEMBER` | `204 No Content` |

Public API는 `ANONYMOUS`, `SIGNUP_REQUIRED`, `MEMBER` 모두 사용할 수 있다. `MEMBER` 전용 API에 대한 `ANONYMOUS`와 `SIGNUP_REQUIRED`의 응답은 상태 코드 정책을 따른다.

## Member API

### POST /api/members/signup

Google OIDC 인증은 완료했지만 아직 `Member`가 없는 `SIGNUP_REQUIRED` 사용자가 커뮤니티 닉네임을 설정하고 회원가입을 완료한다.

Request DTO:

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `nickname` | 예 | 커뮤니티에서 사용할 중복되지 않는 닉네임 |

Request에는 `provider`, `providerId`, `email`, `memberId`를 받지 않는다. 이 값들은 OAuth 인증 후 HTTP Session에 보관된 신뢰할 수 있는 가입 Identity를 사용한다.

- 성공: `201 Created`
- 닉네임 중복: `409 Conflict`
- `ANONYMOUS` 요청: `401 Unauthorized`
- 이미 가입한 `MEMBER`의 재요청: `403 Forbidden`. 인증은 되어 있지만 `SIGNUP_REQUIRED` 전용 기능에 대한 권한이 없기 때문이다.

성공 Response Body는 없으며, 가입 직후 현재 HTTP Session의 인증 상태가 `MEMBER`로 전환된다.

### GET /api/members/me

Spring Security 인증 컨텍스트의 현재 `Member` 정보를 조회한다. 클라이언트가 조회 대상 `memberId`를 지정하지 않는다.

- 접근 권한: `MEMBER`
- 성공: `200 OK`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 요청: `403 Forbidden`

Response DTO:

| 필드 | 설명 |
| --- | --- |
| `provider` | 로그인 공급자. V1은 `GOOGLE` |
| `email` | Google에서 가입 시 확보한 이메일. nullable |
| `nickname` | 커뮤니티 닉네임 |
| `createdAt` | UTC 기준 가입 완료 시각 |

서비스 내부 `member.id`와 Google `providerId(sub)`는 응답에 노출하지 않는다.

### POST /api/logout

현재 서비스 HTTP Session을 무효화하고 인증 정보를 제거한다.

- 인증된 요청은 현재 Session을 무효화한다. Session이 없는 요청도 멱등적으로 성공 처리한다.
- 성공: `204 No Content`
- CSRF 보호 적용

Google 계정 자체에서 로그아웃하거나 Google Token을 revoke하지 않는다.

## Movie API

Movie API는 우리 DB의 내부 `movie.id`가 아니라 TMDB의 `tmdbId`를 외부 식별자로 사용한다. 리뷰가 없는 영화는 서비스 DB에 존재하지 않을 수 있지만 검색·상세·추천 조회는 가능해야 하기 때문이다.

### GET /api/movies/search?query={query}

TMDB API를 사용해 `query`와 일치하는 영화를 검색한다.

- 접근 권한: Public
- 성공: `200 OK`
- 잘못된 검색 입력: `400 Bad Request`

### GET /api/movies/{tmdbId}

`tmdbId`를 기준으로 TMDB 영화 상세 정보를 조회한다.

- 접근 권한: Public
- 성공: `200 OK`
- 대상 영화 없음: `404 Not Found`

### GET /api/movies/{tmdbId}/recommendations

`tmdbId`를 기준으로 TMDB 추천 영화 정보를 조회한다.

- 접근 권한: Public
- 성공: `200 OK`
- 대상 영화 없음: `404 Not Found`

Movie Response에서 노출할 정확한 TMDB 필드는 아직 결정하지 않는다.

## Review API

### GET /api/movies/{tmdbId}/reviews

특정 TMDB 영화의 리뷰 목록을 Offset Pagination으로 조회한다.

Query Parameter:

| 이름 | 기본값 | 설명 |
| --- | --- | --- |
| `page` | `0` | 0부터 시작하는 페이지 번호 |
| `size` | `20` | 페이지당 리뷰 수 |

- 기본 정렬: `createdAt DESC`
- 접근 권한: Public
- 성공: `200 OK`

목록 Response의 정확한 필드와 페이지 메타데이터 구조는 아직 결정하지 않는다.

### GET /api/reviews/{reviewId}

서비스 DB의 `reviewId`를 기준으로 리뷰 상세 정보를 조회한다.

- 접근 권한: Public
- 성공: `200 OK`
- 대상 리뷰 없음: `404 Not Found`

### POST /api/reviews

현재 로그인한 `MEMBER`가 특정 TMDB 영화에 리뷰를 작성한다.

Request DTO:

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `tmdbId` | 예 | 리뷰 대상 TMDB 영화 ID |
| `title` | 예 | 리뷰 제목 |
| `content` | 예 | 리뷰 본문 |
| `rating` | 예 | 사용자 관점의 `0.5`부터 `5.0`까지 `0.5` 단위 평점 |

`memberId`는 Request에 포함하지 않는다. 작성자는 Spring Security 인증 컨텍스트에서 얻는다. API의 평점은 서버 내부 도메인에서 `1`부터 `10`까지의 정수로 변환한다.

작성 흐름:

1. Spring Security 인증 컨텍스트에서 현재 `Member`를 얻는다.
2. `tmdbId`에 해당하는 `Movie`가 DB에 존재하는지 확인한다.
3. 없다면 TMDB에서 서비스에 필요한 최소 정보를 조회해 `Movie`를 생성한다.
4. 동일한 `member + movie`의 `Review`가 존재하는지 확인한다.
5. 존재하지 않으면 `Review`를 생성한다.

- 성공: `201 Created`
- 잘못된 Request 또는 평점: `400 Bad Request`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 요청: `403 Forbidden`
- 동일 회원·영화 리뷰 중복: `409 Conflict`
- TMDB 대상 영화 없음: `404 Not Found`

### PATCH /api/reviews/{reviewId}

현재 `Member`가 작성한 리뷰를 수정한다.

수정 가능 필드:

- `title`
- `content`
- `rating`

작성자와 대상 `Movie`는 변경할 수 없다. Request에 `memberId`, `tmdbId`, `movieId`를 받지 않는다.

- 성공: `200 OK`
- 잘못된 Request 또는 평점: `400 Bad Request`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 또는 작성자가 아닌 `MEMBER`: `403 Forbidden`
- 대상 리뷰 없음: `404 Not Found`

PATCH에서 각 수정 가능 필드의 생략과 `null`을 어떻게 해석할지는 아직 결정하지 않는다.

### DELETE /api/reviews/{reviewId}

작성자만 리뷰를 삭제할 수 있다. 리뷰를 삭제하면 해당 리뷰에 속한 댓글도 함께 삭제한다. DB Cascade, JPA Cascade, `orphanRemoval`, Service 명시적 삭제 중 어떤 구현을 사용할지는 이 API 계약에서 결정하지 않는다.

- 성공: `204 No Content`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 또는 작성자가 아닌 `MEMBER`: `403 Forbidden`
- 대상 리뷰 없음: `404 Not Found`

## Comment API

### GET /api/reviews/{reviewId}/comments

특정 리뷰의 댓글 목록을 조회한다.

- 접근 권한: Public
- 성공: `200 OK`
- 대상 리뷰 없음: `404 Not Found`

V1에서 댓글 목록의 Pagination 사용 여부는 아직 결정하지 않는다.

### POST /api/reviews/{reviewId}/comments

현재 로그인한 `MEMBER`가 URL의 `reviewId`에 해당하는 리뷰에 댓글을 작성한다.

Request DTO:

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `content` | 예 | 댓글 내용 |

Request Body에 `memberId`와 `reviewId`를 받지 않는다. 작성자는 Spring Security 인증 컨텍스트, 대상 리뷰는 URL의 `reviewId`를 사용한다.

- 성공: `201 Created`
- 잘못된 Request: `400 Bad Request`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 요청: `403 Forbidden`
- 대상 리뷰 없음: `404 Not Found`

### PATCH /api/comments/{commentId}

현재 `Member`가 작성한 댓글의 `content`를 수정한다. 작성자만 수정할 수 있다.

Request DTO:

| 필드 | 필수 | 설명 |
| --- | --- | --- |
| `content` | 예 | 변경할 댓글 내용 |

- 성공: `200 OK`
- 잘못된 Request: `400 Bad Request`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 또는 작성자가 아닌 `MEMBER`: `403 Forbidden`
- 대상 댓글 없음: `404 Not Found`

### DELETE /api/comments/{commentId}

작성자만 자신의 댓글을 삭제할 수 있다. 리뷰 작성자는 다른 회원의 댓글을 삭제할 수 없다. V1에서는 Hard Delete로 처리한다.

- 성공: `204 No Content`
- `ANONYMOUS` 요청: `401 Unauthorized`
- `SIGNUP_REQUIRED` 또는 작성자가 아닌 `MEMBER`: `403 Forbidden`
- 대상 댓글 없음: `404 Not Found`

## 상태 코드 정책

| 상태 코드 | 적용 사례 |
| --- | --- |
| `200 OK` | 정상 조회, 정상 수정 |
| `201 Created` | Member 가입 완료, Review 생성, Comment 생성 |
| `204 No Content` | Review 삭제, Comment 삭제 |
| `400 Bad Request` | Validation 실패, 잘못된 입력 |
| `401 Unauthorized` | 인증되지 않은 사용자의 Member 전용 요청 |
| `403 Forbidden` | 인증은 완료했으나 필요한 상태·권한·소유권이 없는 요청 |
| `404 Not Found` | Movie, Review, Comment 등 대상이 존재하지 않음 |
| `409 Conflict` | 닉네임 중복, 동일 Member·Movie Review 중복 |

## Error Response

오류 응답은 다음 기본 구조를 사용한다.

```json
{
  "code": "ERROR_CODE",
  "message": "사용자 또는 개발자가 이해할 수 있는 메시지"
}
```

Validation 오류에는 필드 오류 정보를 추가할 수 있다. 필드 오류의 정확한 구조와 구체적인 Java `ErrorResponse` 구현은 아직 결정하지 않는다.

## Pagination

- 리뷰 목록은 V1에서 Offset Pagination을 사용한다.
- 기본 페이지는 `page = 0`이다.
- 기본 페이지 크기는 `size = 20`이다.
- 기본 정렬은 `createdAt DESC`다.
- Cursor Pagination은 V1에서 사용하지 않는다.
- 실제 성능 측정으로 문제가 확인된 경우에만 다른 방식을 검토한다.

## ID 노출 정책

| 리소스 | 외부 식별자 |
| --- | --- |
| Movie | TMDB `tmdbId` |
| Review | 서비스 DB `reviewId` |
| Comment | 서비스 DB `commentId` |

Member의 내부 DB ID는 일반 클라이언트 요청에서 직접 사용할 필요가 없도록 설계한다. 인증과 소유권 판단에는 Spring Security 인증 컨텍스트의 현재 `Member`를 사용한다.

## V1에서 제공하지 않는 API

- 좋아요
- 팔로우
- 대댓글
- 회원 탈퇴
- 관리자 및 모더레이션
- 이미지 직접 업로드
- 개인화 추천

## Open Questions

- Review 목록 Response의 정확한 필드와 페이지 메타데이터 구조
- Review 상세 및 생성·수정 성공 Response의 정확한 필드
- Member 가입 및 내 정보 Response의 정확한 필드
- Movie 상세 Response에서 TMDB 필드를 어디까지 노출할지
- Comment 목록과 개별 Comment Response의 정확한 필드
- Comment Pagination 적용 여부
- 닉네임, Review 제목·content, Comment content의 길이 제한
- PATCH Request에서 필드 생략과 명시적 `null`을 구분하는 방법
- 배포 환경별 OAuth 성공 후 프론트엔드 Redirect URL의 실제 값
- CSRF Token 전달 API와 SPA 교환 방식
- Error Code 명명 규칙과 Validation Field Error 구조
- 날짜·시간 및 Enum의 외부 표현 방식
- OpenAPI 생성·게시 정책과 API 호환성·폐기 정책
