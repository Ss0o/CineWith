# 3.5단계 영화 추천·탐색 홈

`GET /api/movies/discovery/home`은 현재 상영, 일반 추천, 개봉 예정, 5개 장르를 한 번에 반환한다. 최신 Cinewith 리뷰는 기존 `GET /api/reviews`를 프런트가 별도로 호출한다. 이는 외부 추천 실패가 커뮤니티 피드를 가리지 않게 한다.

## 후보와 순위

- 현재 상영: `TMDB_LANGUAGE`·`TMDB_REGION` 설정을 적용한 TMDB 후보 16개를 `popularity DESC`, `releaseDate DESC`, `tmdbId ASC`로 정렬한다.
- 개봉 예정: 미래 개봉일이 있는 TMDB 후보 16개를 `popularity DESC`, `releaseDate ASC`, `tmdbId ASC`로 정렬한다.
- TMDB 섹션 실패는 해당 섹션만 `UNAVAILABLE`이고 캐시하지 않는다.
- 일반 추천: `voteCount >= 100` 후보에 `0.55 * voteAverage/10 + 0.30 * log1p(voteCount)/max + 0.15 * popularity/max` 초기 heuristic을 적용한다. 관람객 수/KOBIS는 범위 밖이다.
- 장르 ID는 `MovieCategory`가 ACTION 28, COMEDY 35, ROMANCE 10749, HORROR 27, SCIENCE_FICTION 878로 캡슐화한다.

## 캐시와 호출

Spring Cache + Caffeine의 `movieDiscoveryHome:home` key를 3시간 TTL, 최대 1개로 사용한다. 첫 요청 cache miss는 TMDB 8회(현재상영, top-rated, upcoming, genre 5)다. cache hit는 TMDB 0회다. 3시간은 영화 탐색 결과가 초 단위로 바뀌지 않고 TMDB 호출을 제한하기 위한 V1 값이다.

로컬 캐시는 재시작 시 사라지고 서버 2대에서는 각각 다른 값을 가질 수 있다. 단일 서버 V1에는 운영 Redis가 불필요하며, 다중 인스턴스/공유 무효화가 필요해지면 Redis를 검토한다.

추후 후보: KOBIS 관객 수, Caffeine→Redis 전환, 추천 가중치 A/B 테스트, 개인화 추천.
