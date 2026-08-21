<script setup>
// 多系列折线图（SVG，无依赖）：series = [{ name, color, values: [] }]
import { computed } from 'vue'

const props = defineProps({
  series: { type: Array, required: true },
  labels: { type: Array, default: () => [] },
  height: { type: Number, default: 240 },
  /** 是否显示网格 */
  grid: { type: Boolean, default: true },
  loading: { type: Boolean, default: false }
})

const W = 640
const PAD = { top: 12, right: 12, bottom: 24, left: 34 }

const dims = computed(() => {
  const w = W - PAD.left - PAD.right
  const h = props.height - PAD.top - PAD.bottom
  return { w, h }
})

const allValues = computed(() => props.series.flatMap((s) => s.values))
const maxV = computed(() => Math.max(1, ...allValues.value))
const minV = computed(() => Math.min(0, ...allValues.value))

/** y 轴刻度（3~4 条） */
const yTicks = computed(() => {
  const ticks = []
  for (let i = 0; i <= 3; i++) {
    ticks.push(minV.value + ((maxV.value - minV.value) * i) / 3)
  }
  return ticks
})

function xAt(i) {
  const n = Math.max(2, props.series[0]?.values.length || 2)
  return PAD.left + (dims.value.w * i) / (n - 1)
}

function yAt(v) {
  const range = maxV.value - minV.value || 1
  return PAD.top + dims.value.h - ((v - minV.value) / range) * dims.value.h
}

function linePath(values) {
  return values.map((v, i) => `${i === 0 ? 'M' : 'L'}${xAt(i).toFixed(1)},${yAt(v).toFixed(1)}`).join(' ')
}

function areaPath(values) {
  if (!values.length) return ''
  const base = yAt(minV.value)
  return `${linePath(values)} L${xAt(values.length - 1).toFixed(1)},${base.toFixed(1)} L${xAt(0).toFixed(1)},${base.toFixed(1)} Z`
}

const fmtTick = (v) => (v >= 1000 ? `${(v / 1000).toFixed(v >= 10000 ? 0 : 1)}k` : Math.round(v))
</script>

<template>
  <div class="a-chart">
    <div v-if="loading" class="a-skeleton" :style="{ height: `${height}px` }" />
    <template v-else>
      <svg :viewBox="`0 0 ${W} ${height}`" :height="height" preserveAspectRatio="xMidYMid meet" role="img">
        <!-- 网格 + y 轴刻度 -->
        <template v-if="grid">
          <line
            v-for="(t, i) in yTicks"
            :key="i"
            :x1="PAD.left"
            :x2="W - PAD.right"
            :y1="yAt(t)"
            :y2="yAt(t)"
            stroke="var(--a-border)"
            stroke-width="1"
          />
          <text
            v-for="(t, i) in yTicks"
            :key="`t${i}`"
            :x="PAD.left - 8"
            :y="yAt(t) + 4"
            text-anchor="end"
            font-size="10.5"
            fill="var(--a-text-3)"
          >{{ fmtTick(t) }}</text>
        </template>

        <!-- x 轴标签 -->
        <text
          v-for="(l, i) in labels"
          :key="`x${i}`"
          :x="xAt(i)"
          :y="height - 6"
          text-anchor="middle"
          font-size="10.5"
          fill="var(--a-text-3)"
        >{{ l }}</text>

        <!-- 面积 + 折线 -->
        <template v-for="(s, si) in series" :key="si">
          <path
            v-if="s.values.length > 1"
            :d="areaPath(s.values)"
            :fill="s.color"
            fill-opacity="0.07"
            stroke="none"
          />
          <polyline
            v-if="s.values.length > 1"
            :points="s.values.map((v, i) => `${xAt(i).toFixed(1)},${yAt(v).toFixed(1)}`).join(' ')"
            fill="none"
            :stroke="s.color"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <circle
            v-for="(v, i) in s.values"
            :key="i"
            :cx="xAt(i)"
            :cy="yAt(v)"
            r="2.5"
            :fill="s.color"
            stroke="var(--a-card)"
            stroke-width="1.2"
          />
        </template>
      </svg>
      <div v-if="series.some((s) => s.name)" class="a-chart-legend">
        <span v-for="(s, si) in series" :key="si" class="item">
          <span class="swatch" :style="{ background: s.color }" />
          {{ s.name }}
        </span>
      </div>
    </template>
  </div>
</template>
