<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AvatarBadge from '../AvatarBadge.vue'
import { useAuth, AUTH_STATE } from '../../composables/useAuth'

defineProps({
  showBoardLink: { type: Boolean, default: true },
})

const { state, openLoginModal, logout } = useAuth()
const isDropdownOpen = ref(false)

function handleLogout() {
  isDropdownOpen.value = false
  logout()
}
</script>

<template>
  <div class="nav" style="height: 56px; padding: 0 20px">
    <RouterLink to="/" class="nav-brand" style="display: flex; align-items: center; gap: 9px; font-size: 21px; margin-right: 0">
      <i class="ph-fill ph-film-reel" style="font-size: 25px; color: var(--color-accent)"></i>CINEWITH
    </RouterLink>
    <RouterLink v-if="showBoardLink" to="/reviews" style="font-size: 13px; color: color-mix(in srgb, var(--color-text) 60%, transparent)">
      리뷰 게시판
    </RouterLink>
    <div style="flex: 1"></div>
    <slot name="actions" />
    <button v-if="state.status === AUTH_STATE.ANONYMOUS" class="btn btn-primary" style="font-size: 13px" @click="openLoginModal">
      로그인 / 가입
    </button>
    <div v-else style="position: relative">
      <button class="btn btn-icon btn-secondary" style="border-color: transparent" aria-label="마이페이지 메뉴" @click="isDropdownOpen = !isDropdownOpen">
        <AvatarBadge :initial="state.member?.initial ?? '?'" size="30px" font-size="11px" />
      </button>
      <div v-if="isDropdownOpen" style="position: absolute; right: 0; top: 40px; width: 180px; padding: 8px; border-radius: var(--radius-md); background: var(--color-bg); box-shadow: var(--shadow-lg); display: flex; flex-direction: column; gap: 2px; z-index: 20">
        <RouterLink to="/mypage" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false"><i class="ph ph-user"></i>마이페이지</RouterLink>
        <RouterLink to="/mypage#reviews" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false"><i class="ph ph-note-pencil"></i>내가 쓴 리뷰</RouterLink>
        <RouterLink to="/mypage#comments" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false"><i class="ph ph-chat-circle"></i>내가 쓴 댓글</RouterLink>
        <RouterLink to="/mypage#recommended-reviews" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false"><i class="ph ph-thumbs-up"></i>추천한 리뷰</RouterLink>
        <RouterLink to="/mypage#recommended-comments" class="side-item" style="height: 32px; font-size: 13px" @click="isDropdownOpen = false"><i class="ph ph-thumbs-up"></i>추천한 댓글</RouterLink>
        <div class="fade-rule" style="margin: 4px -8px"></div>
        <button class="side-item" style="height: 32px; font-size: 13px; border: 0; background: transparent; text-align: left" @click="handleLogout"><i class="ph ph-sign-out"></i>로그아웃</button>
      </div>
    </div>
  </div>
  <div class="fade-rule"></div>
</template>
