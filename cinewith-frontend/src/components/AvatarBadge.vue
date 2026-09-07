<script setup>
defineProps({
  initial: { type: String, required: true },
  size: { type: String, default: '30px' },
  fontSize: { type: String, default: '11px' },
})
</script>

<template>
  <span class="av" :style="{ width: size, height: size, fontSize }">{{ initial }}</span>
</template>
