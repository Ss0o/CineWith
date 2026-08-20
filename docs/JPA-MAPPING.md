# JPA 연관관계 매핑

## 목적과 범위

이 문서는 `docs/schema.dbml`에 확정된 V1 DB 관계를 향후 JPA Entity에서 어떤 방향으로 표현할지 정의한다. 실제 Entity 코드, Repository, 조회 최적화, 삭제 구현 방식은 이 문서의 범위에 포함하지 않는다.

## 기본 연관관계 전략

V1에서는 현재 도메인 동작에 필요한 자식에서 부모 방향의 연관관계만 정의한다. 관계가 DB에 존재한다는 이유만으로 반대 방향의 컬렉션을 추가하지 않는다.

모든 관계는 다음 설정을 기본으로 한다.

```java
@ManyToOne(fetch = FetchType.LAZY)
```

`schema.dbml`에서 네 FK가 모두 `NOT NULL`이므로 연관 대상 없이 `Review`나 `Comment`가 존재할 수 없다. 구현된 Review와 Comment의 네 관계는 `optional = false`와 nullable이 아닌 `@JoinColumn`로 이 필수성을 표현한다.

## Review 연관관계

| Entity 방향 | 관계 | Fetch | FK 컬럼 | 참조 대상 |
| --- | --- | --- | --- | --- |
| `Review → Member` | `ManyToOne` | `LAZY` | `review.member_id` | `member.id` |
| `Review → Movie` | `ManyToOne` | `LAZY` | `review.movie_id` | `movie.id` |

### Review → Member

- `Review`는 작성자인 `Member`를 참조한다.
- `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- FK는 `review.member_id`이며 NULL을 허용하지 않는다.

### Review → Movie

- `Review`는 리뷰 대상인 `Movie`를 참조한다.
- `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- FK는 `review.movie_id`이며 NULL을 허용하지 않는다.

## Comment 연관관계

| Entity 방향 | 관계 | Fetch | FK 컬럼 | 참조 대상 |
| --- | --- | --- | --- | --- |
| `Comment → Member` | `ManyToOne` | `LAZY` | `comment.member_id` | `member.id` |
| `Comment → Review` | `ManyToOne` | `LAZY` | `comment.review_id` | `review.id` |

### Comment → Member

