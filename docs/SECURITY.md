# 보안

## 현재 기준

Spring Security 7.0.6과 Spring Security OAuth2 Client를 사용한다. Google OIDC 공급자 등록, Security Filter Chain, 서비스 Principal과 로그인 성공 Redirect 기반이 구현되어 있다. Client ID와 Client Secret은 환경 변수로만 주입하며 추적 중인 설정에는 실제 자격 증명이 없다.

## 인증 방식

V1은 Google 로그인을 OpenID Connect(OIDC) 기반으로 사용한다.

- Google 사용자의 고유 식별자는 OIDC의 `sub` Claim을 사용한다.
- 서비스의 OAuth Identity는 `provider + providerId` 조합으로 표현한다.
- Google 사용자는 `provider = GOOGLE`, `providerId = Google sub`로 표현한다.
- `email`은 변경될 수 있으므로 사용자 식별자로 사용하지 않는다.
- Google 인증 성공과 커뮤니티 `Member` 가입 완료는 별개의 상태다.

## Session 기반 인증

- 서비스 로그인 상태는 Spring Security와 HTTP Session을 기반으로 관리한다.
- V1에서는 JWT 기반 인증을 사용하지 않는다.
- 로그아웃하면 현재 서비스 Session을 무효화한다.
- Google Access Token과 Refresh Token을 서비스 DB에 직접 저장하지 않는다.
- Google Access Token과 Refresh Token을 서비스 자체 인증 토큰으로 사용하지 않는다.
- `POST /api/logout`은 현재 서비스 Session을 무효화하고 `JSESSIONID` Cookie를 제거한다. Google 계정 로그아웃이나 Google Token revoke는 수행하지 않는다.

## 인증 상태 모델

서비스 사용자를 다음 세 상태로 구분한다.

| 상태 | Google 인증 | DB의 Member | 허용 범위 |
| --- | --- | --- | --- |
| `ANONYMOUS` | 완료하지 않음 | 없음 | Public 기능 |
| `SIGNUP_REQUIRED` | 완료 | 없음 | Public 기능과 가입용 닉네임 설정 |
| `MEMBER` | 완료 | 존재 | Public 기능과 로그인 회원 기능 |

### ANONYMOUS

- Google 인증을 완료하지 않은 사용자다.
- 인증이나 회원가입 없이 허용되는 Public 기능만 사용할 수 있다.

### SIGNUP_REQUIRED

- Google 인증에 성공해 `provider`, `providerId`, `email` 등 가입에 필요한 Identity를 확보한 상태다.
- `provider + providerId`에 대응하는 `Member`는 아직 존재하지 않는다.
- 커뮤니티 닉네임을 설정할 수 있다.
- 리뷰와 댓글 작성 등 `Member` 전용 기능은 사용할 수 없다.

### MEMBER

- Google 인증과 닉네임 설정을 완료했고 DB에 `Member`가 존재하는 상태다.
- 로그인 회원 전용 기능을 사용할 수 있다.

인증된 사용자는 `CommunityOidcPrincipal`로 표현한다. 기존 회원은 `ROLE_MEMBER`와 내부 `memberId`를 가지며, 가입 미완료 사용자는 `ROLE_SIGNUP_REQUIRED`와 가입용 OAuth Identity를 가진다. `ANONYMOUS`는 별도 서비스 Principal을 만들지 않고 Spring Security의 익명 상태로 유지한다. V1의 두 인증 상태만 구분하기 위한 최소 권한이며 별도의 복잡한 역할 계층은 만들지 않는다.

## 최초 가입 흐름

최초 사용자의 가입 흐름은 다음과 같다.

1. Google OIDC 인증에 성공한다.
2. `provider + providerId`로 `Member`를 조회한다.
3. `Member`가 존재하면 `MEMBER` 상태가 된다.
4. `Member`가 없으면 `SIGNUP_REQUIRED` 상태가 된다.
5. 가입에 필요한 OAuth Identity를 HTTP Session에 임시 보관한다.
6. 사용자가 커뮤니티 닉네임을 입력한다.
7. 닉네임 형식을 검증하고 중복 여부를 확인한다.
8. 검증에 성공하면 `Member`를 생성한다.
9. 사용자를 `MEMBER` 상태로 전환한다.

