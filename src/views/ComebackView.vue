<template>
  <div class="comeback-view">
    <!-- ============ 回归 Hero：仪式感倒计时 ============ -->
    <section class="cb-hero">
      <span class="cb-hero-year" aria-hidden="true">{{ artist.year }}</span>

      <div class="container cb-hero-inner">
        <p class="eyebrow cb-eyebrow">{{ t('comeback.eyebrow') }}</p>
        <p class="cb-artist">{{ artist.name }}</p>

        <div class="cb-center">
          <h1 class="cb-title">{{ comeback.title }}</h1>
          <div class="cb-countdown" :data-state="dState">{{ dLabel }}</div>
          <p class="cb-tagline">{{ text(comeback.tagline) }}</p>
        </div>

        <div class="cb-release">
          <span class="cb-release-label">{{ t('comeback.releaseDate') }}</span>
          <span class="cb-release-date">{{ editorialDate(comeback.releaseDate) }}</span>
          <span class="cb-release-time">
            {{ comeback.releaseTime }} {{ comeback.releaseTimezone }}
            <span v-if="localRelease" class="cb-release-local">
              · {{ t('timezone.yourTime') }} {{ localRelease.time }} {{ localRelease.tz }}
              <i v-if="localRelease.date !== comeback.releaseDate" class="cb-shift">
                {{ localRelease.date < comeback.releaseDate ? '−1' : '+1' }}
              </i>
            </span>
          </span>
        </div>
      </div>
    </section>

    <!-- 多回归切换（当前仅一个，预留） -->
    <div v-if="data.comebacks.length > 1" class="container cb-tabs">
      <button
        v-for="c in data.comebacks"
        :key="c.id"
        type="button"
        class="cb-tab"
        :class="{ active: c.id === comeback.id }"
        @click="selectComeback(c.id)"
      >
        {{ c.title }}
      </button>
    </div>

    <!-- ============ Comeback Timeline ============ -->
    <section class="container cb-timeline">
      <div class="cb-timeline-head">
        <h2 class="cb-timeline-title">{{ t('comeback.timeline') }}</h2>
        <span class="cb-timeline-note">{{ t('comeback.timelineNote') }}</span>
      </div>

      <div class="cb-stages">
        <div
          v-for="stage in stageItems"
          :key="stage.id"
          class="cb-stage"
          :data-status="stage.status"
        >
          <div class="cb-rail" aria-hidden="true">
            <span class="cb-node"></span>
          </div>

          <div class="cb-card" @click="ui.openEvent(stage.main.id)">
            <div class="cb-card-top">
              <span class="cb-stage-name">{{ t(`comeback.stages.${stage.id}`) }}</span>
              <span class="cb-status" :data-status="stage.status">
                {{ t(`comeback.statuses.${stage.status}`) }}
              </span>
            </div>

            <div class="cb-card-meta">
              <span class="cb-date">{{ editorialDate(stage.main.date) }}</span>
              <span class="cb-time">
                <EventTime :event="stage.main" inline />
              </span>
            </div>

            <!-- 多活动节点（如 Music Shows）：逐条列出，可分别跳转 -->
            <div v-if="stage.events.length > 1" class="cb-events">
              <button
                v-for="ev in stage.events"
                :key="ev.id"
                type="button"
                class="cb-event-row"
                @click.stop="ui.openEvent(ev.id)"
              >
                <span class="cb-event-type" :data-type="ev.type">{{ t(`types.${ev.type}`) }}</span>
                <span class="cb-event-title">{{ text(ev.title) }}</span>
                <span class="cb-event-arrow" aria-hidden="true">→</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
// Comeback Hub：回归专题页 —— 倒计时 + 节点时间线
// 节点复用 events.js 现有活动；D-Day 按官方时区（KST）日期计算
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { useTimezoneStore } from '../stores/timezone'
import { localizeEvent, todayKeyInZone, diffDaysKey } from '../utils/time'
import { editorialDate } from '../utils/date'
import { useText } from '../i18n'
import EventTime from '../components/EventTime.vue'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const timezone = useTimezoneStore()
const data = useDataStore()

const artist = computed(() => data.currentArtist)

// 当前专题（多专题时可在顶部切换）
const selectedId = ref(data.comebacks[0]?.id || null)
const comeback = computed(() => data.getComeback(selectedId.value))

function selectComeback(id) {
  selectedId.value = id
}

// 官方时区（KST）的今天
const todayKST = computed(() => todayKeyInZone('Asia/Seoul'))

