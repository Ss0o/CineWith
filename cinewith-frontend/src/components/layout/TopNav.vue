<script setup>
import { RouterLink } from 'vue-router'
import AvatarBadge from '../AvatarBadge.vue'
import { useAuth, AUTH_STATE } from '../../composables/useAuth'

defineProps({
  showBoardLink: { type: Boolean, default: true },
})

const { state, openLoginModal } = useAuth()
</script>

<template>
  <div class="nav" style="height: 56px; padding: 0 20px">
    <RouterLink to="/" class="nav-brand" style="display: flex; align-items: center; gap: 8px; font-size: 16px; margin-right: 0">
      <i class="ph-fill ph-film-reel" style="font-size: 20px; color: var(--color-accent)"></i>씨네위드
    </RouterLink>
    <RouterLink v-if="showBoardLink" to="/" style="font-size: 13px; color: color-mix(in srgb, var(--color-text) 60%, transparent)">
      리뷰 게시판
    </RouterLink>
    <div style="flex: 1"></div>
    <slot name="actions" />
    <button v-if="state.status === AUTH_STATE.ANONYMOUS" class="btn btn-primary" style="font-size: 13px" @click="openLoginModal">
      로그인 / 가입
    </button>
    <RouterLink v-else to="/mypage">
      <AvatarBadge :initial="state.member?.initial ?? '?'" size="30px" font-size="11px" />
    </RouterLink>
  </div>
  <div class="fade-rule"></div>
</template>
