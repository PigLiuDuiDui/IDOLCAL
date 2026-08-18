<template>
  <div class="timeline-view container">
    <header class="page-head">
      <p class="eyebrow">{{ t('timeline.eyebrow') }}</p>
      <h1 class="page-title">TIMELINE</h1>
      <p class="page-sub">
        {{ t('timeline.sub', { artist: currentArtist.name, era: currentArtist.era }) }}
      </p>
    </header>

    <!-- 时间线主体 -->
    <div class="timeline" v-if="grouped.length">
      <section v-for="group in grouped" :key="group.month" class="tl-month">
        <div class="tl-month-head">
          <h2 class="tl-month-title">{{ group.label }}</h2>
          <span class="tl-month-count">{{ t('timeline.eventCount', { n: group.items.length }) }}</span>
        </div>

        <div class="tl-entries">
          <div v-for="item in group.items" :key="item.id" class="tl-entry">
            <div class="tl-rail">
              <span class="tl-node" :data-type="item.type" aria-hidden="true"></span>
            </div>

            <button type="button" class="tl-row" @click="ui.openEvent(item.id)">
              <span class="tl-date">{{ shortDate(item.date) }}<span v-if="item.endDate" class="tl-date-end">–{{ shortDate(item.endDate) }}</span></span>
              <span class="tl-type" :data-type="item.type">{{ t(`types.${item.type}`) }}</span>
              <span class="tl-title">{{ text(item.title) }}</span>
              <span v-if="item.time" class="tl-time">{{ item.time }} {{ item.timezone }}</span>
              <span class="tl-status" :data-status="item.status">{{ t(`status.${item.status}`) }}</span>
            </button>
          </div>
        </div>
      </section>
    </div>

    <p v-else class="timeline-empty">
      {{ t('timeline.empty') }}
    </p>

    <!-- 底部链接到完整 Archive -->
    <footer class="timeline-foot">
      <RouterLink to="/archive" class="link-quiet">{{ t('timeline.browseArchive') }}</RouterLink>
    </footer>
  </div>
</template>

<script setup>
// Timeline：快速浏览整个时期（月度分组纵向时间线，Archive 感）
// 拒绝企业项目管理风格：无甘特条、无连线色块，只有档案索引式排版
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { eventsSorted } from '../data/events'
import { currentArtist } from '../data/artists'
import { useUiStore } from '../stores/ui'
import { monthKeyOf, monthLabel, shortDate } from '../utils/date'
import { useText } from '../i18n'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()

// 按月份分组（升序），应用类型筛选
const grouped = computed(() => {
  const filtered =
    ui.activeTypes.length === 0
      ? eventsSorted
      : eventsSorted.filter((e) => ui.activeTypes.includes(e.type))

  const map = new Map()
  for (const e of filtered) {
    const key = monthKeyOf(e.date)
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(e)
  }
  return [...map.entries()].map(([month, items]) => ({
    month,
    label: monthLabel(month),
    items
  }))
})
</script>

<style scoped>
.timeline-view {
  padding-top: 72px;
  padding-bottom: 120px;
  max-width: 860px;
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
  max-width: 520px;
  line-height: 1.8;
}

/* 月份分组 */
.tl-month {
  margin-bottom: 56px;
}

.tl-month-head {
  display: flex;
  align-items: baseline;
  gap: 16px;
  border-bottom: 1px solid var(--line-strong);
  padding-bottom: 14px;
  margin-bottom: 8px;
}

.tl-month-title {
  font-family: var(--serif);
  font-size: 24px;
  font-weight: 400;
  letter-spacing: 0.1em;
}

.tl-month-count {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: var(--ink-faint);
}

/* 档案索引式条目 */
.tl-entries {
  position: relative;
}

.tl-entry {
  display: flex;
}

.tl-rail {
  flex-shrink: 0;
  width: 28px;
  position: relative;
}

/* 竖向细线 */
.tl-rail::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--line);
}

.tl-entry:first-child .tl-rail::before {
  top: 50%;
}

.tl-entry:last-child .tl-rail::before {
  bottom: 50%;
}

/* 节点：类型色小点 */
.tl-node {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--ink-faint);
  z-index: 1;
}

.tl-node[data-type='RELEASE'] { background: var(--t-release); }
.tl-node[data-type='EVENT'] { background: var(--t-event); }
.tl-node[data-type='TV'] { background: var(--t-tv); }
.tl-node[data-type='LIVE'] { background: var(--t-live); }
.tl-node[data-type='PHOTO'] { background: var(--t-photo); }
.tl-node[data-type='MAGAZINE'] { background: var(--t-magazine); }
.tl-node[data-type='OFFLINE'] { background: var(--t-offline); }
.tl-node[data-type='BRAND'] { background: var(--t-brand); }
.tl-node[data-type='BIRTHDAY'] { background: var(--t-birthday); }

/* 行：无卡片边框，hover 时轻微背景 */
.tl-row {
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 16px;
  min-height: 48px; /* 点击区域 ≥ 44px */
  padding: 10px 12px;
  margin-left: -12px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--dur) var(--ease);
  min-width: 0;
}

.tl-row:hover {
  background: var(--surface-alt);
}

.tl-date {
  font-family: var(--mono);
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-soft);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.tl-date-end {
  color: var(--ink-faint);
  font-size: 10px;
}

.tl-type {
  flex-shrink: 0;
  width: 84px;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.2em;
}

.tl-type[data-type='RELEASE'] { color: var(--t-release); }
.tl-type[data-type='EVENT'] { color: var(--t-event); }
.tl-type[data-type='TV'] { color: var(--t-tv); }
.tl-type[data-type='LIVE'] { color: var(--t-live); }
.tl-type[data-type='PHOTO'] { color: var(--t-photo); }
.tl-type[data-type='MAGAZINE'] { color: var(--t-magazine); }
.tl-type[data-type='OFFLINE'] { color: var(--t-offline); }
.tl-type[data-type='BRAND'] { color: var(--t-brand); }
.tl-type[data-type='BIRTHDAY'] { color: var(--t-birthday); }

.tl-title {
  flex: 1;
  font-family: var(--serif);
  font-size: 15px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.tl-time {
  flex-shrink: 0;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
}

.tl-status {
  flex-shrink: 0;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--ink-faint);
}

.tl-status[data-status='CONFIRMED'] {
  color: var(--accent);
}

.tl-status[data-status='RUMORED'] {
  font-weight: 400;
}

.timeline-empty {
  padding: 48px 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: var(--ink-faint);
  border-top: 1px solid var(--line);
}

.timeline-foot {
  margin-top: 16px;
  padding-top: 28px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  letter-spacing: 0.1em;
}

/* 移动端：紧凑 */
@media (max-width: 640px) {
  .timeline-view {
    padding-top: 48px;
    padding-bottom: 80px;
  }

  .page-head {
    margin-bottom: 40px;
  }

  .tl-month {
    margin-bottom: 44px;
  }

  .tl-row {
    gap: 10px;
    flex-wrap: wrap;
    padding: 8px 8px;
  }

  .tl-type {
    order: 3;
    width: auto;
    margin-left: 0;
  }

  .tl-title {
    order: 2;
    white-space: normal;
  }

  .tl-time {
    order: 4;
  }

  .tl-status {
    order: 5;
  }
}
</style>
