<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="ui.drawerOpen" class="drawer-backdrop" @click="ui.closeEvent()"></div>
    </Transition>

    <Transition name="drawer">
      <aside
        v-if="ui.drawerOpen && event"
        class="detail-panel"
        role="dialog"
        aria-modal="true"
        :aria-label="t('drawer.label')"
      >
        <header class="panel-head">
          <div class="panel-head-meta">
            <span class="type-marker" :data-type="event.type" :data-marker="data.TYPE_MARKER[event.type]">
              {{ t(`types.${event.type}`) }}
            </span>
            <span class="status-tag" :data-status="event.status">{{ t(`status.${event.status}`) }}</span>
          </div>
          <button type="button" class="panel-close" :aria-label="t('drawer.close')" @click="ui.closeEvent()">✕</button>
        </header>

        <div class="panel-scroll">
          <!-- 视觉占位（预留图片区域，避免图片墙） -->
          <div class="panel-visual" :data-type="event.type">
            <span class="visual-mark" aria-hidden="true">{{ data.TYPE_MARKER[event.type] }}</span>
            <span class="visual-type">{{ t(`types.${event.type}`) }}</span>
          </div>

          <h2 class="panel-title">{{ text(event.title) }}</h2>

          <dl class="panel-facts">
            <div class="fact">
              <dt>{{ t('drawer.date') }}</dt>
              <dd>{{ editorialDate(event.date) }}<span v-if="event.endDate"> — {{ editorialDate(event.endDate) }}</span></dd>
            </div>
            <div v-if="event.time" class="fact">
              <dt>{{ t('drawer.time') }}</dt>
              <dd>
                <EventTime :event="event" inline />
              </dd>
            </div>
            <div v-if="event.location" class="fact">
              <dt>{{ t('drawer.location') }}</dt>
              <dd>{{ text(event.location) }}</dd>
            </div>
            <div class="fact">
              <dt>{{ t('drawer.countdown') }}</dt>
              <dd :class="{ 'is-past': isPast }">{{ countdownLabel(event.date) }}</dd>
            </div>
          </dl>

          <p v-if="event.description" class="panel-desc">{{ text(event.description) }}</p>

          <!-- 官方来源：克制区域 -->
          <section class="panel-source">
            <div class="source-head">
              <span class="eyebrow">{{ t('drawer.officialSource') }}</span>
              <span class="source-level" :data-level="sourceLevel">{{ t(`sourceLevels.${sourceLevelKey}`) }}</span>
            </div>

            <div class="source-body">
              <div class="source-name">{{ event.sourceName }}</div>
              <div class="source-published">{{ t('drawer.published', { date: fullDate(event.date) }) }}</div>
              <a
                v-if="event.sourceUrl"
                :href="event.sourceUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="source-link"
              >
                {{ t('drawer.viewOriginal') }}
              </a>
            </div>
          </section>
        </div>

        <!-- 底部操作：根据事件类型动态出现 -->
        <footer class="panel-actions">
          <button
            v-if="canRemind"
            type="button"
            class="btn"
            :class="hasReminder ? 'btn-primary' : 'btn-accent'"
            @click="reminderOpen = true"
          >
            <svg
              class="btn-bell"
              viewBox="0 0 24 24"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.7 21a2 2 0 0 1-3.4 0" />
            </svg>
            {{ hasReminder ? t('reminder.setBtnDone') : t('reminder.setBtn') }}
          </button>
          <template v-if="event.isOfficial && event.sourceUrl">
            <a
              :href="event.sourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="btn btn-primary"
            >
              {{ t('drawer.viewOfficialSource') }}
            </a>
          </template>
          <template v-else-if="event.sourceUrl">
            <a
              :href="event.sourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="btn"
            >
              {{ t('drawer.viewSource') }}
            </a>
          </template>

          <a v-if="event.onlineUrl" :href="event.onlineUrl" target="_blank" rel="noopener noreferrer" class="btn btn-accent">
            {{ isOnline ? t('drawer.watchLive') : t('drawer.joinOnline') }}
          </a>

          <a v-if="event.mapUrl" :href="event.mapUrl" target="_blank" rel="noopener noreferrer" class="btn">
            {{ t('drawer.viewLocation') }}
          </a>

          <a v-if="event.mapUrl" :href="event.mapUrl" target="_blank" rel="noopener noreferrer" class="btn">
            {{ t('drawer.navigate') }}
          </a>

          <button type="button" class="btn btn-ghost" @click="downloadIcs">{{ t('drawer.addToCalendar') }}</button>
        </footer>
      </aside>
    </Transition>

    <ReminderPanel :open="reminderOpen" :event="event" @close="reminderOpen = false" />
  </Teleport>
