<template>
  <article class="day-card" :class="{ 'has-reminder': hasReminder }" @click="ui.openEvent(event.id)">
    <!-- 时间列（本地时间为主，官方时间为辅） -->
    <div class="dc-time">
      <template v-if="time.local">
        <span class="dc-time-local">{{ time.local.time }} <em>{{ time.local.tz }}</em></span>
        <span v-if="time.dayShift !== 0" class="dc-time-shift" :data-dir="time.dayShift > 0 ? 'plus' : 'minus'">
          {{ time.dayShift > 0 ? `+${time.dayShift}D` : `${time.dayShift}D` }}
        </span>
        <span v-if="!isSameTime" class="dc-time-official">{{ time.official.time }} {{ time.official.tz }}</span>
      </template>
      <span v-else class="dc-time-allday">{{ t('today.allDay') }}</span>
    </div>

    <!-- 内容 -->
    <div class="dc-body">
      <div class="dc-meta">
        <span class="type-marker" :data-type="event.type" :data-marker="data.TYPE_MARKER[event.type]">
          {{ t(`types.${event.type}`) }}
        </span>
        <span v-if="event.sourceName" class="dc-source">{{ event.sourceName }}</span>
        <span class="dc-artist">{{ artistName }}</span>
      </div>
      <h3 class="dc-title">{{ text(event.title) }}</h3>
    </div>

    <!-- 快速提醒 + 跳转 -->
    <div class="dc-side">
      <button
        v-if="canRemind"
        type="button"
        class="dc-remind"
        :class="{ active: hasReminder }"
        :aria-label="t('reminder.setBtn')"
        :title="hasReminder ? t('reminder.setBtnDone') : t('reminder.setBtn')"
        @click.stop="reminderOpen = true"
      >
        <svg
          viewBox="0 0 24 24"
          width="15"
          height="15"
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
        <span v-if="hasReminder" class="dc-remind-dot" aria-hidden="true"></span>
      </button>
      <span class="dc-arrow" aria-hidden="true">→</span>
    </div>

    <ReminderPanel :open="reminderOpen" :event="event" @close="reminderOpen = false" />
  </article>
</template>

<script setup>
// Today / This Week 视图的日程行卡片：
// 本地时间突出 + 官方时间参考 + 艺人/来源 + 快速设置提醒
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { useRemindersStore } from '../stores/reminders'
import { useTimezoneStore } from '../stores/timezone'
import { localizeEvent, getEventStart, nowInstant } from '../utils/time'
import { useText } from '../i18n'
import ReminderPanel from './ReminderPanel.vue'

const props = defineProps({
  event: { type: Object, required: true }
})

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const reminders = useRemindersStore()
const timezone = useTimezoneStore()
const data = useDataStore()

const reminderOpen = ref(false)

const time = computed(() => localizeEvent(props.event, timezone.displayZone))

const isSameTime = computed(
  () => time.value.local && time.value.official && time.value.local.time === time.value.official.time && time.value.local.tz === time.value.official.tz
)

const artistName = computed(() => {
  const artist = data.getArtist(props.event.artist)
  return artist ? artist.name : props.event.artist
})

const hasReminder = computed(() => reminders.hasReminder(props.event.id))

// 有明确开始时间且未开始 → 可设置提醒
const canRemind = computed(() => {
  const start = getEventStart(props.event)
  if (!start) return false
  return Number(start.toInstant().epochMilliseconds) >= Number(nowInstant().epochMilliseconds)
})
</script>

<style scoped>
.day-card {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  cursor: pointer;
  overflow: hidden;
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
  text-align: left;
}

.day-card:hover {
  border-color: var(--line-strong);
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(25, 25, 25, 0.06);
}

.day-card.has-reminder {
  border-color: color-mix(in srgb, var(--accent) 32%, var(--line));
}

/* 时间列 */
.dc-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 96px;
  padding: 18px 14px;
  border-right: 1px solid var(--line);
  background: var(--surface-alt);
}

.dc-time-local {
  font-family: var(--mono);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.dc-time-local em {
  font-style: normal;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--ink-soft);
  margin-left: 3px;
}

.dc-time-shift {
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 999px;
}

.dc-time-shift[data-dir='plus'] {
  color: var(--accent);
  background: var(--accent-soft);
}

.dc-time-shift[data-dir='minus'] {
  color: var(--ink-faint);
  background: var(--surface);
}

.dc-time-official {
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.dc-time-allday {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: var(--ink-faint);
}

/* 内容 */
.dc-body {
  flex: 1;
  min-width: 0;
  padding: 16px 18px;
}

.dc-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-bottom: 6px;
}

.dc-source {
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--ink-faint);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dc-artist {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--ink-soft);
}

.dc-title {
  font-family: var(--serif);
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0.03em;
  line-height: 1.35;
  overflow-wrap: break-word;
}

/* 右侧操作 */
.dc-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 16px 14px 16px 6px;
}

.dc-remind {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--line-strong);
  border-radius: 50%;
  background: var(--surface);
  color: var(--ink-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  position: relative;
}

.dc-remind:hover {
  border-color: var(--accent);
  color: var(--accent);
  transform: translateY(-1px);
}

.dc-remind.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.dc-remind-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--accent-ink);
  border: 2px solid var(--surface);
}

.dc-arrow {
  font-size: 12px;
  color: var(--ink-faint);
  transition: transform var(--dur) var(--ease), color var(--dur) var(--ease);
}

.day-card:hover .dc-arrow {
  transform: translateX(3px);
  color: var(--accent);
}

/* 移动端 */
@media (max-width: 640px) {
  .dc-time {
    min-width: 78px;
    padding: 14px 10px;
  }

  .dc-time-local {
    font-size: 16px;
  }

  .dc-time-official {
    display: none;
  }

  .dc-body {
    padding: 12px 12px;
  }

  .dc-title {
    font-size: 14.5px;
  }

  .dc-source {
    max-width: 110px;
  }

  .dc-side {
    padding: 12px 10px 12px 2px;
  }
}
</style>
