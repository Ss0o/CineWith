<script setup>
import AvatarBadge from '../AvatarBadge.vue'
import AppSidebar from './AppSidebar.vue'
import { useAuth, AUTH_STATE } from '../../composables/useAuth'

defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const { state, logout } = useAuth()
</script>

<template>
  <div v-if="open" class="mobile-only" style="position: fixed; inset: 0; z-index: 40">
    <div style="position: absolute; inset: 0; background: color-mix(in srgb, var(--color-neutral-900) 55%, transparent)" @click="emit('close')"></div>
    <div style="position: absolute; left: 0; top: 0; bottom: 0; width: 296px; background: var(--color-bg); box-shadow: var(--shadow-lg); padding: 16px 12px; display: flex; flex-direction: column; gap: 18px; overflow-y: auto">
      <div v-if="state.status === AUTH_STATE.MEMBER" style="display: flex; align-items: center; gap: 10px; padding: 4px 10px 0">
        <AvatarBadge :initial="state.member?.initial ?? '?'" size="38px" font-size="14px" />
        <div style="flex: 1">
          <div style="font: 400 13.5px/1.3 var(--font-body)">{{ state.member?.nickname }}</div>
          <div style="font: 400 10.5px/1.4 var(--font-body); color: color-mix(in srgb, var(--color-text) 42%, transparent)">
            {{ state.member?.provider }} 연동 · 리뷰 {{ state.member?.reviewCount }}
          </div>
        </div>
      </div>
      <div v-if="state.status === AUTH_STATE.MEMBER" class="fade-rule" style="margin: 0 -12px"></div>

      <AppSidebar :show-footer-card="false" />

      <button v-if="state.status === AUTH_STATE.MEMBER" class="btn btn-secondary btn-block" style="margin-top: auto" @click="logout">
        <i class="ph ph-sign-out" style="font-size: 15px"></i>로그아웃
      </button>
    </div>
  </div>
</template>