- `Comment`는 작성자인 `Member`를 참조한다.
- `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- FK는 `comment.member_id`이며 NULL을 허용하지 않는다.

### Comment → Review

- `Comment`는 소속된 `Review`를 참조한다.
- `@ManyToOne(fetch = FetchType.LAZY)`를 사용한다.
- FK는 `comment.review_id`이며 NULL을 허용하지 않는다.

## V1에서 만들지 않는 연관관계

다음 `OneToMany` 컬렉션은 V1 Entity에 추가하지 않는다.

- `Member → List<Review>`
- `Member → List<Comment>`
- `Movie → List<Review>`
- `Review → List<Comment>`

현재 요구사항은 자식 Entity가 작성자와 대상을 참조하는 방향만으로 표현할 수 있다. 사용 여부가 확인되지 않은 부모 컬렉션을 미리 만들면 연관관계 동기화, 컬렉션 생명주기, Cascade 범위, `equals`·`hashCode`·`toString` 처리 등 관리할 상태가 늘어난다. 향후 실제 도메인 로직에서 부모에서 자식으로 객체 탐색해야 할 필요가 확인되면 그 시점에 추가 여부를 검토한다.

DB의 `1:N` 관계와 Entity의 양방향 연관관계는 같은 의미가 아니다. DB 관계는 FK로 유지하면서 Entity에서는 필요한 `N:1` 방향만 표현할 수 있다.

## Fetch 전략

- 네 `ManyToOne` 관계는 모두 `LAZY`를 사용한다.
- `Fetch Join`, `EntityGraph`, DTO Projection, Batch Size는 현재 추가하지 않는다.
- Entity를 API Response로 직접 반환하지 않으므로 응답 직렬화를 위해 연관관계를 EAGER로 변경하지 않는다.
- V1 구현 후 실제 Query 수와 API 응답 성능을 재현 가능한 조건에서 측정하고 문제가 확인된 경우에만 조회 방식을 개선한다.

## Review 삭제와 Comment 삭제

`Review` 삭제 시 해당 `Review`에 속한 `Comment`도 함께 삭제한다는 도메인 정책은 확정되어 있다. 다만 다음 중 어떤 구현 방법을 사용할지는 아직 확정하지 않는다. 현재 JPA 매핑에는 임의의 Cascade 또는 `orphanRemoval` 설정을 전제하지 않는다.

| 후보 | 장점 | 단점 | 프로젝트 영향 |
| --- | --- | --- | --- |
| DB `ON DELETE CASCADE` | DB가 참조 무결성과 삭제 순서를 일관되게 처리하며, 부모 삭제 시 별도 Comment 로딩이 필요 없다. | JPA 영속성 컨텍스트가 DB에서 삭제된 Comment 상태를 즉시 알지 못할 수 있고 DB 스키마에 삭제 정책이 결합된다. | DB 종류와 Migration 전략을 확정하고 `schema.dbml` 및 실제 스키마에 삭제 동작을 명시해야 한다. |
| `CascadeType.REMOVE` | Entity 삭제 동작을 JPA 생명주기 안에서 표현할 수 있다. | 부모에서 자식으로 Cascade를 전파할 연관관계가 필요하며 Comment 로딩 및 개별 삭제 Query가 발생할 수 있다. | 현재 제외한 `Review → List<Comment>` 관계를 추가해야 할 가능성이 있어 단방향 전략을 재검토해야 한다. |
| `orphanRemoval` | 부모 컬렉션에서 제거된 자식의 생명주기를 JPA가 관리할 수 있다. | 부모 컬렉션을 필수로 관리해야 하며, Review 자체 삭제 정책과 컬렉션 요소 제거 정책이 함께 결합된다. | 현재 제외한 `Review → List<Comment>` 관계와 컬렉션 동기화 규칙이 필요하므로 V1의 단순한 매핑 범위가 커진다. |
| Service 계층에서 명시적 삭제 | 현재의 자식→부모 단방향 매핑을 유지하면서 삭제 순서와 트랜잭션을 명시적으로 제어할 수 있다. | Comment 삭제 누락을 애플리케이션 코드가 방지해야 하며, DB 자체로는 Review 삭제 시 정책을 보장하지 못한다. | Comment 일괄 삭제 동작, 트랜잭션 경계, 실패 시 원자성에 대한 테스트가 필요하다. |

## Entity 설계 원칙

- Entity를 API Response로 직접 반환하지 않는다.
- Entity에 Lombok `@Data`를 사용하지 않는다.
- 연관관계 필드를 `toString`, `equals`, `hashCode`에 포함하지 않는다.
- Setter를 무분별하게 공개하지 않고 도메인 동작을 나타내는 변경 방법을 사용한다.
- JPA 기본 생성자 요구사항을 고려하되 필요한 범위보다 넓게 공개하지 않는다.
- 구체적인 Lombok 사용 여부와 Annotation 조합은 아직 확정하지 않는다.
- 연관관계가 있다는 이유만으로 JPA 양방향 관계를 추가하지 않는다.

## 스키마와의 일관성

이 문서의 네 연관관계는 `schema.dbml`의 다음 Ref와 일치한다.

- `review.member_id → member.id`
- `review.movie_id → movie.id`
- `comment.member_id → member.id`
- `comment.review_id → review.id`

모든 FK가 NULL을 허용하지 않는다는 점도 일치한다. `schema.dbml`은 Review 삭제 시 Comment도 삭제한다는 정책만 기록하고 구체적인 삭제 구현은 지정하지 않으므로, 이 문서에서 Cascade 방식을 미결정으로 유지하는 것과 충돌하지 않는다.

## Open Questions

- Review 삭제 시 Comment를 함께 삭제할 구체적인 구현 방식
- 삭제 구현 방식에 따른 트랜잭션 경계와 원자성 보장 방법
- DB 삭제 동작과 JPA 영속성 컨텍스트 사이의 일관성 처리
- 실제 도메인 로직에서 부모에서 자식으로 탐색해야 하는 요구가 생기는지 여부
- 프록시를 고려한 Entity `equals`와 `hashCode` 정책
- JPA 기본 생성자의 접근 수준
- Lombok 사용 범위
- 실제 측정 결과를 기반으로 한 조회 성능 개선 필요 여부와 적용 방법
