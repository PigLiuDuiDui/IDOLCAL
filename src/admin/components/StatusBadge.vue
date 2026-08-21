<script setup>
// 状态徽章：Push 任务/调度状态与通用结果状态统一映射
// 文案统一走 i18n（status.*），未收录的状态原样显示
import { computed } from 'vue'
import { t, adminLocale, messages } from '../i18n'

const props = defineProps({
  status: { type: String, default: '' },
  /** 显示小圆点（默认 true） */
  dot: { type: Boolean, default: true }
})

// 状态 → 徽章样式类
const CLS_MAP = {
  // Push 任务 / 调度状态
  PENDING: 'a-badge-slate',
  PROCESSING: 'a-badge-info',
  SUCCESS: 'a-badge-success',
  FAILED: 'a-badge-error',
  RETRY: 'a-badge-warning',
  // 投递结果
  EXPIRED: 'a-badge-warning',
  // 设备 / 订阅
  ACTIVE: 'a-badge-success',
  INACTIVE: 'a-badge-slate',
  ORPHAN: 'a-badge-warning',
  REGISTERED: 'a-badge-info',
  // 艺人 / 事件
  current: 'a-badge-brand',
  upcoming: 'a-badge-info',
  CONFIRMED: 'a-badge-success',
  TBA: 'a-badge-warning',
  RUMORED: 'a-badge-slate',
  ready: 'a-badge-success',
  coming: 'a-badge-slate',
  CURRENT: 'a-badge-brand',
  ARCHIVED: 'a-badge-slate',
  // 管理账号角色
  ADMIN: 'a-badge-brand',
  USER: 'a-badge-slate',
  // 通用布尔
  true: 'a-badge-success',
  false: 'a-badge-slate',
  // 来源可信度
  official: 'a-badge-success',
  fan: 'a-badge-info',
  rumor: 'a-badge-warning'
}

const meta = computed(() => {
  const s = props.status || ''
  const key = `status.${s}`
  const locale = adminLocale.value
  const hasKey =
    (messages[locale] || messages.en)[key] !== undefined || messages.en[key] !== undefined
  return {
    cls: CLS_MAP[s] || 'a-badge-slate',
    label: s ? (hasKey ? t(key) : s) : '—'
  }
})
</script>

<template>
  <span class="a-badge" :class="meta.cls">
    <span v-if="dot" class="dot" />
    {{ meta.label }}
  </span>
</template>
