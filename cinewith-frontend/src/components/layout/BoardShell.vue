<script setup>
import { ref } from 'vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import MobileDrawer from './MobileDrawer.vue'
import BottomTabBar from './BottomTabBar.vue'

defineProps({
  hasRightRail: { type: Boolean, default: false },
})

const isDrawerOpen = ref(false)
</script>

<template>
  <div style="display: flex; flex-direction: column; min-height: 100vh">
    <AppHeader @toggle-drawer="isDrawerOpen = true" />

    <div style="display: flex; flex: 1; min-height: 0">
      <div
        class="desktop-only"
        style="width: 228px; flex: none; padding: 20px 12px; border-right: 1px solid color-mix(in srgb, var(--color-text) 8%, transparent)"
      >
        <AppSidebar />
      </div>

      <div style="flex: 1; min-width: 0; display: flex; flex-direction: column">
        <slot />
      </div>

      <div
        v-if="hasRightRail"
        class="desktop-only"
        style="width: 280px; flex: none; padding: 24px 20px; border-left: 1px solid color-mix(in srgb, var(--color-text) 8%, transparent)"
      >
        <slot name="right-rail" />
      </div>
    </div>

    <BottomTabBar />
    <MobileDrawer :open="isDrawerOpen" @close="isDrawerOpen = false" />
  </div>
</template>
