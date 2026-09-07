<script setup>
import { RouterLink, useRoute } from 'vue-router'

const tabs = [
  { to: '/', icon: 'ph ph-magnifying-glass', label: '탐색' },
  { to: '/reviews/new', icon: 'ph ph-note-pencil', label: '작성' },
  { to: '/mypage', icon: 'ph ph-user', label: 'MY' },
]

const route = useRoute()
</script>

<template>
  <div class="mobile-only fade-rule" style="margin: 0"></div>
  <div class="mobile-only" style="display: flex; justify-content: space-around; padding: 10px 8px 14px; background: var(--color-bg)">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      style="display: flex; flex-direction: column; align-items: center; gap: 4px; width: 56px; text-decoration: none"
      :style="{ color: route.path === tab.to ? 'var(--color-accent)' : 'color-mix(in srgb, var(--color-text) 50%, transparent)' }"
    >
      <i :class="tab.icon" style="font-size: 20px"></i>
      <span style="font: 400 10px/1 var(--font-body)">{{ tab.label }}</span>
    </RouterLink>
  </div>
</template>
