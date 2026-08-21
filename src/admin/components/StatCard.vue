<script setup>
import { computed } from 'vue'
import Icon from './Icon.vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '-' },
  /** 趋势文案：+8.4% */
  trend: { type: String, default: '' },
  /** trend 方向：up / down / flat（缺省按符号推断） */
  trendDir: { type: String, default: '' },
  sub: { type: String, default: '' },
  icon: { type: String, default: '' },
  /** 迷你趋势数据（数组） */
  spark: { type: Array, default: null },
  loading: { type: Boolean, default: false }
})

const dir = computed(() => {
  if (props.trendDir) return props.trendDir
  if (!props.trend) return 'flat'
  return String(props.trend).startsWith('-') ? 'down' : 'up'
})

const sparkPoints = computed(() => {
  const data = props.spark || []
  if (data.length < 2) return ''
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  return data
    .map((v, i) => {
      const x = (i / (data.length - 1)) * 72
      const y = 20 - ((v - min) / range) * 18 - 1
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})
</script>

<template>
  <div class="a-card a-kpi">
    <div class="a-kpi-top">
      <span class="a-kpi-label">{{ label }}</span>
      <span v-if="icon" class="a-kpi-icon">
        <Icon :name="icon" :size="17" />
      </span>
    </div>

    <div v-if="loading" class="a-skeleton" style="height: 34px; width: 60%" />

    <template v-else>
      <div class="a-kpi-value">{{ value }}</div>
      <div class="a-kpi-foot">
        <span v-if="trend" class="a-trend" :class="dir">
          <Icon :name="dir === 'down' ? 'trending-down' : 'trending-up'" :size="13" />
          {{ trend }}
        </span>
        <span v-if="sub">{{ sub }}</span>
        <svg
          v-if="spark && spark.length > 1"
          class="a-spark"
          :width="72"
          :height="22"
          viewBox="0 0 72 22"
          preserveAspectRatio="none"
        >
          <polyline
            :points="sparkPoints"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </div>
    </template>
  </div>
</template>

<style scoped>
.a-spark {
  margin-left: auto;
  color: var(--a-primary);
}
</style>