// 倒计时：回归前 D-n / 当天 D-DAY / 结束后 D+n（保留为历史）
const dDiff = computed(() => diffDaysKey(todayKST.value, comeback.value.releaseDate))
const dLabel = computed(() => {
  if (dDiff.value === 0) return 'D-DAY'
  return dDiff.value > 0 ? `D-${dDiff.value}` : `D+${-dDiff.value}`
})
const dState = computed(() => {
  if (dDiff.value === 0) return 'today'
  return dDiff.value > 0 ? 'upcoming' : 'past'
})

// 官方发布时间在用户时区下的显示
const localRelease = computed(() => {
  const fake = {
    date: comeback.value.releaseDate,
    time: comeback.value.releaseTime,
    timezone: comeback.value.releaseTimezone
  }
  return localizeEvent(fake, timezone.displayZone).local
})

// 组装时间线节点：按标准节点顺序，引用现有事件
const stageItems = computed(() => {
  return data.comebackStages.map((s) => {
    const def = comeback.value.stages.find((st) => st.stage === s.id)
    const events = (def?.eventIds || [])
      .map((id) => data.getEventById(id))
      .filter(Boolean)
      .sort((a, b) => (a.date < b.date ? -1 : 1))
    const main = events[0] || null
    return {
      id: s.id,
      events,
      main,
      // 状态按官方时区日期计算：已完成弱化 / 当天突出 / 未来保持
      status: main ? stageStatus(main.date) : 'UPCOMING'
    }
  }).filter((s) => s.main)
})

function stageStatus(dateKey) {
  const today = todayKST.value
  if (dateKey < today) return 'COMPLETED'
  if (dateKey === today) return 'TODAY'
  return 'UPCOMING'
}
</script>

<style scoped>
/* ---------- Hero ---------- */
.cb-hero {
  position: relative;
  background: var(--bg-deep);
  color: var(--ink-invert);
  overflow: hidden;
}

.cb-hero-year {
  position: absolute;
  right: -0.05em;
  top: 50%;
  transform: translateY(-50%);
  font-family: var(--serif);
  font-size: clamp(160px, 22vw, 360px);
  line-height: 1;
  color: rgba(255, 255, 255, 0.04);
  letter-spacing: -0.02em;
  user-select: none;
  pointer-events: none;
}

.cb-hero-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-top: 88px;
  padding-bottom: 88px;
  gap: 8px;
}

.cb-eyebrow {
  color: var(--accent-soft);
  letter-spacing: 0.42em;
  margin-bottom: 10px;
}

.cb-artist {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5em;
  color: rgba(255, 255, 255, 0.55);
}

.cb-center {
  margin-top: 34px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.cb-title {
  font-family: var(--serif);
  font-size: clamp(44px, 7vw, 92px);
  font-weight: 400;
  font-style: italic;
  letter-spacing: 0.08em;
  line-height: 1;
}

.cb-countdown {
  font-family: var(--mono);
  font-size: clamp(64px, 10vw, 128px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.04em;
  color: #fff;
  padding: 26px 44px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.cb-countdown[data-state='today'] {
  color: #fff;
  background: var(--accent);
  border-color: var(--accent);
  box-shadow: 0 0 60px rgba(166, 47, 47, 0.4);
}

.cb-countdown[data-state='past'] {
  color: rgba(255, 255, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.1);
}

.cb-tagline {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.4em;
  color: rgba(255, 255, 255, 0.4);
  text-transform: uppercase;
}

.cb-release {
  margin-top: 40px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  padding-top: 20px;
  font-size: 11px;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.66);
}

.cb-release-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.3em;
  color: rgba(255, 255, 255, 0.4);
  text-transform: uppercase;
}

.cb-release-date {
  font-family: var(--serif);
  font-size: 15px;
  color: #fff;
}

.cb-release-time {
  font-family: var(--mono);
  font-size: 12px;
  color: #fff;
}

.cb-release-local {
  font-family: var(--sans);
  font-size: 10px;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.55);
}

.cb-shift {
  font-style: normal;
  font-family: var(--mono);
  font-size: 9px;
  color: var(--accent-soft);
  margin-left: 3px;
}

/* 多回归切换 */
.cb-tabs {
  display: flex;
  gap: 8px;
  padding-top: 24px;
}

.cb-tab {
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--ink-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.cb-tab.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

/* ---------- Timeline ---------- */
.cb-timeline {
  padding-top: 64px;
  padding-bottom: 120px;
  max-width: 720px;
}

.cb-timeline-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--line-strong);
  padding-bottom: 16px;
  margin-bottom: 8px;
}

