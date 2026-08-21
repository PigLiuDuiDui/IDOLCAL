<script setup>
import Icon from './Icon.vue'
import { t } from '../i18n'

defineProps({
  icon: { type: String, default: 'box' },
  title: { type: String, default: () => t('common.noData') },
  message: { type: String, default: '' },
  /** 操作按钮文字（为空不显示按钮） */
  actionText: { type: String, default: '' }
})

const emit = defineEmits(['action'])
</script>

<template>
  <div class="a-empty">
    <div class="a-empty-icon">
      <Icon :name="icon" :size="22" />
    </div>
    <h5>{{ title }}</h5>
    <p>{{ message }}</p>
    <button v-if="actionText" class="a-btn a-btn-primary" @click="emit('action')">
      <Icon name="plus" :size="14" />
      {{ actionText }}
    </button>
  </div>
</template>
