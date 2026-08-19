<template>
  <div class="reminders-view container">
    <header class="page-head">
      <p class="eyebrow">{{ t('reminders.eyebrow') }}</p>
      <h1 class="page-title">REMINDERS</h1>
      <p class="page-sub">{{ t('reminders.sub') }}</p>
    </header>

    <!-- 提醒列表 -->
    <section v-if="list.length" class="reminders-list">
      <div v-for="item in list" :key="item.eventId" class="reminder-row" :data-state="stateOf(item)">
        <span class="reminder-bell" aria-hidden="true">
          <svg
            viewBox="0 0 24 24"
            width="15"
            height="15"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
          </svg>
        </span>

        <button type="button" class="reminder-main" @click="ui.openEvent(item.event.id)">
          <div class="reminder-meta">
            <span class="type-marker" :data-type="item.event.type" :data-marker="data.TYPE_MARKER[item.event.type]">
              {{ t(`types.${item.event.type}`) }}
            </span>
            <span class="reminder-date">{{ fullDate(item.event.date) }}</span>
            <span class="reminder-time">
              <EventTime :event="item.event" inline />
            </span>
          </div>
          <h3 class="reminder-title">{{ text(item.event.title) }}</h3>
          <p class="reminder-at">
            {{ t('reminders.alertAt') }}
            <strong>{{ offsetLabel(item.offset) }}</strong>
            · {{ atText(item) }}
          </p>
        </button>

        <button type="button" class="reminder-cancel" :aria-label="t('reminders.cancel')" @click="cancel(item.eventId)">
          {{ t('reminders.cancel') }}
        </button>
      </div>
    </section>

    <!-- 空状态 -->
    <section v-else class="reminders-empty">
      <span class="reminders-empty-bell" aria-hidden="true">♢</span>
      <h2 class="reminders-empty-title">{{ t('reminders.emptyTitle') }}</h2>
      <p class="reminders-empty-desc">{{ t('reminders.emptyDesc') }}</p>
      <RouterLink to="/" class="btn">{{ t('reminders.browse') }}</RouterLink>
    </section>

    <p class="reminders-note">{{ t('reminders.note') }}</p>
  </div>
</template>

<script setup>
// 提醒列表：集中查看所有已设置的活动提醒（浏览器本地存储，
// 为未来 PWA / Web Push 通知预留，本次先完成前端状态管理）
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { useRemindersStore } from '../stores/reminders'
import { useTimezoneStore } from '../stores/timezone'
import { formatReminderAt, getEventStart, getReminderInstant, nowInstant } from '../utils/time'
import { fullDate } from '../utils/date'
import { useText } from '../i18n'
import EventTime from '../components/EventTime.vue'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const reminders = useRemindersStore()
const timezone = useTimezoneStore()
const data = useDataStore()

const list = computed(() => reminders.list)

function offsetLabel(offsetId) {
  return t(`reminder.options.${offsetId}`)
}

function atText(item) {
  const at = formatReminderAt(item.event, item.offset, timezone.displayZone)
  if (!at) return ''
  const [m, d] = at.date.split('-').slice(1)
  return `${m}.${d} · ${at.time} ${at.tz}`
}

// 状态：upcoming 即将提醒 / done 提醒时刻已过 / passed 活动已开始
function stateOf(item) {
  const now = Number(nowInstant().epochMilliseconds)
  const start = getEventStart(item.event)
  if (start && Number(start.toInstant().epochMilliseconds) <= now) return 'passed'
  const instant = getReminderInstant(item.event, item.offset)
  if (instant && Number(instant.epochMilliseconds) <= now) return 'done'
  return 'upcoming'
}

function cancel(eventId) {
  reminders.cancelReminder(eventId)
}
</script>

<style scoped>
.reminders-view {
  padding-top: 72px;
  padding-bottom: 120px;
  max-width: 820px;
}

.page-head {
  margin-bottom: 48px;
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

/* 提醒行 */
.reminders-list {
  display: flex;
  flex-direction: column;
}

.reminder-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 8px;
  border-bottom: 1px solid var(--line);
}

.reminder-row[data-state='passed'] {
  opacity: 0.5;
}

.reminder-bell {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid color-mix(in srgb, var(--accent) 26%, transparent);
}

.reminder-main {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  padding: 4px 0;
}

.reminder-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 14px;
  margin-bottom: 4px;
}

.reminder-date {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--ink-faint);
  text-transform: uppercase;
}

.reminder-time {
  font-size: 10px;
  color: var(--ink-soft);
}

.reminder-title {
  font-family: var(--serif);
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0.03em;
  line-height: 1.35;
  margin-bottom: 4px;
  overflow-wrap: break-word;
}

.reminder-at {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
}

.reminder-at strong {
  color: var(--accent-ink);
  font-weight: 700;
}

.reminder-cancel {
  flex-shrink: 0;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--ink-faint);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.reminder-cancel:hover {
  border-color: var(--accent);
  color: var(--accent);
}

/* 空状态 */
.reminders-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  padding: 72px 24px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius);
}

.reminders-empty-bell {
  font-size: 30px;
  color: var(--ink-faint);
  margin-bottom: 6px;
}

.reminders-empty-title {
  font-family: var(--serif);
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

.reminders-empty-desc {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
  max-width: 380px;
  line-height: 1.8;
  margin-bottom: 10px;
}

.reminders-note {
  margin-top: 28px;
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--ink-faint);
  line-height: 1.9;
}

/* 移动端 */
@media (max-width: 640px) {
  .reminders-view {
    padding-top: 48px;
    padding-bottom: 80px;
  }

  .page-head {
    margin-bottom: 36px;
  }

  .reminder-row {
    padding: 14px 4px;
  }

  .reminder-bell {
    width: 34px;
    height: 34px;
  }

  .reminder-title {
    font-size: 15px;
  }

  .reminder-cancel {
    padding: 0 10px;
  }
}
</style>
