<script setup>
// 环形图（SVG，无依赖）：segments = [{ label, value, color }]
import { computed } from 'vue'

const props = defineProps({
  segments: { type: Array, default: () => [] },
  size: { type: Number, default: 180 },
  thickness: { type: Number, default: 22 },
  /** 中心主文案 */
  centerLabel: { type: String, default: '' },
  centerValue: { type: [String, Number], default: '' },
  loading: { type: Boolean, default: false }
})

const total = computed(() => props.segments.reduce((s, x) => s + (x.value || 0), 0))

const radius = computed(() => (props.size - props.thickness) / 2)

const C = computed(() => 2 * Math.PI * radius.value)

/** 每段描边偏移（环形分段） */
const arcs = computed(() => {
  if (!total.value) return []
  let offset = 0
  return props.segments.map((s) => {
    const frac = s.value / total.value
    const arc = {
      ...s,
      frac,
      dash: `${frac * C.value} ${C.value}`,
      offset: -offset * C.value
    }
    offset += frac
    return arc
  })
})
</script>

<template>
  <div class="a-chart" style="display: flex; flex-direction: column; align-items: center">
    <div v-if="loading" class="a-skeleton" :style="{ width: `${size}px`, height: `${size}px`, borderRadius: '50%' }" />
    <template v-else>
      <div :style="{ position: 'relative', width: `${size}px`, height: `${size}px` }">
        <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`" role="img">
          <!-- 底环 -->
          <circle
            :cx="size / 2"
            :cy="size / 2"
            :r="radius"
            fill="none"
            stroke="var(--a-border)"
            :stroke-width="thickness"
          />
          <!-- 分段 -->
          <circle
            v-for="(a, i) in arcs"
            :key="i"
            :cx="size / 2"
            :cy="size / 2"
            :r="radius"
            fill="none"
            :stroke="a.color"
            :stroke-width="thickness"
            stroke-linecap="butt"
            :stroke-dasharray="a.dash"
            :stroke-dashoffset="a.offset"
            transform="rotate(-90 90 90)"
          />
        </svg>
        <div
          class="a-donut-center"
          :style="{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }"
        >
          <b style="font-size: 22px; font-weight: 700; color: var(--a-text); font-variant-numeric: tabular-nums">{{ centerValue }}</b>
          <span style="font-size: 11.5px; color: var(--a-text-3)">{{ centerLabel }}</span>
        </div>
      </div>
      <div class="a-chart-legend" style="justify-content: center">
        <span v-for="(s, i) in segments" :key="i" class="item">
          <span class="swatch" :style="{ background: s.color }" />
          {{ s.label }} <b style="font-weight: 600; color: var(--a-text)">{{ s.value }}</b>
        </span>
      </div>
    </template>
  </div>
</template>
