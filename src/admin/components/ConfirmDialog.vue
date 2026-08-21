<script setup>
import Icon from './Icon.vue'
import { t } from '../i18n'

defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: () => t('common.confirmAction') },
  message: { type: String, default: '' },
  /** 危险操作（红色图标） */
  danger: { type: Boolean, default: true },
  confirmText: { type: String, default: () => t('common.confirm') },
  cancelText: { type: String, default: () => t('common.cancel') },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="a-overlay" @click.self="!loading && emit('cancel')">
      <div class="a-dialog" role="dialog" aria-modal="true">
        <div class="a-dialog-body">
          <div class="a-dialog-icon" :class="{ warn: !danger }">
            <Icon :name="danger ? 'alert-triangle' : 'help-circle'" :size="22" />
          </div>
          <h4>{{ title }}</h4>
          <p>{{ message }}</p>
        </div>
        <div class="a-dialog-actions">
          <button class="a-btn" :disabled="loading" @click="emit('cancel')">{{ cancelText }}</button>
          <button class="a-btn" :class="danger ? 'a-btn-danger' : 'a-btn-primary'" :disabled="loading" @click="emit('confirm')">
            <Icon v-if="loading" name="loader" :size="14" class="spin" />
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
