<script setup lang="ts">
withDefaults(
  defineProps<{
    showChrome?: boolean
  }>(),
  {
    showChrome: true,
  },
)

import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'
</script>

<template>
  <div class="page-shell">
    <AppHeader v-if="showChrome" />
    <main>
      <slot />
    </main>
    <AppFooter v-if="showChrome" />
  </div>
</template>
