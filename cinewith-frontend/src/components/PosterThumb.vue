<script setup>
import { computed, ref, watch } from 'vue'
import { tmdbPosterUrl } from '../utils/tmdbImage'

const props = defineProps({
  width: { type: String, default: '62px' },
  height: { type: String, default: '90px' },
  label: { type: String, default: '' },
  posterPath: { type: String, default: null },
})
const imageFailed = ref(false)
const imageUrl = computed(() => imageFailed.value ? null : tmdbPosterUrl(props.posterPath))
watch(() => props.posterPath, () => { imageFailed.value = false })
</script>

<template>
  <div class="poster" :style="{ width, height }">
    <img v-if="imageUrl" :src="imageUrl" :alt="`${label || '영화'} 포스터`" loading="lazy" @error="imageFailed = true" />
    <span v-else>{{ label || '포스터 없음' }}</span>
  </div>
</template>

<style scoped>
img { width: 100%; height: 100%; object-fit: cover; display: block; }
</style>
