<template>
  <Teleport to="body">
    <Transition name="reminder">
      <div v-if="open && event" class="reminder-overlay" @click.self="close">
        <div class="reminder-panel" role="dialog" aria-modal="true" :aria-label="t('reminder.title')">
          <button class="reminder-close" type="button" :aria-label="t('reminder.close')" @click="close">✕</button>

          <div class="reminder-head">
            <span class="reminder-bell" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.7 21a2 2 0 0 1-3.4 0" />
              </svg>
            </span>
            <p class="eyebrow reminder-eyebrow">{{ t('reminder.eyebrow') }}</p>
            <h2 class="reminder-title">{{ t('reminder.title') }}</h2>
            <p class="reminder-event">{{ text(event.title) }}</p>
            <p class="reminder-meta">
              <EventTime :event="event" inline />
            </p>
          </div>

          <!-- 无明确开始时间：不提供提醒 -->
          <p v-if="!event.time" class="reminder-na">{{ t('reminder.noTime') }}</p>

          <template v-else>
            <div class="reminder-options" role="radiogroup" :aria-label="t('reminder.title')">
              <button
                v-for="opt in options"
                :key="opt.id"
                type="button"
                role="radio"
                :aria-checked="selected === opt.id"
                class="reminder-option"
                :class="{ active: selected === opt.id, disabled: opt.past }"
                :disabled="opt.past"
                @click="choose(opt.id)"
              >
                <span class="ro-radio" aria-hidden="true"></span>
                <span class="ro-label">{{ t(`reminder.options.${opt.id}`) }}</span>
                <span class="ro-at">{{ t('reminder.at') }} {{ opt.atText }}</span>
              </button>

              <!-- 自定义偏移：输入分钟/小时/天 -->
              <button
                type="button"
                role="radio"
                :aria-checked="selected === 'custom'"
                class="reminder-option"
                :class="{ active: selected === 'custom', disabled: customPast }"
                :disabled="customPast"
                @click="choose('custom')"
              >
                <span class="ro-radio" aria-hidden="true"></span>
                <span class="ro-label">{{ t('reminder.options.custom') }}</span>
                <span v-if="customAtText" class="ro-at">{{ t('reminder.at') }} {{ customAtText }}</span>
              </button>

              <div v-if="selected === 'custom'" class="reminder-custom">
                <input
                  v-model="customNum"
                  class="reminder-custom-num"
                  type="number"
                  min="1"
                  :max="customMax"
                  step="1"
                  inputmode="numeric"
                  :aria-label="t('reminder.options.custom')"
                />
                <select v-model="customUnit" class="reminder-custom-unit" :aria-label="t('reminder.units.hour')">
                  <option value="minute">{{ t('reminder.units.minute') }}</option>
                  <option value="hour">{{ t('reminder.units.hour') }}</option>
                  <option value="day">{{ t('reminder.units.day') }}</option>
                </select>
                <p class="reminder-custom-hint">{{ t('reminder.customHint') }}</p>
              </div>
            </div>

            <div class="reminder-foot">
              <p v-if="current" class="reminder-set">
                {{ t('reminder.setAt', { at: currentAtText }) }}
              </p>
              <div class="reminder-actions">
                <button type="button" class="btn btn-accent" :disabled="!canConfirm" @click="confirm">
                  {{ current ? t('reminder.update') : t('reminder.confirm') }}
                </button>
                <button v-if="current" type="button" class="btn btn-ghost" @click="cancel">
                  {{ t('reminder.cancel') }}
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
// 提醒设置弹窗：选择提前量（1天前 / 3小时前 / 1小时前 / 30分钟前 / 开始时 / 自定义）
// 提醒时刻基于活动官方时区计算，再转换到用户显示时区展示
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  REMINDER_OFFSETS,
  getReminderInstant,
  formatReminderAt,
  nowInstant,
  minutesToParts,
  CUSTOM_OFFSET_MIN,
  CUSTOM_OFFSET_MAX
} from '../utils/time.js'
import { useRemindersStore } from '../stores/reminders'
import { useTimezoneStore } from '../stores/timezone'
import { useText } from '../i18n'
import { shortDate } from '../utils/date'
import EventTime from './EventTime.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  event: { type: Object, default: null }
})
const emit = defineEmits(['close'])