OAuth2 인증만 완료한 가입 미완료 사용자를 `Member(status=PENDING)` 형태로 DB에 저장하지 않는다.

## 가입 Session 정책

- `SIGNUP_REQUIRED`에 필요한 임시 OAuth Identity는 HTTP Session에 유지한다.
- V1에서는 가입 진행 정보를 위한 별도의 Redis나 DB 저장소를 사용하지 않는다.
- Session이 만료되기 전에 가입을 완료하지 못하면 가입 진행 정보를 폐기한다.
- 가입 진행 정보가 폐기된 사용자는 Google 로그인을 다시 수행해야 한다.
- V1에서는 가입 정보만을 위한 별도의 TTL 시스템을 만들지 않는다.
- HTTP Session의 구체적인 만료 시간과 Cookie 옵션은 아직 결정하지 않는다.
- 가입용 최소 Identity인 `provider`, `providerId`, `email`은 `CommunityOidcPrincipal`에 포함되며 Spring SecurityContext와 함께 HTTP Session에 유지된다.
- 가입 완료 시 `SIGNUP_REQUIRED` Principal을 `MEMBER` Principal로 교체하고 같은 `SecurityContextRepository`를 통해 현재 HTTP Session에 저장한다. 별도 가입 Session 속성은 만들지 않으므로 이전 가입 진행 상태는 Principal 교체와 함께 제거된다.

## 권한 정책

### Public 기능

다음 기능은 `ANONYMOUS`, `SIGNUP_REQUIRED`, `MEMBER` 모두 사용할 수 있다.

- 영화 검색
- 영화 상세 조회
- 추천 영화 조회
- 리뷰 목록 조회
- 리뷰 상세 조회
- 댓글 조회

### SIGNUP_REQUIRED 기능

- 커뮤니티 닉네임 설정

`SIGNUP_REQUIRED`는 `MEMBER` 전용 기능을 사용할 수 없다.

### MEMBER 기능

- 자신의 회원 정보 조회
- 리뷰 작성
- 자신의 리뷰 수정
- 자신의 리뷰 삭제
- 댓글 작성
- 자신의 댓글 수정
- 자신의 댓글 삭제

## 리소스 소유권

리뷰와 댓글의 수정·삭제 권한은 Spring Security 인증 컨텍스트에서 얻은 현재 `Member`를 기준으로 검증한다. 클라이언트 요청으로 전달된 `memberId`는 권한 판단의 근거로 신뢰하지 않는다.

- Review 수정·삭제: `currentMember.id == review.member.id`인 경우에만 허용한다.
- Comment 수정·삭제: `currentMember.id == comment.member.id`인 경우에만 허용한다.
- 리뷰 작성자라도 다른 `Member`가 작성한 Comment를 수정하거나 삭제할 수 없다.

이 소유권 검사는 URL이나 요청 Body에 포함된 회원 식별자와 무관하게 적용한다.

## 401 / 403 정책

- 인증되지 않은 `ANONYMOUS` 사용자가 인증이 필요한 기능을 요청하면 `401 Unauthorized`를 반환한다.
- Google 인증은 완료했지만 `Member` 권한이 없는 `SIGNUP_REQUIRED` 사용자가 `MEMBER` 전용 기능을 요청하면 `403 Forbidden`을 반환한다.
- `MEMBER`가 다른 회원 소유 리소스에 대한 수정·삭제를 요청하면 `403 Forbidden`을 반환한다.

| 요청 사례 | 결과 |
| --- | --- |
| `ANONYMOUS → Review 작성` | `401 Unauthorized` |
| `SIGNUP_REQUIRED → Review 작성` | `403 Forbidden` |
| `Member A → Member B의 Review 삭제` | `403 Forbidden` |
| `Member A → Member B의 Comment 삭제` | `403 Forbidden` |

구체적인 오류 응답 Body와 이를 처리할 Handler 클래스는 API 및 구현 설계 단계에서 결정한다.

## CSRF 정책

