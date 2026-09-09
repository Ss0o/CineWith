# 3단계: 전체 리뷰 피드와 검색

## API와 조회 선택

`GET /api/reviews`는 전체 피드와 선택 검색어 `query`를 제공한다. 기존 Review 상세 URL과 같은 리소스 경로를 사용하고, 영화별 목록은 기존 `/api/movies/{tmdbId}/reviews`로 유지한다. 조회는 Public이며 수정/삭제 권한에는 변화가 없다.

Review의 `member`, `movie`는 LAZY다. 전체 목록에서 Entity를 가져온 후 `getMember()`/`getMovie()`로 화면 DTO를 만들면 목록 SELECT 뒤 각 연관 Entity를 추가로 읽는 N+1이 될 수 있다. 따라서 JPQL constructor expression으로 Review·Movie·Member에서 필요한 scalar 컬럼만 `ReviewFeedItem`에 담는다.

검색어는 Service에서 trim한다. 빈 검색어는 where 없는 일반 피드 query를 사용하고, 그 외에는 bound parameter로 다음 조건을 사용한다. 문자열을 SQL에 이어 붙이지 않는다.

```jpql
where lower(r.title) like concat('%', lower(:query), '%')
   or lower(r.content) like concat('%', lower(:query), '%')
   or lower(m.title) like concat('%', lower(:query), '%')
```

## PostgreSQL에서 확인한 SQL

```sql
select r.id, m.tmdb_id, m.title, m.poster_path, member.nickname,
       r.rating, r.title, r.content, r.created_at
from review r
join movie m on m.id = r.movie_id
join member on member.id = r.member_id
where lower(r.title) like ('%' || lower(?) || '%')
   or lower(r.content) like ('%' || lower(?) || '%')
   or lower(m.title) like ('%' || lower(?) || '%')
order by r.created_at desc, r.id desc
offset ? rows fetch first ? rows only;

select count(r.id)
from review r
join movie m on m.id = r.movie_id
where lower(r.title) like ('%' || lower(?) || '%')
   or lower(r.content) like ('%' || lower(?) || '%')
   or lower(m.title) like ('%' || lower(?) || '%');
```

PageRequest의 page/size는 content query의 OFFSET/LIMIT(`fetch first`)를 결정한다. content query는 해당 페이지 카드만, count query는 `totalElements`와 `totalPages`를 계산한다. `createdAt DESC, id DESC`는 같은 시각 리뷰에도 고정 순서를 제공한다.

이는 SQL 수/형태 확인일 뿐 대용량 성능 측정은 아니다. `%keyword%`, 큰 OFFSET, COUNT 비용과 인덱스 후보는 측정 후 검토한다.
