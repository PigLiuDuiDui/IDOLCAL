<template>
  <div class="filter-bar">
    <div class="filter-scroll" role="group" :aria-label="t('calendar.switchView')">
      <button
        type="button"
        class="filter-pill"
        :class="{ active: isAllActive }"
        @click="selectAll"
      >
        {{ t('common.all') }}
      </button>

      <button
        v-for="type in types"
        :key="type.id"
        type="button"
        class="filter-pill"
        :class="{ active: isTypeActive(type.id) }"
        @click="ui.toggleType(type.id)"
      >
        <span class="pill-dot" :style="{ background: dotColor(type.id) }"></span>
        {{ text(type.label) }}
      </button>
    </div>
  </div>
</template>

<script setup>
// 轻量 Segmented / Pill Filter：支持多选，移动端横向滚动
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { EVENT_TYPES } from '../data/events'
import { useUiStore } from '../stores/ui'
import { useText } from '../i18n'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const types = EVENT_TYPES

const isAllActive = computed(() => ui.activeTypes.length === 0)

function isTypeActive(id) {
  return ui.activeTypes.includes(id)
}

function selectAll() {
  ui.clearTypes()
}

// 类型标记色（与全局 .type-marker 保持一致）
const DOT_COLORS = {
  RELEASE: 'var(--t-release)',
  EVENT: 'var(--t-event)',
  TV: 'var(--t-tv)',
  LIVE: 'var(--t-live)',
  PHOTO: 'var(--t-photo)',
  MAGAZINE: 'var(--t-magazine)',
  OFFLINE: 'var(--t-offline)',
  BRAND: 'var(--t-brand)',
  BIRTHDAY: 'var(--t-birthday)'
}

function dotColor(id) {
  return DOT_COLORS[id] || 'var(--ink-faint)'
}
</script>

<style scoped>
.filter-bar {
  border-bottom: 1px solid var(--line);
  background: var(--bg);
}

.filter-scroll {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 0;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.filter-scroll::-webkit-scrollbar {
  display: none;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-soft);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  user-select: none;
}

.filter-pill:hover {
  border-color: var(--line-strong);
  color: var(--ink);
  transform: translateY(-1px);
}

/* Active Filter：使用强调色（5% 强调色应用场景之一） */
.filter-pill.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.filter-pill.active .pill-dot {
  background: #fff !important;
  opacity: 0.85;
}

@media (max-width: 640px) {
  .filter-scroll {
    padding: 12px 0;
    margin: 0 -16px;
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
