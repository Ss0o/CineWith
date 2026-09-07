<script setup>
defineProps({ page: Number, totalPages: Number, loading: Boolean, label: { type: String, default: '목록' } })
defineEmits(['change'])
</script>

<template>
  <nav v-if="totalPages > 0" :aria-label="`${label} 페이지 이동`" class="pagination">
    <button class="btn btn-secondary" :disabled="loading || page === 0" @click="$emit('change', page - 1)">이전</button>
    <span aria-live="polite">{{ page + 1 }} / {{ totalPages }} 페이지</span>
    <button class="btn btn-secondary" :disabled="loading || page + 1 >= totalPages" @click="$emit('change', page + 1)">다음</button>
  </nav>
</template>

<style scoped>
.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin: 20px 0; }
</style>