const { t } = useI18n()
const text = useText()
const reminders = useRemindersStore()
const timezone = useTimezoneStore()

const selected = ref(null)
const confirmed = ref(false)
// 自定义偏移输入：数值 + 单位（分钟 / 小时 / 天）
const customNum = ref('1')
const customUnit = ref('hour')

// 已设置的提醒配置
const current = computed(() => (props.event ? reminders.reminderOf(props.event.id) : null))

// 打开时同步选中值（含自定义输入回填）
watch(
  () => props.open,
  (open) => {
    if (open) {
      selected.value = current.value?.offset || null
      confirmed.value = false
      if (current.value?.offset === 'custom') {
        const parts = minutesToParts(current.value.offsetMinutes)
        if (parts) {
          customNum.value = String(parts.value)
          customUnit.value = parts.unit
        }
      }
    }
  }
)

// 预设选项（排除 custom，custom 单独渲染）：附带本地化后的提醒时刻（已过去的选项置灰）
const options = computed(() => {
  if (!props.event?.time) return []
  const now = nowInstant()
  return REMINDER_OFFSETS.filter((o) => !o.custom).map((o) => {
    const instant = getReminderInstant(props.event, o.id)
    const at = formatReminderAt(props.event, o.id, timezone.displayZone)
    return {
      ...o,
      past: Boolean(instant && Number(instant.epochMilliseconds) < Number(now.epochMilliseconds)),
      atText: at ? `${shortDate(at.date)} · ${at.time} ${at.tz}` : ''
    }
  })
})

// 自定义输入 → 分钟数（范围外返回 null）
const customMinutes = computed(() => {
  const n = Number(customNum.value)
  if (!Number.isInteger(n) || n <= 0) return null
  const mult = { minute: 1, hour: 60, day: 1440 }[customUnit.value]
  if (!mult) return null
  const m = n * mult
  return m >= CUSTOM_OFFSET_MIN && m <= CUSTOM_OFFSET_MAX ? m : null
})

/** 单位对应的输入上限（保证不超过 30 天） */
const customMax = computed(() => {
  const mult = { minute: 1, hour: 60, day: 1440 }[customUnit.value]
  return Math.floor(CUSTOM_OFFSET_MAX / mult)
})

const customPast = computed(() => {
  if (!props.event?.time) return true
  const now = nowInstant()
  const start = getReminderInstant(props.event, 'start')
  return Boolean(start && Number(start.epochMilliseconds) < Number(now.epochMilliseconds))
})

const customAtText = computed(() => {
  if (!props.event?.time || !customMinutes.value) return ''
  const at = formatReminderAt(props.event, 'custom', timezone.displayZone, customMinutes.value)
  return at ? `${shortDate(at.date)} · ${at.time} ${at.tz}` : ''
})

const currentAtText = computed(() => {
  if (!current.value) return ''
  const at = formatReminderAt(props.event, current.value.offset, timezone.displayZone, current.value.offsetMinutes)
  return at ? `${shortDate(at.date)} · ${at.time} ${at.tz}` : ''
})

// 确认按钮可用性：custom 需输入有效分钟数
const canConfirm = computed(() => {
  if (!selected.value) return false
  return selected.value === 'custom' ? Boolean(customMinutes.value) : true
})

function choose(id) {
  selected.value = id
}

function confirm() {
  if (!props.event || !canConfirm.value) return
  if (selected.value === 'custom') {
    reminders.setReminder(props.event.id, 'custom', customMinutes.value)
  } else {
    reminders.setReminder(props.event.id, selected.value)
  }
  confirmed.value = true
  setTimeout(close, 600)
}

function cancel() {
  if (!props.event) return
  reminders.cancelReminder(props.event.id)
  close()
}

function close() {
  emit('close')
}
</script>

