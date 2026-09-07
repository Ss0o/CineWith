# 씨네위드 (CineWith) frontend

Vue 3 + Vite implementation of the "영화 리뷰 추천 게시판 UI" Claude Design
project (source file `Cineboard Mockups.dc.html` — that was the working
title in Claude Design; the shipped product name is CineWith / 씨네위드),
Nocturne design system. Spring Boot backend 저장소 내부의 독립 Vite 앱이며
Gradle build에는 포함되지 않는다.

## Status

현재 CineWith Backend API만 사용하는 Vue UI다. 컴포넌트는 `src/data`의
공통 API 계층을 사용하며 Session, Google OAuth2, CSRF 계약을 따른다.

## Run

```
npm install
npm run dev
```

Vite는 `/api`, `/oauth2`, `/login`을 `http://localhost:8080`으로 proxy한다.
따라서 개발 중에는 Backend도 함께 실행해야 한다.

## Structure

- `src/styles/tokens.css` — Nocturne design-system tokens, ported verbatim
  from the Claude Design project's `styles.css`.
- `src/styles/app.css` — the mockup doc's own component classes
  (`.side-item`, `.poster`, `.stars`, `.av`, `.chip`, `.h6`, `.fade-rule`)
  that are real UI, as opposed to the `.dv-*` classes which were only the
  Claude Design canvas/gallery chrome and were intentionally not ported.
- `src/components/layout/` — `AppHeader` (full header w/ search + auth
  state, implements mockup panel `1k`'s states 1–2), `TopNav` (simpler
  header used by detail-style pages), `AppSidebar`, `BoardShell`
  (sidebar + content + optional right rail), `MobileDrawer` +
  `BottomTabBar` (mobile layout, mockup panel `1m`).
- `src/views/` — one file per mockup screen:

  | View | Mockup panel |
  | --- | --- |
  | `ReviewBoardView` | 영화 검색 및 리뷰 탐색 |
  | `ReviewDetailView` | `1c` |
  | `ReviewWriteView` | `1d` |
  | `MovieDetailView` | `1e` |
  | `MyPageView` | `1l` |

  Panel `1b` (icon-rail layout) and `1h` (no-map theater alternative)
  were not implemented — `1a`/`1g` were chosen instead. Panel `1j`
  (login modal) and `1k` (auth header states + signup step) are implemented
  as `LoginModal.vue` / `SignupNicknameModal.vue`, driven by
  `src/composables/useAuth.js`.

## Known gaps / next steps

- 영화 평균 평점·리뷰 수와 추가 상세 필드는 아직 API에 연결되지 않아 기본값으로 표시된다.
- 리뷰·댓글 화면은 첫 페이지를 조회하며, 페이지 이동 UI는 아직 제공하지 않는다.
- 수정·삭제 버튼은 로그인 회원에게 표시되고 최종 작성자 권한 검사는 Backend가 수행한다. 리뷰 수정 UI는 제목·본문만 지원한다.
- 프론트엔드 자동 테스트 및 실제 Google 로그인부터 작성까지의 브라우저 검증은 별도 과제다. 현재 빌드 검증 명령은 `npm run build`다.

- Backend에 전역 Review 목록과 회원별 활동 목록 Endpoint가 없으므로 홈은 영화
  검색 중심으로 구성하고 마이페이지는 현재 회원 정보만 표시한다.
- 최근/인기 영화, 극장, 추천 게시판은 현재 Backend 계약에 없어 탐색 메뉴에서 제거했다.
- Only the Google OAuth2 button is functional in the login modal — Kakao/
  Naver/GitHub are shown for design fidelity but disabled, since V1 only
  defines Google (`docs/SECURITY.md`).
