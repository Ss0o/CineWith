<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuth } from '../composables/useAuth'

const { state, completeSignup, closeSignupModal } = useAuth()
const nickname = ref('영화광지현')
const genres = ref(['드라마', '애니메이션'])
const errorMessage = ref('')
const submitting = ref(false)

async function submit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    await completeSignup(nickname.value)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    submitting.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Escape' && state.isSignupModalOpen) closeSignupModal()
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div v-if="state.isSignupModalOpen" class="dialog-backdrop" @click.self="closeSignupModal">
    <div class="dialog" style="width: 380px; gap: 14px; padding: 22px; position: relative">
      <button
        class="btn btn-icon btn-secondary"
        style="position: absolute; top: 12px; right: 12px; border-color: transparent"
        aria-label="닫기"
        @click="closeSignupModal"
      >
        <i class="ph ph-x" style="font-size: 16px"></i>
      </button>
      <div class="dialog-title">씨네위드 시작하기</div>
      <div style="padding: 18px; border-radius: var(--radius-md); background: var(--color-surface); display: flex; flex-direction: column; gap: 12px">
        <div style="display: flex; align-items: center; gap: 10px">
          <span class="av" style="width: 38px; height: 38px; font-size: 14px">지</span>
          <div>
            <div style="font: 500 14px/1.3 var(--font-heading)">Google 계정으로 연결됨</div>
            <div style="font: 400 11.5px/1.4 var(--font-body); color: color-mix(in srgb, var(--color-text) 45%, transparent)">jihyeon@gmail.com</div>
          </div>
        </div>
        <div class="field">
          <label>게시판에서 쓸 닉네임</label>
          <input class="input" v-model="nickname" />
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <span v-for="genre in genres" :key="genre" class="tag tag-outline">{{ genre }}</span>
          <span class="tag tag-neutral" style="cursor: pointer">+ 장르 추가</span>
        </div>
        <div v-if="errorMessage" style="color: var(--color-danger, #d66); font-size: 12px">{{ errorMessage }}</div>
        <button class="btn btn-primary btn-block" :disabled="submitting" @click="submit">씨네위드 시작하기</button>
      </div>
    </div>
  </div>
</template>
