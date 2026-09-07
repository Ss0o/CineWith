<script setup>
import { computed } from 'vue'
import TopNav from '../components/layout/TopNav.vue'
import AvatarBadge from '../components/AvatarBadge.vue'
import { useAuth, AUTH_STATE } from '../composables/useAuth'

const { state, openLoginModal } = useAuth()
const isMember = computed(() => state.status === AUTH_STATE.MEMBER)
</script>

<template>
  <TopNav />
  <div v-if="!isMember" style="padding: 80px 32px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 14px; color: color-mix(in srgb, var(--color-text) 55%, transparent)">
    <div>로그인 후 회원 정보를 확인할 수 있습니다.</div>
    <button class="btn btn-primary" @click="openLoginModal">Google로 로그인</button>
  </div>
  <div v-else style="padding: 30px 32px; max-width: 760px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px">
    <h3 style="margin: 0">내 회원 정보</h3>
    <div class="card elev-sm" style="padding: 22px; flex-direction: row; align-items: center; gap: 16px">
      <AvatarBadge :initial="state.member.initial" size="58px" font-size="22px" />
      <div style="display: flex; flex-direction: column; gap: 7px">
        <div style="font: 500 20px/1.2 var(--font-heading)">{{ state.member.nickname }}</div>
        <div class="meta">{{ state.member.email || '이메일 정보 없음' }}</div>
        <div class="meta">{{ state.member.provider }} 로그인 · 가입일 {{ state.member.joinedAt }}</div>
      </div>
    </div>
    <div class="card" style="padding: 18px; color: color-mix(in srgb, var(--color-text) 58%, transparent); font-size: 13px; line-height: 1.7">
      현재 Backend는 회원별 Review/Comment 목록 Endpoint를 제공하지 않습니다. 내부 Member ID를 클라이언트에 노출하거나 임의로 전달하지 않으며, 지원 API가 추가되면 이 영역에 내 활동을 연결합니다.
    </div>
  </div>
</template>
