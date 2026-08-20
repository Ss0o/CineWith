# 외부 서비스 연동

## 확정 범위

- V1의 외부 영화 데이터 공급자는 TMDB다.
- 영화 검색, 기본 상세 정보와 추천 영화 정보는 TMDB API를 원천으로 사용한다.
- 서비스 DB에는 실제 리뷰가 작성된 영화의 최소 정보만 저장한다. 검색, 상세, 추천 조회만으로는 영화를 저장하지 않는다.
- 성능 테스트에서는 TMDB 성능과 우리 애플리케이션 성능을 구분해서 측정한다.

## TMDB Client 구현

- Application 경계는 `MovieClient`이며 영화 검색, 상세, 추천 조회만 제공한다.
- 실제 구현은 Spring MVC와 같은 동기식 모델의 `RestClient`를 사용하는 `TmdbMovieClient`다. WebFlux 의존성은 추가하지 않는다.
- TMDB API Read Access Token을 `Authorization: Bearer {token}` Header로 전송한다. API Key Query Parameter 방식은 사용하지 않는다.
- Token은 `TMDB_ACCESS_TOKEN` 환경 변수에서만 주입한다.
- 기본 Base URL은 `https://api.themoviedb.org`이며 `TMDB_BASE_URL`로 변경할 수 있다.
- `TMDB_LANGUAGE`와 `TMDB_REGION`은 값이 있을 때만 Query Parameter로 보낸다. V1의 언어·지역 기본 정책은 확정하지 않는다.
- 연결 Timeout과 응답 Timeout은 각각 `TMDB_CONNECT_TIMEOUT`, `TMDB_READ_TIMEOUT`으로 관리하며 기본값은 `2s`, `5s`다.
- TMDB DTO는 `integration.tmdb.dto` 안에 한정하고 Application에는 `MovieSummary`, `MovieDetail` 조회 모델만 반환한다.
- 검색·상세·추천 호출은 Movie Entity나 Repository를 사용하지 않으며 DB에 저장하지 않는다.

### V1 Endpoint

- 검색: `GET /3/search/movie`
- 상세: `GET /3/movie/{movie_id}`
- 추천: `GET /3/movie/{movie_id}/recommendations`

### 오류 경계

- 영화 없음: `MovieNotFoundException`
- TMDB 인증 실패: `MovieClientAuthenticationException`
- TMDB 5xx: `MovieClientUnavailableException`
- 연결·읽기 Timeout 등 통신 실패: `MovieClientCommunicationException`
- 그 밖의 TMDB 응답 처리 실패: `MovieClientException`

Retry, Circuit Breaker, Fallback은 적용하지 않는다.

## 현재 범위 제외

- 확정된 요구사항과 측정 없이 Cache, Retry, 대체 응답, 호출 제한 대응을 미리 구현하는 것

## 미결정 사항

- TODO: TMDB 이용 약관, 호출 한도, 제공 데이터의 세부 범위를 확인한다.
- TODO: 저장된 최소 영화 정보를 TMDB 변경에 맞춰 갱신할지 여부와 갱신 시점을 결정한다.
- TODO: Retry, 대체 응답, 기능 축소, 호출 제한 대응 전략을 정의한다.
- TODO: 공급자 또는 자격 증명 세부 정보를 노출하지 않는 API 오류 응답 방식을 정의한다.
- TODO: 실제 TMDB 계약 Smoke Test의 실행 환경과 주기를 결정한다.
