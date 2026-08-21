<script setup>
defineProps({
  /** 0~100 */
  value: { type: Number, default: 0 },
  /** 状态色：brand / success / warning / error */
  tone: { type: String, default: 'brand' },
  /** 显示百分比文字 */
  showValue: { type: Boolean, default: true }
})
</script>

<template>
  <div class="a-progress">
    <div class="a-progress-track">
      <div class="a-progress-fill" :class="tone" :style="{ width: `${Math.min(100, Math.max(0, value))}%` }" />
    </div>
    <span v-if="showValue" class="a-progress-value">{{ value }}%</span>
  </div>
</template>
