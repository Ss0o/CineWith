<script setup>
import { computed } from 'vue'

const props = defineProps({
  rating: { type: Number, required: true }, // 0.5 ~ 5.0 user-facing scale
  size: { type: String, default: '12px' },
  showValue: { type: Boolean, default: true },
})

const stars = computed(() => {
  const full = Math.round(props.rating)
  return '★'.repeat(full) + '☆'.repeat(5 - full)
})
</script>

<template>
  <span class="stars" :style="{ fontSize: size }">{{ stars }}<template v-if="showValue"> {{ rating.toFixed(1) }}</template></span>
</template>
