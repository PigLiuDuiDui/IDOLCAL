<template>
  <section class="calendar-panel">
    <!-- 自定义工具条：月份标题 [<] [Today] [>] · Month / List -->
    <div class="cal-toolbar">
      <h2 class="cal-title">{{ currentLabel }}</h2>

      <div class="cal-controls">
        <button type="button" class="cal-btn" :aria-label="t('calendar.prevMonth')" @click="calendar?.prev()">←</button>
        <button type="button" class="cal-btn cal-today" @click="calendar?.today()">{{ t('calendar.today') }}</button>
        <button type="button" class="cal-btn" :aria-label="t('calendar.nextMonth')" @click="calendar?.next()">→</button>
      </div>

      <div class="cal-views" role="tablist" :aria-label="t('calendar.switchView')">
        <button
          type="button"
          class="cal-view-btn"
          :class="{ active: viewMode === 'month' }"
          @click="switchView('month')"
        >
          {{ t('calendar.month') }}
        </button>
        <button
          type="button"
          class="cal-view-btn"
          :class="{ active: viewMode === 'list' }"
          @click="switchView('list')"
        >
          {{ t('calendar.list') }}
        </button>
      </div>
    </div>

    <FullCalendar ref="calendarRef" :options="calendarOptions">
      <!-- 事件内容插槽：Editorial 风格（月视图微型标记 / 列表视图完整行） -->
      <template #eventContent="arg">
        <div
          v-if="arg.view?.type !== 'listMonth'"
          class="cal-event"
          :data-type="arg.event.extendedProps?.type"
        >
          <span class="cal-event-dot"></span>
          <span class="cal-event-title">{{ arg.event.title }}</span>
          <span v-if="arg.event.extendedProps?.time" class="cal-event-time">
            {{ arg.event.extendedProps.time }}
          </span>
        </div>

        <div v-else class="cal-list-event">
          <span class="cal-list-marker" :data-type="arg.event.extendedProps?.type">
            {{ TYPE_MARKER[arg.event.extendedProps?.type] || '●' }}
          </span>
          <div class="cal-list-body">
            <div class="cal-list-meta">
              <span class="cal-list-type" :data-type="arg.event.extendedProps?.type">
                {{ t(`types.${arg.event.extendedProps?.type}`) }}
              </span>
              <span v-if="arg.event.extendedProps?.time" class="cal-list-time">
                {{ arg.event.extendedProps.time }} {{ arg.event.extendedProps?.timezone }}
              </span>
              <span v-if="arg.event.extendedProps?.location" class="cal-list-loc">
                {{ text(arg.event.extendedProps.location) }}
              </span>
            </div>
            <div class="cal-list-title">{{ arg.event.title }}</div>
          </div>
          <span class="cal-list-status">{{ t(`status.${arg.event.extendedProps?.status}`) }}</span>
        </div>
      </template>
    </FullCalendar>

    <p v-if="filteredEvents.length === 0" class="cal-empty">
      {{ t('calendar.empty') }}
    </p>
  </section>
</template>

<script setup>
// 日历基础：FullCalendar（Standard 功能）
// 视觉完全重设计为 Editorial Calendar 风格，不使用默认样式
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/vue3/daygrid'
import listPlugin from '@fullcalendar/vue3/list'
import classicThemePlugin from '@fullcalendar/vue3/themes/classic'
import '@fullcalendar/vue3/skeleton.css'
import '@fullcalendar/vue3/themes/classic/theme.css'
import '@fullcalendar/vue3/themes/classic/palette.css'
import zhCnLocale from '@fullcalendar/vue3/locales/zh-cn'
import koLocale from '@fullcalendar/vue3/locales/ko'
import { eventsSorted, TYPE_MARKER } from '../data/events'
import { useUiStore } from '../stores/ui'
import { monthLabel } from '../utils/date'
import { useText, FC_LOCALES } from '../i18n'

const { t, locale } = useI18n()
const text = useText()

const MOBILE_QUERY = '(max-width: 900px)'

// FullCalendar 语言包映射（en 为内置默认，无需导入）
const FC_LOCALE_DATA = {
  'zh-cn': zhCnLocale,
  ko: koLocale
}