<style scoped>
.reminder-overlay {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 10, 12, 0.5);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}

.reminder-panel {
  position: relative;
  width: 100%;
  max-width: 400px;
  max-height: 86vh;
  overflow-y: auto;
  padding: 32px 30px 28px;
  background: var(--bg);
  color: var(--ink);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.28);
}

.reminder-close {
  position: absolute;
  top: 14px;
  right: 16px;
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  color: var(--ink-faint);
  font-size: 14px;
  cursor: pointer;
  border-radius: 50%;
  transition: color var(--dur) var(--ease), background var(--dur) var(--ease);
}

.reminder-close:hover {
  color: var(--ink);
  background: rgba(0, 0, 0, 0.05);
}

.reminder-head {
  text-align: center;
  margin-bottom: 22px;
}

.reminder-bell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: var(--accent);
  background: var(--accent-soft);
  margin-bottom: 14px;
}

.reminder-eyebrow {
  color: var(--accent);
  margin-bottom: 8px;
}

.reminder-title {
  font-family: var(--serif);
  font-size: 24px;
  font-weight: 400;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
}

.reminder-event {
  font-size: 13px;
  letter-spacing: 0.04em;
  color: var(--ink-soft);
}

.reminder-meta {
  margin-top: 8px;
  font-size: 12px;
}

.reminder-na {
  text-align: center;
  padding: 18px 0 6px;
  font-size: 12px;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
}

/* 选项列表 */
.reminder-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.reminder-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 52px;
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  text-align: left;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.reminder-option:hover {
  border-color: var(--line-strong);
  transform: translateY(-1px);
}

.reminder-option.active {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.reminder-option.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}

.ro-radio {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border: 1.5px solid var(--line-strong);
  border-radius: 50%;
  position: relative;
  transition: all var(--dur) var(--ease);
}

.reminder-option.active .ro-radio {
  border-color: var(--accent);
}

.reminder-option.active .ro-radio::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: var(--accent);
  animation: ro-pop 0.25s var(--ease);
}

@keyframes ro-pop {
  from {
    transform: scale(0.4);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.ro-label {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--ink);
}

.ro-at {
  font-size: 10.5px;
  letter-spacing: 0.06em;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
}

.reminder-option.active .ro-at {
  color: var(--accent-ink);
}

/* 自定义偏移输入行 */
.reminder-custom {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
}

.reminder-custom-num {
  width: 88px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--line-strong);
  border-radius: calc(var(--radius-sm) - 2px);
  background: var(--bg);
  color: var(--ink);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.reminder-custom-num:focus {
  outline: none;
  border-color: var(--accent);
}

.reminder-custom-unit {
  min-height: 36px;
  padding: 0 8px;
  border: 1px solid var(--line-strong);
  border-radius: calc(var(--radius-sm) - 2px);
  background: var(--bg);
  color: var(--ink);
  font-size: 12px;
  cursor: pointer;
}

.reminder-custom-hint {
  width: 100%;
  margin: 2px 0 0;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
}

/* 底部 */
.reminder-foot {
  border-top: 1px solid var(--line);
  padding-top: 16px;
}

.reminder-set {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--ink-soft);
  margin-bottom: 12px;
  font-variant-numeric: tabular-nums;
}

.reminder-actions {
  display: flex;
  gap: 10px;
}

.reminder-actions .btn {
  flex: 1;
}

/* 动画 */
.reminder-enter-active,
.reminder-leave-active {
  transition: opacity 0.22s var(--ease);
}

.reminder-enter-active .reminder-panel,
.reminder-leave-active .reminder-panel {
  transition: transform 0.24s var(--ease), opacity 0.24s var(--ease);
}

.reminder-enter-from,
.reminder-leave-to {
  opacity: 0;
}

.reminder-enter-from .reminder-panel,
.reminder-leave-to .reminder-panel {
  transform: translateY(14px) scale(0.98);
  opacity: 0;
}

@media (max-width: 480px) {
  .reminder-panel {
    padding: 28px 20px 24px;
  }
}
</style>
