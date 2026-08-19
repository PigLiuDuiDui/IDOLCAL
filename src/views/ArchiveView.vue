<template>
  <div class="archive-view container">
    <header class="page-head">
      <p class="eyebrow">{{ t('archive.eyebrow', { year: data.currentArtist.year }) }}</p>
      <h1 class="page-title">ARCHIVE</h1>
      <p class="page-sub">
        {{ t('archive.sub') }}
      </p>
    </header>

    <!-- 归档索引：年份 → 月份分组 -->
    <section v-if="years.length" class="archive-index">
      <div v-for="year in years" :key="year.year" class="arch-year">
        <h2 class="arch-year-title">{{ year.year }}</h2>

        <div class="arch-months">
          <section v-for="month in year.months" :key="month.label" class="arch-month">
            <h3 class="arch-month-title">{{ month.label }}</h3>

            <div class="arch-list">
              <button
                v-for="item in month.items"
                :key="item.id"
                type="button"
                class="arch-row"
                @click="ui.openEvent(item.id)"
              >
                <span class="arch-date">{{ shortDate(item.date) }}</span>
                <span class="arch-type" :data-type="item.type">{{ t(`types.${item.type}`) }}</span>
                <span class="arch-title">{{ text(item.title) }}</span>
                <span class="arch-src">{{ item.sourceName }}</span>
                <span class="arch-arrow" aria-hidden="true">→</span>
              </button>
            </div>
          </section>
        </div>
      </div>
    </section>

    <p v-else class="archive-empty">
      {{ t('archive.empty') }}
    </p>
  </div>
</template>

<script setup>
// Archive：历史活动归档（已过去的日期）
// 档案索引风格：按年份 / 月份分组，克制无卡片
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { monthKeyOf, monthLabel, shortDate, todayKey } from '../utils/date'
import { useText } from '../i18n'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const data = useDataStore()

const pastEvents = computed(() => {
  const today = todayKey()
  const past = data.eventsSorted.filter((e) => e.date < today)

  // 应用类型筛选
  return ui.activeTypes.length === 0
    ? past
    : past.filter((e) => ui.activeTypes.includes(e.type))
})

// 年份 → 月份 分组
const years = computed(() => {
  const yearMap = new Map()
  for (const e of pastEvents.value) {
    const year = e.date.slice(0, 4)
    if (!yearMap.has(year)) yearMap.set(year, new Map())
    const monthKey = monthKeyOf(e.date)
    const months = yearMap.get(year)
    if (!months.has(monthKey)) months.set(monthKey, [])
    months.get(monthKey).push(e)
  }
  return [...yearMap.entries()].map(([year, monthMap]) => ({
    year,
    months: [...monthMap.entries()].map(([key, items]) => ({
      label: monthLabel(key),
      items
    }))
  }))
})
</script>

<style scoped>
.archive-view {
  padding-top: 72px;
  padding-bottom: 120px;
  max-width: 960px;
}

.page-head {
  margin-bottom: 56px;
}

.page-title {
  font-family: var(--serif);
  font-size: clamp(40px, 6vw, 64px);
  font-weight: 400;
  letter-spacing: 0.1em;
  margin: 12px 0 16px;
}

.page-sub {
  font-size: 13px;
  color: var(--ink-soft);
  max-width: 560px;
  line-height: 1.8;
}

/* 年份分区 */
.arch-year {
  margin-bottom: 64px;
}

.arch-year-title {
  font-family: var(--serif);
  font-size: 40px;
  font-weight: 400;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
  border-bottom: 1px solid var(--line-strong);
  padding-bottom: 14px;
  margin-bottom: 32px;
}

.arch-months {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 40px;
  align-items: start;
}

.arch-month-title {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.28em;
  color: var(--ink-soft);
  padding-bottom: 10px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--line);
}

/* 档案行：索引式，无卡片 */
.arch-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  width: 100%;
  min-height: 44px;
  padding: 8px 8px;
  margin-left: -8px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--dur) var(--ease);
  min-width: 0;
}

.arch-row:hover {
  background: var(--surface-alt);
}

.arch-row:hover .arch-arrow {
  transform: translateX(3px);
  color: var(--accent);
}

.arch-date {
  font-family: var(--mono);
  font-size: 11px;
  color: var(--ink-faint);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
  width: 46px;
}

.arch-type {
  flex-shrink: 0;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.18em;
  width: 78px;
}

.arch-type[data-type='RELEASE'] { color: var(--t-release); }
.arch-type[data-type='EVENT'] { color: var(--t-event); }
.arch-type[data-type='TV'] { color: var(--t-tv); }
.arch-type[data-type='LIVE'] { color: var(--t-live); }
.arch-type[data-type='PHOTO'] { color: var(--t-photo); }
.arch-type[data-type='MAGAZINE'] { color: var(--t-magazine); }
.arch-type[data-type='OFFLINE'] { color: var(--t-offline); }
.arch-type[data-type='BRAND'] { color: var(--t-brand); }
.arch-type[data-type='BIRTHDAY'] { color: var(--t-birthday); }

.arch-title {
  flex: 1;
  font-family: var(--serif);
  font-size: 14px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.arch-src {
  flex-shrink: 0;
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arch-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--ink-faint);
  transition: transform var(--dur) var(--ease), color var(--dur) var(--ease);
}

.archive-empty {
  padding: 48px 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: var(--ink-faint);
  border-top: 1px solid var(--line);
}

@media (max-width: 640px) {
  .archive-view {
    padding-top: 48px;
    padding-bottom: 80px;
  }

  .page-head {
    margin-bottom: 40px;
  }

  .arch-months {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .arch-src {
    display: none;
  }
}
</style>