</template>

<script setup>
// 活动详情面板：桌面右侧滑出 420~520px，移动端底部 Bottom Sheet
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { useRemindersStore } from '../stores/reminders'
import { editorialDate, fullDate, countdownLabel, todayKey } from '../utils/date'
import { nowInstant, getEventStart } from '../utils/time'
import { useText } from '../i18n'
import EventTime from './EventTime.vue'
import ReminderPanel from './ReminderPanel.vue'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const reminders = useRemindersStore()
const data = useDataStore()

// 提醒设置弹窗开关
const reminderOpen = ref(false)

// 是否有明确开始时间（可设置提醒）
const canRemind = computed(() => {
  if (!event.value?.time) return false
  const start = getEventStart(event.value)
  if (!start) return false
  return Number(start.toInstant().epochMilliseconds) >= Number(nowInstant().epochMilliseconds)
})

const hasReminder = computed(() => (event.value ? reminders.hasReminder(event.value.id) : false))

const event = computed(() => {
  if (!ui.selectedEventId) return null
  return data.getEventById(ui.selectedEventId) || null
})

const isPast = computed(() => (event.value ? event.value.date < todayKey() : false))

// 在线类判断：基于英文原始地点字段（关键词匹配稳定）
const isOnline = computed(() => {
  if (!event.value) return false
  const loc = (event.value.location?.en || event.value.location || '').toLowerCase()
  return /online|youtube|weverse|live/i.test(loc)
})

// 来源可信度：Official 最高优先级，明确区分 Fan Project / Media / Brand（基于英文 sourceName 判断）
const sourceLevel = computed(() => {
  if (!event.value) return 'MEDIA'
  const e = event.value
  if (e.isOfficial) return 'OFFICIAL'
  const name = (e.sourceName || '').toLowerCase()
  if (/fan|community/i.test(name)) return 'FAN PROJECT'
  if (/lumen|brand/i.test(name)) return 'BRAND'
  return 'MEDIA'
})

// 来源等级 i18n key（FAN PROJECT -> sourceLevels.FAN）
const sourceLevelKey = computed(() =>
  sourceLevel.value === 'FAN PROJECT' ? 'FAN' : sourceLevel.value
)

// 加入我的日历：生成本地 .ics 文件（标题 / 地点 / 描述使用当前语言）
function downloadIcs() {
  if (!event.value) return
  const e = event.value
  const pad = (s) => s.replaceAll('-', '')
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//IdolCal//EVAN Official Schedule//EN',
    'BEGIN:VEVENT',
    `UID:${e.id}@idolcal`,
    `DTSTART;VALUE=DATE:${pad(e.date)}`,
    ...(e.endDate ? [`DTEND;VALUE=DATE:${pad(e.endDate)}`] : []),
    `SUMMARY:${text(e.title)}`,
    ...(e.location ? [`LOCATION:${text(e.location)}`] : []),
    ...(e.time ? [`DESCRIPTION:${e.time} ${e.timezone} KST`] : []),
    'END:VEVENT',
    'END:VCALENDAR'
  ]
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `EVAN-${e.id}.ics`
  a.click()
  URL.revokeObjectURL(url)
}

// Esc 关闭 + 锁定背景滚动
function onKeydown(e) {
  if (e.key === 'Escape') ui.closeEvent()
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})

const bodyLocked = computed(() => ui.drawerOpen)
watch(bodyLocked, (locked) => {
  document.body.style.overflow = locked ? 'hidden' : ''
})
</script>

<style scoped>
/* 遮罩 */
.drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(19, 19, 19, 0.42);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

/* 面板：桌面端从右侧滑出 */
.detail-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  width: min(520px, 92vw);
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-left: 1px solid var(--line);
  box-shadow: -24px 0 60px rgba(19, 19, 19, 0.14);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 24px;
  border-bottom: 1px solid var(--line);
}

.panel-head-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-close {
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--ink-faint);
  cursor: pointer;
  border-radius: 50%;
  transition: all var(--dur) var(--ease);
}

.panel-close:hover {
  color: var(--ink);
  background: var(--surface-alt);
}

.panel-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 28px 24px 32px;
}

