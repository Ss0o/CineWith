<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useAuth } from '../composables/useAuth'

const { state, closeLoginModal, googleLoginUrl } = useAuth()

function onKeydown(event) {
  if (event.key === 'Escape' && state.isLoginModalOpen) closeLoginModal()
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))

// The Google button is wired to the real Spring Security OAuth2 entry
// point (see useAuth.js). Kakao/Naver/GitHub are shown for visual parity
// with the mockup but stay disabled — community-service/docs/SECURITY.md
// only defines a Google provider for V1.
</script>

<template>
  <div v-if="state.isLoginModalOpen" class="dialog-backdrop" @click.self="closeLoginModal">
    <div class="dialog" style="width: 340px; gap: 14px; padding: 22px; position: relative">
      <button
        class="btn btn-icon btn-secondary"
        style="position: absolute; top: 12px; right: 12px; border-color: transparent"
        aria-label="닫기"
        @click="closeLoginModal"
      >
        <i class="ph ph-x" style="font-size: 16px"></i>
      </button>
      <div style="display: flex; flex-direction: column; gap: 6px">
        <i class="ph-fill ph-film-reel" style="font-size: 26px; color: var(--color-accent)"></i>
        <div class="dialog-title">씨네위드 시작하기</div>
        <div class="dialog-body" style="font-size: 13px">
          소셜 계정으로 3초 만에 가입됩니다. 리뷰·스크랩·팔로우는 로그인 후 이용할 수 있어요.
        </div>
      </div>
      <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 2px">
        <a
          :href="googleLoginUrl()"
          class="btn btn-secondary btn-block"
          style="justify-content: flex-start; gap: 10px; height: 42px; margin-top: 0"
        >
          <span style="width: 20px; height: 20px; border-radius: 50%; background: var(--color-neutral-300); flex: none"></span>Google로 계속하기
        </a>
        <button class="btn btn-secondary btn-block" disabled style="justify-content: flex-start; gap: 10px; height: 42px; margin-top: 0">
          <span style="width: 20px; height: 20px; border-radius: var(--radius-sm); background: var(--color-neutral-400); flex: none"></span>Kakao로 계속하기 (준비 중)
        </button>
        <button class="btn btn-secondary btn-block" disabled style="justify-content: flex-start; gap: 10px; height: 42px; margin-top: 0">
          <span style="width: 20px; height: 20px; border-radius: var(--radius-sm); background: var(--color-neutral-500); flex: none"></span>Naver로 계속하기 (준비 중)
        </button>
        <button class="btn btn-secondary btn-block" disabled style="justify-content: flex-start; gap: 10px; height: 42px; margin-top: 0">
          <i class="ph-fill ph-github-logo" style="font-size: 20px"></i>GitHub로 계속하기 (준비 중)
        </button>
      </div>
      <div style="font: 400 11px/1.6 var(--font-body); color: color-mix(in srgb, var(--color-text) 42%, transparent)">
        계속하면 이용약관과 개인정보 처리방침에 동의하게 됩니다. 최초 로그인 시 닉네임만 추가로 입력합니다.
      </div>
    </div>
  </div>
</template>