const ui = useUiStore()
const calendarRef = ref(null)
const currentLabel = ref('')
const viewMode = ref(window.matchMedia(MOBILE_QUERY).matches ? 'list' : 'month')

const calendar = computed(() => calendarRef.value?.getApi?.() ?? null)

// 类型筛选后的事件（数据独立，组件只做过滤与渲染）
const filteredEvents = computed(() =>
  ui.activeTypes.length === 0
    ? eventsSorted
    : eventsSorted.filter((e) => ui.activeTypes.includes(e.type))
)

// FullCalendar 事件源（含 extendedProps，供自定义渲染使用）
const fcEvents = computed(() =>
  filteredEvents.value.map((e) => ({
    id: e.id,
    title: text(e.title),
    date: e.date,
    allDay: true,
    extendedProps: {
      type: e.type,
      time: e.time,
      timezone: e.timezone,
      location: e.location,
      status: e.status
    }
  }))
)

function updateLabel() {
  const cal = calendar.value
  if (!cal) return
  const d = cal.getDate()
  currentLabel.value = monthLabel(
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  )
}

// 自定义事件渲染已改用插槽（模板内实现），此处不再需要 DOM 构建函数

function switchView(mode) {
  viewMode.value = mode
  const cal = calendar.value
  if (!cal) return
  cal.changeView(mode === 'month' ? 'dayGridMonth' : 'listMonth')
}

const calendarOptions = computed(() => ({
  plugins: [classicThemePlugin, dayGridPlugin, listPlugin],
  initialView: viewMode.value === 'month' ? 'dayGridMonth' : 'listMonth',
  locale: FC_LOCALE_DATA[FC_LOCALES[locale.value]] || undefined,
  headerToolbar: false,
  height: 'auto',
  contentHeight: 'auto',
  firstDay: locale.value === 'en' ? 0 : 1,
  dayMaxEvents: 3,
  moreLinkText: (n) => t('calendar.more', { n }),
  moreLinkClick: 'popover',
  eventDisplay: 'block',
  events: fcEvents.value,
  eventClick: (info) => {
    ui.openEvent(info.event.id)
    info.jsEvent.preventDefault()
  },
  dateClick: () => {},
  datesSet: updateLabel
}))

// 视图随窗口宽度自动切换：移动端 List / 桌面 Month
function onViewportChange(e) {
  if (e.matches && viewMode.value !== 'list') switchView('list')
  else if (!e.matches && viewMode.value !== 'month') switchView('month')
}

let mql = null
onMounted(() => {
  mql = window.matchMedia(MOBILE_QUERY)
  mql.addEventListener('change', onViewportChange)
  updateLabel()
})

onBeforeUnmount(() => {
  mql?.removeEventListener('change', onViewportChange)
})

// 筛选变化：数据通过 props 传递，FullCalendar 自动 diff 更新
watch(filteredEvents, () => {})

// 语言切换：重新设置 locale 并刷新标题
watch(locale, () => {
  const cal = calendar.value
  if (!cal) return
  cal.setOption('locale', FC_LOCALE_DATA[FC_LOCALES[locale.value]] || undefined)
  cal.setOption('firstDay', locale.value === 'en' ? 0 : 1)
  updateLabel()
})
</script>

