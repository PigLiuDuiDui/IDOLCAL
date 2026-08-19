<template>
  <section class="upcoming">
    <div class="upcoming-head">
      <h2 class="upcoming-title">{{ t('upcoming.title') }}</h2>
      <span class="upcoming-note">{{ t('upcoming.note') }}</span>
    </div>

    <div class="upcoming-list">
      <template v-for="(item, i) in list" :key="item.id">
        <!-- 最近的一个活动：NEXT 强调 -->
        <div v-if="i === 0" class="next-badge">
          <span class="next-badge-label">{{ t('upcoming.next') }}</span>
          <span class="next-badge-count">{{ countdownLabel(item.date) }}</span>
        </div>
        <EventCard :event="item" compact :featured="i === 0" />
      </template>
    </div>

    <p v-if="list.length === 0" class="upcoming-empty">
      {{ t('upcoming.empty') }}
    </p>
  </section>
</template>

<script setup>
// 未来 5～7 个活动的纵向时间线
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import EventCard from './EventCard.vue'
import { todayKey, countdownLabel } from '../utils/date'
import { useUiStore } from '../stores/ui'

const { t } = useI18n()
const ui = useUiStore()
const data = useDataStore()

const list = computed(() => {
  const today = todayKey()
  const upcoming = data.eventsSorted.filter((e) => e.date >= today)

  // 应用类型筛选（未筛选时显示全部）
  const filtered =
    ui.activeTypes.length === 0
      ? upcoming
      : upcoming.filter((e) => ui.activeTypes.includes(e.type))

  return filtered.slice(0, 7)
})
</script>

<style scoped>
.upcoming-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  border-bottom: 1px solid var(--line);
  padding-bottom: 14px;
  margin-bottom: 6px;
}

.upcoming-title {
  font-family: var(--serif);
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0.12em;
}

.upcoming-note {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.24em;
  color: var(--ink-faint);
}

.next-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 20px 0 10px;
}

.next-badge-label {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.3em;
  color: #fff;
  background: var(--accent);
  padding: 4px 12px;
  border-radius: 999px;
}

.next-badge-count {
  font-family: var(--mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--accent);
}

.upcoming-empty {
  padding: 32px 0;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: var(--ink-faint);
}
</style>
