<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useAuth } from '../composables/useAuth'

const { state, completeSignup, closeSignupModal } = useAuth()
const nickname = ref('')
const genres = ref([])
const genreInput = ref('')
const errorMessage = ref('')
const submitting = ref(false)
const email = computed(() => state.signupContext?.email ?? '')
const initial = computed(() => email.value.charAt(0).toUpperCase() || '?')

function addGenre() {
  const genre = genreInput.value.trim()
  if (genre && !genres.value.includes(genre)) genres.value.push(genre)
  genreInput.value = ''
}

function removeGenre(genre) {
  genres.value = genres.value.filter((item) => item !== genre)
}

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
          <span class="av" style="width: 38px; height: 38px; font-size: 14px">{{ initial }}</span>
          <div>
            <div style="font: 500 14px/1.3 var(--font-heading)">Google 계정으로 연결됨</div>
            <div style="font: 400 11.5px/1.4 var(--font-body); color: color-mix(in srgb, var(--color-text) 45%, transparent)">{{ email || 'Google 계정 정보를 불러오지 못했습니다.' }}</div>
          </div>
        </div>
        <div class="field">
          <label>게시판에서 쓸 닉네임</label>
          <input class="input" v-model="nickname" placeholder="닉네임을 입력하세요" />
        </div>
        <div class="field">
          <label>관심 장르 <span class="meta">선택</span></label>
          <div style="display: flex; gap: 8px">
            <input v-model="genreInput" class="input" placeholder="예: 액션" @keydown.enter.prevent="addGenre" />
            <button type="button" class="btn btn-secondary" @click="addGenre">추가</button>
          </div>
          <div v-if="genres.length" style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px">
            <button v-for="genre in genres" :key="genre" type="button" class="tag tag-outline" @click="removeGenre(genre)">{{ genre }} ×</button>
          </div>
        </div>
        <div v-if="errorMessage" style="color: var(--color-danger, #d66); font-size: 12px">{{ errorMessage }}</div>
        <button class="btn btn-primary btn-block" :disabled="submitting" @click="submit">씨네위드 시작하기</button>
      </div>
    </div>
  </div>
</template>
