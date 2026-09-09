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

- 영화 상세는 별도 통계 API의 Cinewith 평균·전체 리뷰 수·10개 평점 분포를 표시한다. 리뷰가 없으면 평균을 표시하지 않는다. 미제공 추가 상세 필드는 표시하지 않는다. 리뷰·댓글 목록 전체 건수는 페이지 응답의 `totalElements`를 사용한다.
- 리뷰·댓글 화면은 20개씩 이전/다음 페이지 이동을 지원한다. 댓글 등록은 마지막 페이지로 이동하며 삭제 후 빈 마지막 페이지는 보정한다.
- 수정·삭제 버튼은 고유 닉네임이 일치하는 작성자에게 표시한다. 최종 권한 검사는 Backend가 회원 ID로 수행한다. 리뷰 수정 UI는 제목·본문·평점을 지원한다.
- `npm test`로 화면 상태/API 계층 자동 테스트, `npm run build`로 번들 검증을 실행한다. 실제 Google 로그인부터 작성까지의 전체 흐름 검증은 별도 과제다.

- Backend에 전역 Review 목록과 회원별 활동 목록 Endpoint가 없으므로 홈은 영화
  검색 중심으로 구성하고 마이페이지는 현재 회원 정보만 표시한다.
- 최근/인기 영화, 극장, 추천 게시판은 현재 Backend 계약에 없어 탐색 메뉴에서 제거했다.
- Only the Google OAuth2 button is functional in the login modal — Kakao/
  Naver/GitHub are shown for design fidelity but disabled, since V1 only
  defines Google (`docs/SECURITY.md`).
