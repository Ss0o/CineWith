<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AvatarBadge from '../AvatarBadge.vue'
import { useAuth, AUTH_STATE } from '../../composables/useAuth'

defineProps({
  showSearch: { type: Boolean, default: true },
})

const emit = defineEmits(['toggle-drawer'])

const { state, openLoginModal, logout } = useAuth()
const isDropdownOpen = ref(false)

function toggleDropdown() {
  isDropdownOpen.value = !isDropdownOpen.value
}

function handleLogout() {
  isDropdownOpen.value = false
  logout()
}
</script>

<template>
  <div class="nav" style="height: 60px; padding: 0 20px; gap: 18px">
    <button
      class="btn btn-icon btn-secondary mobile-only"
      style="border-color: transparent"
      aria-label="메뉴 열기"
      @click="emit('toggle-drawer')"
    >
      <i class="ph ph-list" style="font-size: 20px"></i>
    </button>

    <RouterLink to="/" class="nav-brand" style="display: flex; align-items: center; gap: 8px; margin-right: 0">
      <i class="ph-fill ph-film-reel" style="font-size: 22px; color: var(--color-accent)"></i>씨네위드
    </RouterLink>

    <div
      v-if="showSearch"
      class="desktop-only"
      style="display: flex; align-items: center; gap: 8px; flex: 1; max-width: 420px; height: 34px; padding: 0 12px; background: var(--color-surface); border: 1px solid var(--color-divider); border-radius: var(--radius-md)"
    >
      <i class="ph ph-magnifying-glass" style="font-size: 15px; color: color-mix(in srgb, var(--color-text) 45%, transparent)"></i>
      <span style="font: 400 13px/1 var(--font-body); color: color-mix(in srgb, var(--color-text) 40%, transparent)">영화 · 리뷰 · 유저 검색</span>
    </div>

    <div style="flex: 1"></div>

    <RouterLink v-if="state.status === AUTH_STATE.MEMBER" class="btn btn-primary" to="/reviews/new">
      <i class="ph ph-pencil-simple" style="font-size: 15px"></i>리뷰 쓰기
    </RouterLink>

    <button class="btn btn-icon btn-secondary" style="position: relative; border-color: transparent">
      <i class="ph ph-bell" style="font-size: 18px"></i>
      <span
        v-if="state.status === AUTH_STATE.MEMBER"
        style="position: absolute; top: 6px; right: 7px; width: 6px; height: 6px; border-radius: 50%; background: var(--color-accent)"
      ></span>
    </button>

    <!-- 1k · 1. 비로그인 상태 -->
    <button v-if="state.status === AUTH_STATE.ANONYMOUS" class="btn btn-primary" @click="openLoginModal">
      <i class="ph ph-user-circle" style="font-size: 16px"></i>로그인 / 가입
    </button>

    <!-- 1k · 2. 로그인 상태 — 아바타 드롭다운 -->
    <div v-else style="position: relative">
      <div
        style="display: flex; align-items: center; gap: 8px; height: 36px; padding: 2px 10px 2px 2px; border-radius: 18px; border: 1px solid var(--color-divider); cursor: pointer"
        :style="isDropdownOpen ? 'border-color: var(--color-accent)' : ''"
        @click="toggleDropdown"
      >
        <AvatarBadge :initial="state.member?.initial ?? '?'" size="30px" font-size="11px" />
        <span style="font: 400 12.5px/1 var(--font-body)">{{ state.member?.nickname ?? '가입 필요' }}</span>
        <i :class="isDropdownOpen ? 'ph ph-caret-up' : 'ph ph-caret-down'" style="font-size: 12px; color: color-mix(in srgb, var(--color-text) 50%, transparent)"></i>
      </div>

      <div
        v-if="isDropdownOpen"
        style="position: absolute; right: 0; top: 42px; width: 228px; padding: 8px; border-radius: var(--radius-md); background: var(--color-bg); box-shadow: var(--shadow-lg); display: flex; flex-direction: column; gap: 2px; z-index: 20"
      >
        <div style="display: flex; align-items: center; gap: 10px; padding: 8px 10px 10px">
          <AvatarBadge :initial="state.member?.initial ?? '?'" size="34px" font-size="13px" />
          <div>
            <div style="font: 400 13px/1.3 var(--font-body)">{{ state.member?.nickname ?? '가입 필요' }}</div>
            <div style="font: 400 10.5px/1.4 var(--font-body); color: color-mix(in srgb, var(--color-text) 42%, transparent)">
              {{ state.member?.provider ?? 'Google' }} 연동
            </div>
          </div>
        </div>
        <div class="fade-rule" style="margin: 0 -8px 4px"></div>
        <RouterLink to="/mypage" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false">
          <i class="ph ph-user" style="font-size: 15px"></i>마이페이지
        </RouterLink>
        <RouterLink to="/mypage" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false">
          <i class="ph ph-note-pencil" style="font-size: 15px"></i>내가 쓴 리뷰
        </RouterLink>
        <div class="side-item" style="height: 32px; font-size: 13px">
          <i class="ph ph-bookmark-simple" style="font-size: 15px"></i>스크랩
        </div>
        <div class="side-item" style="height: 32px; font-size: 13px">
          <i class="ph ph-gear" style="font-size: 15px"></i>계정 · 연동 관리
        </div>
        <div class="fade-rule" style="margin: 4px -8px"></div>
        <div
          class="side-item"
          style="height: 32px; font-size: 13px; color: color-mix(in srgb, var(--color-text) 55%, transparent)"
          @click="handleLogout"
        >
          <i class="ph ph-sign-out" style="font-size: 15px"></i>로그아웃
        </div>
      </div>
    </div>
  </div>
  <div class="fade-rule"></div>
</template>
