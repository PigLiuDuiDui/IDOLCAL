<script setup>
import Icon from './Icon.vue'
import { toasts, dismiss } from '../toast'

const ICONS = { success: 'check', error: 'x', warning: 'alert-triangle', info: 'info' }
</script>

<template>
  <div class="a-toast-wrap">
    <TransitionGroup name="toast">
      <div v-for="t in toasts" :key="t.id" class="a-toast" :class="t.type">
        <Icon :name="ICONS[t.type] || 'info'" :size="16" />
        <span>{{ t.message }}</span>
        <button class="a-icon-btn" style="margin-left: auto" @click="dismiss(t.id)">
          <Icon name="x" :size="14" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