.cb-timeline-title {
  font-family: var(--serif);
  font-size: 24px;
  font-weight: 400;
  letter-spacing: 0.14em;
}

.cb-timeline-note {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.24em;
  color: var(--ink-faint);
}

.cb-stages {
  position: relative;
}

.cb-stage {
  display: flex;
}

.cb-rail {
  flex-shrink: 0;
  width: 34px;
  position: relative;
}

.cb-rail::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--line);
}

.cb-stage:first-child .cb-rail::before {
  top: 50%;
}

.cb-stage:last-child .cb-rail::before {
  bottom: 50%;
}

/* 节点：状态区分 */
.cb-node {
  position: absolute;
  left: 50%;
  top: 26px;
  transform: translate(-50%, -50%);
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: var(--surface);
  border: 2px solid var(--line-strong);
  z-index: 1;
}

.cb-stage[data-status='COMPLETED'] .cb-node {
  border-color: var(--ink-faint);
  background: var(--ink-faint);
  opacity: 0.55;
}

.cb-stage[data-status='TODAY'] .cb-node {
  border-color: var(--accent);
  background: var(--accent);
  box-shadow: 0 0 0 5px var(--accent-soft);
}

.cb-stage[data-status='UPCOMING'] .cb-node {
  border-color: var(--accent);
}

/* 节点卡片 */
.cb-card {
  flex: 1;
  min-width: 0;
  margin: 8px 0 20px;
  padding: 18px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  cursor: pointer;
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease),
    opacity var(--dur) var(--ease);
}

.cb-card:hover {
  border-color: var(--line-strong);
  transform: translateY(-2px);
}

/* 已完成节点：视觉弱化但保留完整记录 */
.cb-stage[data-status='COMPLETED'] .cb-card {
  opacity: 0.55;
  background: transparent;
}

.cb-stage[data-status='COMPLETED'] .cb-card:hover {
  opacity: 0.85;
}

/* 当天节点：突出 */
.cb-stage[data-status='TODAY'] .cb-card {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent), 0 14px 34px rgba(166, 47, 47, 0.1);
}

.cb-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.cb-stage-name {
  font-family: var(--serif);
  font-size: 18px;
  letter-spacing: 0.06em;
  color: var(--ink);
}

.cb-status {
  flex-shrink: 0;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.2em;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
  color: var(--ink-soft);
}

.cb-status[data-status='COMPLETED'] {
  color: var(--ink-faint);
  border-color: var(--line);
}

.cb-status[data-status='TODAY'] {
  color: #fff;
  background: var(--accent);
  border-color: var(--accent);
}

.cb-status[data-status='UPCOMING'] {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 40%, transparent);
}

.cb-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 16px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
}

.cb-date {
  font-family: var(--mono);
  font-variant-numeric: tabular-nums;
}

/* 多活动节点 */
.cb-events {
  margin-top: 12px;
  border-top: 1px dashed var(--line);
  padding-top: 8px;
}

.cb-event-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 40px;
  padding: 6px 8px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--dur) var(--ease);
}

.cb-event-row:hover {
  background: var(--surface-alt);
}

.cb-event-type {
  flex-shrink: 0;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.18em;
  width: 64px;
  color: var(--ink-faint);
}

.cb-event-type[data-type='RELEASE'] { color: var(--t-release); }
.cb-event-type[data-type='EVENT'] { color: var(--t-event); }
.cb-event-type[data-type='TV'] { color: var(--t-tv); }
.cb-event-type[data-type='LIVE'] { color: var(--t-live); }

.cb-event-title {
  flex: 1;
  min-width: 0;
  font-family: var(--serif);
  font-size: 13.5px;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cb-event-arrow {
  font-size: 11px;
  color: var(--ink-faint);
}

.cb-event-row:hover .cb-event-arrow {
  color: var(--accent);
  transform: translateX(2px);
}

/* 移动端 */
@media (max-width: 900px) {
  .cb-hero-inner {
    padding-top: 56px;
    padding-bottom: 56px;
  }

  .cb-title {
    font-size: 40px;
  }

  .cb-countdown {
    font-size: 56px;
    padding: 20px 30px;
  }

  .cb-timeline {
    padding-top: 48px;
    padding-bottom: 80px;
  }

  .cb-rail {
    width: 26px;
  }

  .cb-card {
    padding: 14px 16px;
    margin: 6px 0 16px;
  }

  .cb-stage-name {
    font-size: 16px;
  }

  .cb-timeline-note {
    display: none;
  }
}
</style>