- V1은 Session과 Cookie 기반 인증을 사용하므로 Spring Security의 CSRF 보호를 유지한다.
- 단순히 REST API라는 이유만으로 `csrf.disable()`을 사용하지 않는다.
- 프론트엔드 SPA와 CSRF Token을 교환하는 구체적인 방법은 API 및 구현 설계 단계에서 결정한다.
- CSRF Token 전달 Endpoint 유무와 URL은 아직 결정하지 않는다.

## 필수 보안 원칙

- 비밀번호, Client Secret, Token, Private Key를 커밋하지 않는다.
- 민감 정보는 환경 변수 기반 또는 배포 환경에서 관리하는 비밀 저장소에 보관한다.
- 엔드포인트를 도입하면 명시적인 정책으로 접근을 거부하고 허용·거부 사례를 모두 테스트한다.
- 신뢰할 수 없는 입력을 검증하고 내부 예외 세부 정보를 노출하지 않는다.
- OAuth2 인증만 완료한 사용자가 `Member` 전용 기능을 사용하지 못하도록 권한 경계를 적용한다.

## 현재 구현 구조

- `GoogleOidcUserService`는 Spring Security의 `OidcUserService`에 Google 통신과 표준 OIDC 검증을 위임한다.
- 위임 결과의 `sub`를 `providerId`로 사용해 `MemberRepository.findByProviderAndProviderId(GOOGLE, sub)`를 호출한다.
- 조회 결과에 따라 `CommunityOidcPrincipal`을 `MEMBER` 또는 `SIGNUP_REQUIRED`로 생성한다. 신규 사용자의 Member는 생성하지 않는다.
- 로그인 성공 시 `OidcLoginSuccessHandler`가 상태별 Frontend URL로 Redirect한다. 두 URL은 `app.security.oauth2.redirect.*` 설정으로 관리한다.
- Google Client ID와 Client Secret은 각각 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 환경 변수로 주입한다. Scope는 `openid`, `profile`, `email`이다.
- `POST /api/members/signup`은 `ROLE_SIGNUP_REQUIRED`만 접근한다. 성공 시 Session의 신뢰된 Identity와 요청 닉네임으로 Member를 생성한 뒤 인증을 `ROLE_MEMBER`로 전환한다.
- `ANONYMOUS`의 가입 요청은 401, 이미 가입한 `MEMBER`의 재요청은 인증은 되어 있으나 가입 권한이 없으므로 403이다.
- `GET /api/members/me`는 `ROLE_MEMBER` Principal의 내부 `memberId`로 현재 Member를 조회한다. 클라이언트가 조회 ID를 전달하지 않는다.
- 인증·인가 오류와 OIDC 로그인 실패는 고정된 `code`, `message` JSON만 반환하며 내부 예외 메시지와 Stack Trace를 노출하지 않는다.
- OIDC 로그인 실패는 `401`과 `OAUTH_LOGIN_FAILED`를 반환한다.

## 현재 확정하지 않는 구현

다음 클래스와 API 세부사항은 아직 만들거나 확정하지 않는다.

- `AuthenticationEntryPoint`
- `AccessDeniedHandler`
- CSRF Token 전달 Endpoint
- Session Cookie 세부 옵션

## Open Questions

- Session Cookie의 이름, `HttpOnly`, `Secure`, `SameSite`, Domain, Path, 만료 설정
- HTTP Session의 구체적인 만료 시간, 갱신, 동시 Session 정책
- 배포 환경별 Frontend Redirect URL의 실제 값
- SPA에 CSRF Token을 전달하고 갱신하는 방식
- OAuth 실패 응답 이후 Frontend 화면 전환과 재시도 UX
- 닉네임 형식 오류와 중복 발생 시 상태 코드 및 응답 형식
- 가입 도중 Session이 만료되거나 가입을 재시도할 때의 사용자 경험
- Session 직렬화 저장소를 도입할 경우 `CommunityOidcPrincipal`의 호환성 정책
- CORS 허용 Origin, Method, Header, Credential 정책
- 계정 연결과 신원 소유권 정책
- 비밀 정보 교체 및 환경별 설정 전략
- 보안 이벤트 로깅과 개인정보 보호 요구사항

## 검증

TODO: 의존성 검사, 정적 분석, 보안 테스트, 사고 대응 기준을 정의한다.