/* 视觉占位（预留官方图片位置） */
.panel-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 168px;
  margin-bottom: 24px;
  border-radius: var(--radius);
  background: var(--surface-alt);
  border: 1px solid var(--line);
}

.panel-visual[data-type='RELEASE'] { background: color-mix(in srgb, var(--t-release) 8%, var(--surface)); }
.panel-visual[data-type='EVENT'] { background: color-mix(in srgb, var(--t-event) 8%, var(--surface)); }
.panel-visual[data-type='TV'] { background: color-mix(in srgb, var(--t-tv) 8%, var(--surface)); }
.panel-visual[data-type='LIVE'] { background: color-mix(in srgb, var(--t-live) 8%, var(--surface)); }
.panel-visual[data-type='PHOTO'] { background: color-mix(in srgb, var(--t-photo) 8%, var(--surface)); }
.panel-visual[data-type='MAGAZINE'] { background: color-mix(in srgb, var(--t-magazine) 8%, var(--surface)); }
.panel-visual[data-type='OFFLINE'] { background: color-mix(in srgb, var(--t-offline) 8%, var(--surface)); }
.panel-visual[data-type='BRAND'] { background: color-mix(in srgb, var(--t-brand) 8%, var(--surface)); }
.panel-visual[data-type='BIRTHDAY'] { background: color-mix(in srgb, var(--t-birthday) 8%, var(--surface)); }

.visual-mark {
  font-family: var(--serif);
  font-size: 52px;
  line-height: 1;
  color: var(--ink-faint);
  opacity: 0.5;
}

.visual-type {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.3em;
  color: var(--ink-faint);
}

.panel-title {
  font-family: var(--serif);
  font-size: 28px;
  font-weight: 400;
  letter-spacing: 0.03em;
  line-height: 1.25;
  margin-bottom: 24px;
}

/* 事实清单：杂志式信息表格 */
.panel-facts {
  border-top: 1px solid var(--line);
  margin-bottom: 24px;
}

.fact {
  display: flex;
  gap: 16px;
  padding: 11px 0;
  border-bottom: 1px solid var(--line);
}

.fact dt {
  flex-shrink: 0;
  width: 92px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: var(--ink-faint);
  padding-top: 2px;
}

.fact dd {
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.fact dd.is-past {
  color: var(--ink-faint);
}

.panel-desc {
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-soft);
  margin-bottom: 28px;
}

/* 官方来源：克制区域 */
.panel-source {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 20px 22px;
  background: var(--surface-alt);
}

.source-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.source-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.source-name {
  font-family: var(--serif);
  font-size: 17px;
  letter-spacing: 0.04em;
}

.source-published {
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--ink-faint);
  margin-bottom: 10px;
}

.source-link {
  align-self: flex-start;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--ink);
  text-decoration: none;
  border-bottom: 1px solid var(--line-strong);
  transition: all var(--dur) var(--ease);
  padding-bottom: 2px;
}

.source-link:hover {
  color: var(--accent);
  border-color: var(--accent);
}

/* 底部操作区 */
.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--line);
  background: var(--surface);
}

.panel-actions .btn {
  flex: 1 1 auto;
  min-width: 0;
}

.btn-bell {
  flex-shrink: 0;
}

/* ---------- 移动端：Bottom Sheet ---------- */
@media (max-width: 900px) {
  .detail-panel {
    top: auto;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    max-height: 88vh;
    border-left: none;
    border-top: 1px solid var(--line);
    border-radius: 16px 16px 0 0;
    box-shadow: 0 -18px 48px rgba(19, 19, 19, 0.16);
  }

  .panel-head {
    padding: 14px 18px;
  }

  .panel-scroll {
    padding: 20px 18px 24px;
  }

  .panel-visual {
    height: 132px;
  }

  .panel-title {
    font-size: 23px;
  }

  .panel-actions {
    padding: 14px 18px;
    padding-bottom: calc(14px + env(safe-area-inset-bottom));
  }
}

/* ---------- 过渡动画 ---------- */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s var(--ease);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.38s var(--ease);
}

/* 桌面：右侧滑入 */
@media (min-width: 901px) {
  .drawer-enter-from,
  .drawer-leave-to {
    transform: translateX(100%);
  }
}

/* 移动：底部滑入 */
@media (max-width: 900px) {
  .drawer-enter-from,
  .drawer-leave-to {
    transform: translateY(100%);
  }
}
</style>
