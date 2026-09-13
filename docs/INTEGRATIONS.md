# 외부 서비스 연동

## 확정 범위

- V1의 외부 영화 데이터 공급자는 TMDB다.
- 영화 검색, 기본 상세 정보와 추천 영화 정보는 TMDB API를 원천으로 사용한다.
- 서비스 DB에는 실제 리뷰가 작성된 영화의 최소 정보만 저장한다. 검색, 상세, 추천 조회만으로는 영화를 저장하지 않는다.
- 성능 테스트에서는 TMDB 성능과 우리 애플리케이션 성능을 구분해서 측정한다.

## TMDB Client 구현

- Application 경계는 `MovieClient`이며 현재 상영작, 영화 검색, 상세, 추천 조회만 제공한다. 상세 조회는 `append_to_response=credits`로 기본 상세와 출연진·제작진을 한 요청에 결합한다.
- 실제 구현은 Spring MVC와 같은 동기식 모델의 `RestClient`를 사용하는 `TmdbMovieClient`다. WebFlux 의존성은 추가하지 않는다.
- TMDB API Read Access Token을 `Authorization: Bearer {token}` Header로 전송한다. API Key Query Parameter 방식은 사용하지 않는다.
- Token은 `TMDB_ACCESS_TOKEN` 환경 변수에서만 주입한다.
- 기본 Base URL은 `https://api.themoviedb.org`이며 `TMDB_BASE_URL`로 변경할 수 있다.
- `TMDB_LANGUAGE`와 `TMDB_REGION`은 값이 있을 때만 Query Parameter로 보낸다. V1의 언어·지역 기본 정책은 확정하지 않는다.
- 연결 Timeout과 응답 Timeout은 각각 `TMDB_CONNECT_TIMEOUT`, `TMDB_READ_TIMEOUT`으로 관리하며 기본값은 `2s`, `5s`다.
- TMDB DTO는 `integration.tmdb.dto` 안에 한정하고 Application에는 `MovieSummary`, `MovieDetail` 조회 모델만 반환한다.
- 현재 상영작·검색·상세·추천 호출은 Movie Entity나 Repository를 사용하지 않으며 DB에 저장하지 않는다.

### V1 Endpoint

- 현재 상영작: `GET /3/movie/now_playing`
- 검색: `GET /3/search/movie`
- 상세: `GET /3/movie/{movie_id}?append_to_response=credits`
- 추천: `GET /3/movie/{movie_id}/recommendations`

### 오류 경계

- 영화 없음: `MovieNotFoundException`
- TMDB 인증 실패: `MovieClientAuthenticationException`
- TMDB 5xx: `MovieClientUnavailableException`
- 연결·읽기 Timeout 등 통신 실패: `MovieClientCommunicationException`
- 그 밖의 TMDB 응답 처리 실패: `MovieClientException`

Retry, Circuit Breaker, Fallback은 적용하지 않는다.

API 경계에서 영화 없음은 `404 Not Found`, TMDB 인증 실패와 그 밖의 Upstream 응답 실패는 `502 Bad Gateway`, TMDB 서비스 장애와 통신 실패는 `503 Service Unavailable`로 변환한다. 공급자 예외 메시지나 자격 증명 정보는 API 응답에 노출하지 않는다.

## KOFIC 국내 극장 정보

- KOFIC Open API는 `KOFIC_API_KEY` 환경 변수로만 인증한다. 개발 환경에서는 Git에서 제외된 프로젝트 루트 `.env`를 Spring Boot가 자동으로 읽으며, `.env.example`은 키 없는 템플릿이다. Docker Compose도 같은 `.env` 값을 backend 컨테이너에 전달한다. 기본 Base URL은 `https://www.kobis.or.kr/kobisopenapi/webservice/rest`이며 `KOFIC_BASE_URL`로 변경할 수 있다. 연결·읽기 timeout은 각각 `KOFIC_CONNECT_TIMEOUT`, `KOFIC_READ_TIMEOUT`으로 관리하며 기본값은 `2s`, `5s`다.
- `searchMovieList`, `searchMovieInfo`, `searchDailyBoxOfficeList`를 사용한다. 기본 영화 정보는 TMDB가 계속 원천이고, KOFIC은 국내 극장 보조 정보만 제공한다.
- KOFIC 영화코드는 제목 일치 후보가 하나일 때만 사용한다. 여러 후보는 TMDB 개봉일과 KOFIC 개봉일이 일치할 때 우선 선택한다. 전 세계 최초 개봉일과 한국 개봉일이 다른 경우에는 같은 연도의 후보가 정확히 하나일 때만 선택하며, 그 외에는 `NOT_AVAILABLE`다.
- 전일 전국 박스오피스 목록에 있는 영화에만 순위·매출 점유율·누적 관객 수를 제공한다. 목록 밖 영화도 KOFIC 영화상세가 매칭되면 기본 국내 극장 정보는 `AVAILABLE`로 반환하되 해당 통계 필드는 null이다.
- KOFIC 키 누락, 응답 오류, 통신 오류는 보조 API의 `UNAVAILABLE` 상태로 표현한다. API 키나 공급자 오류 본문은 클라이언트에 노출하지 않는다.

## 영화 추천·탐색 홈 (3.5단계)

- 홈의 모든 외부 영화 데이터와 정렬 신호는 TMDB만 사용한다. Naver DataLab이나 NAVER API HUB는 연동하지 않는다.
- 현재 상영과 개봉 예정은 TMDB 후보를 인기도 우선으로 정렬하고, 인기도가 같으면 각각 최신·가장 가까운 개봉일을 우선한다.
- 일반 추천은 TMDB 평점·평가 수·인기도 heuristic을 사용하고 개인화 추천은 제공하지 않는다.
- Caffeine 캐시는 discovery home 결과만 3시간 유지한다. dependency 목적은 TMDB 외부 호출 제한이며, 테스트는 cache hit에서 client 재호출이 없음을 추가 대상으로 둔다.

## 현재 범위 제외

- 확정된 요구사항과 측정 없이 Cache, Retry, 대체 응답, 호출 제한 대응을 미리 구현하는 것

## 미결정 사항

- TODO: TMDB 이용 약관, 호출 한도, 제공 데이터의 세부 범위를 확인한다.
- TODO: 저장된 최소 영화 정보를 TMDB 변경에 맞춰 갱신할지 여부와 갱신 시점을 결정한다.
- TODO: Retry, 대체 응답, 기능 축소, 호출 제한 대응 전략을 정의한다.
- TODO: 실제 TMDB 계약 Smoke Test의 실행 환경과 주기를 결정한다.