<style scoped>
.cal-toolbar {
  display: flex;
  align-items: center;
  gap: 18px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.cal-title {
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 0.04em;
  margin-right: auto;
}

.cal-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cal-btn {
  min-height: 40px;
  min-width: 40px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--ink-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.cal-btn:hover {
  border-color: var(--ink);
  color: var(--ink);
}

.cal-today {
  min-width: 64px;
}

.cal-views {
  display: flex;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.cal-view-btn {
  min-height: 40px;
  padding: 0 16px;
  border: none;
  background: var(--surface);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--ink-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.cal-view-btn + .cal-view-btn {
  border-left: 1px solid var(--line);
}

/* Active 状态使用强调色 */
.cal-view-btn.active {
  background: var(--ink);
  color: #fff;
}

.cal-empty {
  padding: 40px 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: var(--ink-faint);
  border-top: 1px solid var(--line);
}

@media (max-width: 640px) {
  .cal-toolbar {
    gap: 10px;
  }

  .cal-title {
    font-size: 19px;
  }
}
</style>

<!-- FullCalendar 深度样式覆盖：完全重设计，不使用默认视觉 -->
<style>
/* ---------- 月视图格子 ---------- */
.calendar-panel .fc {
  --fc-border-color: var(--line);
  --fc-today-bg-color: transparent;
  --fc-neutral-bg-color: transparent;
  --fc-page-bg-color: transparent;
  font-family: var(--sans);
  font-size: 13px;
  color: var(--ink);
}

.calendar-panel .fc .fc-scrollgrid {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
}

.calendar-panel .fc .fc-col-header-cell {
  padding: 12px 0 10px;
  border: none;
  border-bottom: 1px solid var(--line);
  background: transparent;
}

.calendar-panel .fc .fc-col-header-cell-cushion {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.26em;
  color: var(--ink-faint);
  text-transform: uppercase;
  padding: 0;
  text-decoration: none;
}

.calendar-panel .fc .fc-daygrid-day {
  padding: 0;
}

.calendar-panel .fc .fc-daygrid-day-frame {
  min-height: 96px;
  background: var(--surface);
  transition: background var(--dur) var(--ease);
}

.calendar-panel .fc .fc-daygrid-day:hover .fc-daygrid-day-frame {
  background: var(--surface-alt);
}

/* 日期数字：轻量，不粗体 */
.calendar-panel .fc .fc-daygrid-day-number {
  font-size: 12px;
  font-weight: 400;
  color: var(--ink-soft);
  padding: 8px 10px 2px;
  text-decoration: none;
  font-variant-numeric: tabular-nums;
}

/* 非当月日期：淡出 */
.calendar-panel .fc .fc-day-other .fc-daygrid-day-number {
  color: var(--ink-faint);
  opacity: 0.5;
}

.calendar-panel .fc .fc-day-other .fc-daygrid-day-frame {
  background: transparent;
}

/* 今天：非常克制的高亮（细线 + 圆点） */
.calendar-panel .fc .fc-day-today .fc-daygrid-day-number {
  color: var(--accent);
  font-weight: 700;
}

.calendar-panel .fc .fc-day-today .fc-daygrid-day-frame {
  background: var(--accent-soft);
}

/* 有活动的日期：小型标记圆点 */
.calendar-panel .fc .fc-daygrid-day.has-events .fc-daygrid-day-number::after {
  content: '';
  display: block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--accent);
  margin: 3px auto 0;
}

/* ---------- 格子内事件：微型 Editorial 标记 ---------- */
.calendar-panel .fc .fc-daygrid-day-events {
  padding: 2px 6px 6px;
  margin-top: 0;
}

.calendar-panel .fc .fc-daygrid-event {
  border: none;
  background: transparent;
  border-radius: 4px;
  margin: 1px 0;
  padding: 2px 4px;
  cursor: pointer;
  transition: background var(--dur) var(--ease);
}

.calendar-panel .fc .fc-daygrid-event:hover {
  background: var(--surface-alt);
}

.cal-event {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 11px;
  line-height: 1.4;
  color: var(--ink);
}

.cal-event-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--ink-faint);
}

.cal-event[data-type='RELEASE'] .cal-event-dot { background: var(--t-release); }
.cal-event[data-type='EVENT'] .cal-event-dot { background: var(--t-event); }
.cal-event[data-type='TV'] .cal-event-dot { background: var(--t-tv); }
.cal-event[data-type='LIVE'] .cal-event-dot { background: var(--t-live); }
.cal-event[data-type='PHOTO'] .cal-event-dot { background: var(--t-photo); }
.cal-event[data-type='MAGAZINE'] .cal-event-dot { background: var(--t-magazine); }
.cal-event[data-type='OFFLINE'] .cal-event-dot { background: var(--t-offline); }
.cal-event[data-type='BRAND'] .cal-event-dot { background: var(--t-brand); }
.cal-event[data-type='BIRTHDAY'] .cal-event-dot { background: var(--t-birthday); }

.cal-event-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.cal-event-time {
  margin-left: auto;
  font-size: 9px;
  color: var(--ink-faint);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

/* ---------- +N more ---------- */
.calendar-panel .fc .fc-daygrid-more-link {
  display: block;
  margin: 2px 6px;
  padding: 3px 6px;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
  text-decoration: none;
  cursor: pointer;
}

.calendar-panel .fc .fc-daygrid-more-link:hover {
  color: var(--accent);
}

/* more popover：Editorial 卡片 */
.calendar-panel .fc .fc-popover {
  border: 1px solid var(--line-strong);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: 0 18px 48px rgba(25, 25, 25, 0.12);
  font-family: var(--sans);
  z-index: 60;
}

.calendar-panel .fc .fc-popover-header {
  background: var(--surface-alt);
  padding: 10px 14px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
  color: var(--ink-soft);
}

.calendar-panel .fc .fc-popover-header .fc-popover-title {
  font-size: inherit;
  font-weight: inherit;
}

.calendar-panel .fc .fc-popover-close {
  font-size: 16px;
  color: var(--ink-faint);
  opacity: 1;
}

.calendar-panel .fc .fc-popover-body {
  padding: 6px;
}

/* ---------- List 视图：Editorial 列表 ---------- */
.calendar-panel .fc .fc-list {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
}

.calendar-panel .fc .fc-list-day-cushion {
  background: var(--surface-alt);
  padding: 10px 16px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.calendar-panel .fc .fc-list-day-side {
  font-size: 10px;
  color: var(--ink-faint);
}

.calendar-panel .fc .fc-list-event {
  background: var(--surface);
  border: none;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}

.calendar-panel .fc .fc-list-event:hover td {
  background: var(--surface-alt);
}

.calendar-panel .fc .fc-list-event td {
  padding: 12px 16px;
}

.calendar-panel .fc .fc-list-event-dot {
  display: none;
}

.cal-list-event {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  text-align: left;
}

.cal-list-marker {
  font-size: 12px;
  flex-shrink: 0;
  width: 18px;
  text-align: center;
}

.cal-list-marker[data-type='RELEASE'] { color: var(--t-release); }
.cal-list-marker[data-type='EVENT'] { color: var(--t-event); }
.cal-list-marker[data-type='TV'] { color: var(--t-tv); }
.cal-list-marker[data-type='LIVE'] { color: var(--t-live); }
.cal-list-marker[data-type='PHOTO'] { color: var(--t-photo); }
.cal-list-marker[data-type='MAGAZINE'] { color: var(--t-magazine); }
.cal-list-marker[data-type='OFFLINE'] { color: var(--t-offline); }
.cal-list-marker[data-type='BRAND'] { color: var(--t-brand); }
.cal-list-marker[data-type='BIRTHDAY'] { color: var(--t-birthday); }

.cal-list-body {
  flex: 1;
  min-width: 0;
}

.cal-list-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  align-items: center;
  margin-bottom: 2px;
}

.cal-list-type {
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.22em;
}

.cal-list-type[data-type='RELEASE'] { color: var(--t-release); }
.cal-list-type[data-type='EVENT'] { color: var(--t-event); }
.cal-list-type[data-type='TV'] { color: var(--t-tv); }
.cal-list-type[data-type='LIVE'] { color: var(--t-live); }
.cal-list-type[data-type='PHOTO'] { color: var(--t-photo); }
.cal-list-type[data-type='MAGAZINE'] { color: var(--t-magazine); }
.cal-list-type[data-type='OFFLINE'] { color: var(--t-offline); }
.cal-list-type[data-type='BRAND'] { color: var(--t-brand); }
.cal-list-type[data-type='BIRTHDAY'] { color: var(--t-birthday); }

.cal-list-time,
.cal-list-loc {
  font-size: 10px;
  color: var(--ink-faint);
  letter-spacing: 0.08em;
}

.cal-list-title {
  font-family: var(--serif);
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cal-list-status {
  flex-shrink: 0;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--ink-faint);
}

/* 移动端：格子高度更紧凑 */
@media (max-width: 640px) {
  .calendar-panel .fc .fc-daygrid-day-frame {
    min-height: 64px;
  }

  .calendar-panel .fc .fc-daygrid-day-number {
    font-size: 11px;
    padding: 6px 8px 2px;
  }

  .calendar-panel .fc .fc-daygrid-day-events {
    padding: 2px 3px 4px;
  }

  .calendar-panel .fc .fc-event-title {
    font-size: 10px;
  }
}
</style>
