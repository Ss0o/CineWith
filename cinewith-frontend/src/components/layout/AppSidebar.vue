<script setup>
import { RouterLink, useRoute } from 'vue-router'
import { useAuth, AUTH_STATE } from '../../composables/useAuth'

defineProps({
  showFooterCard: { type: Boolean, default: true },
})

const route = useRoute()
const { state } = useAuth()

function isActive(path) {
  return route.path === path && (path !== '/' || (!route.hash && !route.query.q))
}

function isDiscoveryActive(section) {
  return route.path === `/discover/${section}`
}
</script>

<template>
  <nav style="display: flex; flex-direction: column; gap: 24px; height: 100%">
    <div style="display: flex; flex-direction: column; gap: 2px">
      <div class="h6" style="padding: 0 10px 8px">게시판</div>
      <RouterLink to="/" class="side-item" :class="{ 'side-on': isActive('/') }">
        <i class="ph ph-magnifying-glass" style="font-size: 16px"></i>영화·리뷰 탐색
      </RouterLink>
      <RouterLink to="/discover/recommended" class="side-item" :class="{ 'side-on': isDiscoveryActive('recommended') }">
        <i class="ph ph-star" style="font-size: 16px"></i>추천 영화
      </RouterLink>
      <RouterLink to="/discover/now-playing" class="side-item" :class="{ 'side-on': isDiscoveryActive('now-playing') }">
        <i class="ph ph-film-strip" style="font-size: 16px"></i>현재 상영작
      </RouterLink>
      <RouterLink to="/discover/upcoming" class="side-item" :class="{ 'side-on': isDiscoveryActive('upcoming') }">
        <i class="ph ph-calendar-blank" style="font-size: 16px"></i>개봉 예정작
      </RouterLink>
      <RouterLink to="/reviews" class="side-item" :class="{ 'side-on': isActive('/reviews') }">
        <i class="ph ph-list-bullets" style="font-size: 16px"></i>전체 리뷰
      </RouterLink>
      <RouterLink v-if="state.status === AUTH_STATE.MEMBER" to="/reviews/new" class="side-item" :class="{ 'side-on': isActive('/reviews/new') }">
        <i class="ph ph-note-pencil" style="font-size: 16px"></i>리뷰 작성
      </RouterLink>
    </div>

    <div class="fade-rule" style="margin: 0 -12px"></div>

    <div style="display: flex; flex-direction: column; gap: 2px">
      <div class="h6" style="padding: 0 10px 8px">장르별 영화</div>
      <RouterLink v-for="genre in [
        ['action', '액션'], ['adventure', '모험'], ['animation', '애니메이션'], ['comedy', '코미디'], ['drama', '드라마'],
        ['fantasy', '판타지'], ['horror', '공포'], ['romance', '로맨스'], ['sf', 'SF'], ['thriller', '스릴러'],
      ]" :key="genre[0]" :to="`/discover/${genre[0]}`" class="side-item" :class="{ 'side-on': isDiscoveryActive(genre[0]) }">
        <i class="ph ph-ticket" style="font-size: 16px"></i>{{ genre[1] }}
      </RouterLink>
    </div>

    <div class="fade-rule" style="margin: 0 -12px"></div>

    <div style="display: flex; flex-direction: column; gap: 2px">
      <div class="h6" style="padding: 0 10px 8px">내 활동</div>
      <RouterLink to="/mypage" class="side-item"><i class="ph ph-user" style="font-size: 16px"></i>내 회원 정보</RouterLink>
    </div>

    <div v-if="showFooterCard" style="margin-top: auto; padding: 14px; border-radius: var(--radius-md); background: var(--color-surface); font: 400 11.5px/1.6 var(--font-body); color: color-mix(in srgb, var(--color-text) 55%, transparent)">영화를 검색해 리뷰를 읽고, 로그인 후 이야기를 남겨보세요.</div>
  </nav>
</template>
