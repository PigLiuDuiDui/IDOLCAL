<template>
  <div class="reminders-view container">
    <header class="page-head">
      <p class="eyebrow">{{ t('reminders.eyebrow') }}</p>
      <h1 class="page-title">REMINDERS</h1>
      <p class="page-sub">{{ t('reminders.sub') }}</p>
      <button v-if="list.length" type="button" class="btn btn-ghost reminders-batch-toggle" @click="toggleSelectMode">
        {{ selectMode ? t('reminders.done') : t('reminders.batchSet') }}
      </button>
    </header>

    <!-- 系统推送开关（Android 直接开启；iOS 需先添加到主屏幕） -->
    <section class="push-card" :data-state="reminders.pushState">
      <span class="push-icon" aria-hidden="true">
        <svg
          viewBox="0 0 24 24"
          width="16"
          height="16"
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
      <div class="push-body">
        <p class="eyebrow push-eyebrow">{{ t('push.eyebrow') }}</p>
        <h2 class="push-title">{{ t('push.title') }}</h2>
        <template v-if="reminders.pushState === 'on'">
          <p class="push-desc">{{ t('push.on') }} · {{ t('push.needReminders') }}</p>
        </template>
        <template v-else-if="reminders.pushState === 'ios-guide'">
          <p class="push-desc">{{ t('push.iosGuide') }}</p>
          <p class="push-hint">{{ t('push.iosGuide2') }}</p>
        </template>
        <template v-else-if="reminders.pushState === 'unsupported'">
          <p class="push-desc">{{ t('push.unsupported') }}</p>
        </template>
        <template v-else>
          <p class="push-desc">{{ t('push.desc') }}</p>
          <p v-if="reminders.pushState === 'off'" class="push-hint">{{ t('push.androidHint') }}</p>
        </template>
        <p v-if="reminders.pushError" class="push-error">{{ t('push.failed', { msg: reminders.pushError }) }}</p>
        <p v-if="pushSent" class="push-sent">{{ t('push.sent') }}</p>
      </div>
      <div class="push-actions">
        <template v-if="reminders.pushState === 'on'">
          <button type="button" class="btn btn-ghost" :disabled="reminders.pushBusy" @click="sendTest">
            {{ reminders.pushBusy ? t('push.testing') : t('push.test') }}
          </button>
          <button type="button" class="btn btn-ghost" :disabled="reminders.pushBusy" @click="disablePush">
            {{ t('push.disable') }}
          </button>
        </template>
        <button
          v-else-if="reminders.pushState === 'off'"
          type="button"
          class="btn btn-accent"
          :disabled="reminders.pushBusy"
          @click="enablePush"
        >
          {{ reminders.pushBusy ? t('push.busy') : t('push.enable') }}
        </button>
      </div>
    </section>

    <!-- 提醒列表（按提醒触发日期分组：今天 / 明天 / 之后） -->
    <section v-if="list.length" class="reminders-list">
      <template v-for="(items, key) in groups" :key="key">
        <h2 v-if="items.length" class="reminders-group">{{ groupTitle(key) }}</h2>
        <div v-for="item in items" :key="item.eventId" class="reminder-row" :data-state="stateOf(item)">
          <label v-if="selectMode" class="reminder-check" :class="{ checked: isSelected(item.eventId) }">
            <input
              type="checkbox"
              :checked="isSelected(item.eventId)"
              :aria-label="t('reminders.select')"
              @change="toggleSelected(item.eventId)"
            />
            <span class="checkmark" aria-hidden="true"></span>
          </label>
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
              <strong>{{ offsetLabel(item.offset, item.offsetMinutes) }}</strong>
              · {{ atText(item) }}
            </p>
          </button>

          <button type="button" class="reminder-cancel" :aria-label="t('reminders.cancel')" @click="cancel(item.eventId)">
            {{ t('reminders.cancel') }}
          </button>
        </div>
      </template>
    </section>

    <!-- 批量操作条 -->
    <div v-if="selectMode" class="reminders-batchbar">
      <button type="button" class="batchbar-selectall" @click="toggleSelectAll">
        {{ allSelected ? t('reminders.done') : t('reminders.selectAll') }}
      </button>
      <span class="batchbar-count">{{ t('reminders.selected', { n: selectedIds.length }) }}</span>
      <button type="button" class="batchbar-set" :disabled="!selectedIds.length" @click="openBatch">
        {{ t('reminders.batchSet') }}
      </button>
      <button type="button" class="batchbar-cancel" :disabled="!selectedIds.length" @click="cancelSelected">
        {{ t('reminders.batchCancel') }}
      </button>
    </div>

    <!-- 批量设置弹窗 -->
    <Teleport to="body">
      <Transition name="reminder">
        <div v-if="batchOpen" class="batch-overlay" @click.self="batchOpen = false">
          <div class="batch-panel" role="dialog" aria-modal="true" :aria-label="t('reminders.batchTitle')">
            <button class="batch-close" type="button" :aria-label="t('reminder.close')" @click="batchOpen = false">✕</button>
            <p class="eyebrow batch-eyebrow">{{ t('reminders.batchTitle') }}</p>
            <h2 class="batch-title">{{ t('reminders.batchTitle') }}</h2>
            <p class="batch-desc">{{ t('reminders.batchDesc', { n: selectedIds.length }) }}</p>

            <div class="batch-options" role="radiogroup" :aria-label="t('reminders.batchTitle')">
              <button
                v-for="opt in presetOptions"
                :key="opt.id"
                type="button"
                role="radio"
                :aria-checked="batchSelected === opt.id"
                class="batch-option"
                :class="{ active: batchSelected === opt.id }"
                @click="batchSelected = opt.id"
              >
                <span class="ro-radio" aria-hidden="true"></span>
                <span class="ro-label">{{ t(`reminder.options.${opt.id}`) }}</span>
              </button>

              <button
                type="button"
                role="radio"
                :aria-checked="batchSelected === 'custom'"
                class="batch-option"
                :class="{ active: batchSelected === 'custom' }"
                @click="batchSelected = 'custom'"
              >
                <span class="ro-radio" aria-hidden="true"></span>
                <span class="ro-label">{{ t('reminder.options.custom') }}</span>
              </button>

              <div v-if="batchSelected === 'custom'" class="batch-custom">
                <input
                  v-model="batchNum"
                  class="batch-custom-num"
                  type="number"
                  min="1"
                  :max="batchMax"
                  step="1"
                  inputmode="numeric"
                  :aria-label="t('reminder.options.custom')"
                />
                <select v-model="batchUnit" class="batch-custom-unit" :aria-label="t('reminder.units.hour')">
                  <option value="minute">{{ t('reminder.units.minute') }}</option>
                  <option value="hour">{{ t('reminder.units.hour') }}</option>
                  <option value="day">{{ t('reminder.units.day') }}</option>
                </select>
                <p class="batch-custom-hint">{{ t('reminder.customHint') }}</p>
              </div>
            </div>

            <div class="batch-actions">
              <button type="button" class="btn btn-accent" :disabled="!canBatchApply" @click="applyBatch">
                {{ t('reminders.apply') }}
              </button>
              <button type="button" class="btn btn-ghost" @click="batchOpen = false">
                {{ t('reminder.cancel') }}
              </button>
            </div>
            <p v-if="batchApplied" class="batch-applied">{{ t('reminders.applied', { n: batchApplied }) }}</p>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 空状态 -->
    <section v-if="!list.length" class="reminders-empty">
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
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { useRemindersStore } from '../stores/reminders'
import { useTimezoneStore } from '../stores/timezone'
import {
  REMINDER_OFFSETS,
  formatReminderAt,
  getEventStart,
  getReminderInstant,
  nowInstant,
  minutesToParts,
  CUSTOM_OFFSET_MIN,
  CUSTOM_OFFSET_MAX
} from '../utils/time'
import { fullDate, todayKey, diffDays } from '../utils/date'
import { useText } from '../i18n'
import EventTime from '../components/EventTime.vue'

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
const reminders = useRemindersStore()
const timezone = useTimezoneStore()
const data = useDataStore()

const list = computed(() => reminders.list)

// ---- 日期分组：按提醒触发时刻（用户显示时区）分 Today / Tomorrow / Upcoming ----
const groups = computed(() => {
  const today = todayKey()
  const g = { today: [], tomorrow: [], upcoming: [] }
  for (const item of list.value) {
    const at = formatReminderAt(item.event, item.offset, timezone.displayZone, item.offsetMinutes)
    if (!at) {
      g.upcoming.push(item)
      continue
    }
    const diff = diffDays(today, at.date)
    if (diff === 0) g.today.push(item)
    else if (diff === 1) g.tomorrow.push(item)
    else g.upcoming.push(item)
  }
  return g
})

function groupTitle(key) {
  if (key === 'today') return t('reminders.groupToday')
  if (key === 'tomorrow') return t('reminders.groupTomorrow')
  return t('reminders.groupUpcoming')
}

// ---- 批量选择状态 ----
const selectMode = ref(false)
const selectedIds = ref([])
const batchOpen = ref(false)
const batchSelected = ref(null)
const batchNum = ref('1')
const batchUnit = ref('hour')
const batchApplied = ref(0)

const presetOptions = computed(() => REMINDER_OFFSETS.filter((o) => !o.custom))
const allSelected = computed(() => list.value.length > 0 && selectedIds.value.length === list.value.length)

/** 批量自定义输入 → 分钟数（范围外返回 null） */
const batchMinutes = computed(() => {
  const n = Number(batchNum.value)
  if (!Number.isInteger(n) || n <= 0) return null
  const mult = { minute: 1, hour: 60, day: 1440 }[batchUnit.value]
  if (!mult) return null
  const m = n * mult
  return m >= CUSTOM_OFFSET_MIN && m <= CUSTOM_OFFSET_MAX ? m : null
})

const batchMax = computed(() => {
  const mult = { minute: 1, hour: 60, day: 1440 }[batchUnit.value]
  return Math.floor(CUSTOM_OFFSET_MAX / mult)
})

const canBatchApply = computed(() =>
  batchSelected.value ? (batchSelected.value === 'custom' ? Boolean(batchMinutes.value) : true) : false
)

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  selectedIds.value = []
}

function isSelected(id) {
  return selectedIds.value.includes(id)
}

function toggleSelected(id) {
  const i = selectedIds.value.indexOf(id)
  if (i >= 0) selectedIds.value.splice(i, 1)
  else selectedIds.value.push(id)
}

function toggleSelectAll() {
  selectedIds.value = allSelected.value ? [] : list.value.map((r) => r.eventId)
}

function openBatch() {
  if (!selectedIds.value.length) return
  batchSelected.value = null
  batchNum.value = '1'
  batchUnit.value = 'hour'
  batchApplied.value = 0
  batchOpen.value = true
}

/** 将选中的偏移应用到所有勾选提醒 */
function applyBatch() {
  if (!canBatchApply.value) return
  let n = 0
  for (const id of selectedIds.value) {
    if (!reminders.reminderOf(id)) continue
    if (batchSelected.value === 'custom') reminders.setReminder(id, 'custom', batchMinutes.value)
    else reminders.setReminder(id, batchSelected.value)
    n++
  }
  batchApplied.value = n
  setTimeout(() => {
    batchOpen.value = false
    selectMode.value = false
    selectedIds.value = []
  }, 900)
}

/** 取消所有勾选提醒 */
function cancelSelected() {
  for (const id of selectedIds.value) reminders.cancelReminder(id)
  selectMode.value = false
  selectedIds.value = []
}

function offsetLabel(offsetId, offsetMinutes) {
  if (offsetId === 'custom') {
    const parts = minutesToParts(offsetMinutes)
    if (!parts) return ''
    const key = { minute: 'beforeMin', hour: 'beforeHour', day: 'beforeDay' }[parts.unit]
    return t(`reminder.${key}`, { n: parts.value })
  }
  return t(`reminder.options.${offsetId}`)
}

function atText(item) {
  const at = formatReminderAt(item.event, item.offset, timezone.displayZone, item.offsetMinutes)
  if (!at) return ''
  const [m, d] = at.date.split('-').slice(1)
  return `${m}.${d} · ${at.time} ${at.tz}`
}

// 状态：upcoming 即将提醒 / done 提醒时刻已过 / passed 活动已开始
function stateOf(item) {
  const now = Number(nowInstant().epochMilliseconds)
  const start = getEventStart(item.event)
  if (start && Number(start.toInstant().epochMilliseconds) <= now) return 'passed'
  // custom 偏移需传 offsetMinutes，否则触发时刻无法计算，永远显示 upcoming
  const instant = getReminderInstant(item.event, item.offset, item.offsetMinutes)
  if (instant && Number(instant.epochMilliseconds) <= now) return 'done'
  return 'upcoming'
}

function cancel(eventId) {
  reminders.cancelReminder(eventId)
}

// ---- 系统推送 ----
const pushSent = ref(false)

onMounted(() => {
  reminders.initPush()
})

async function enablePush() {
  await reminders.enablePush()
}

async function disablePush() {
  await reminders.disablePush()
}

async function sendTest() {
  pushSent.value = false
  try {
    const { sendTestPush, getDeviceId } = await import('../utils/push')
    await sendTestPush(getDeviceId())
    pushSent.value = true
    setTimeout(() => (pushSent.value = false), 4000)
  } catch (e) {
    reminders.pushError = e && e.message ? e.message : String(e)
  }
}</script>

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

/* 日期分组标题 */
.reminders-group {
  margin: 32px 0 2px;
  padding-bottom: 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.3em;
  color: var(--accent);
  text-transform: uppercase;
  border-bottom: 1px solid var(--line);
}

.reminders-group:first-child {
  margin-top: 0;
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

/* 批量入口 */
.reminders-batch-toggle {
  margin-top: 20px;
}

/* 选择模式 checkbox */
.reminder-check {
  position: relative;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  cursor: pointer;
}

.reminder-check input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.reminder-check .checkmark {
  display: block;
  width: 20px;
  height: 20px;
  border: 1.5px solid var(--line-strong);
  border-radius: 6px;
  background: var(--surface);
  transition: all var(--dur) var(--ease);
}

.reminder-check.checked .checkmark {
  border-color: var(--accent);
  background: var(--accent);
  background-image: radial-gradient(circle at 50% 50%, var(--bg) 0 3px, transparent 4px);
}

/* 批量操作条（fixed 底部） */
.reminders-batchbar {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  z-index: 150;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--ink);
  color: var(--bg);
  border-radius: 999px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
  font-size: 11px;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.batchbar-selectall,
.batchbar-set,
.batchbar-cancel {
  border: none;
  background: transparent;
  color: inherit;
  font-size: inherit;
  letter-spacing: inherit;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 999px;
  transition: opacity var(--dur) var(--ease);
}

.batchbar-selectall:hover,
.batchbar-set:hover {
  opacity: 0.75;
}

.batchbar-set {
  background: var(--accent);
  color: #fff;
}

.batchbar-cancel {
  color: rgba(255, 255, 255, 0.65);
}

.batchbar-cancel:hover {
  color: #fff;
}

.batchbar-selectall:disabled,
.batchbar-set:disabled,
.batchbar-cancel:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.batchbar-count {
  font-variant-numeric: tabular-nums;
}

/* 批量设置弹窗 */
.batch-overlay {
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

.batch-panel {
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

.batch-close {
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

.batch-close:hover {
  color: var(--ink);
  background: rgba(0, 0, 0, 0.05);
}

.batch-eyebrow {
  color: var(--accent);
  margin-bottom: 8px;
}

.batch-title {
  font-family: var(--serif);
  font-size: 24px;
  font-weight: 400;
  letter-spacing: 0.06em;
  margin-bottom: 8px;
}

.batch-desc {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--ink-soft);
  margin-bottom: 18px;
}

.batch-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}

.batch-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 48px;
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  text-align: left;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.batch-option:hover {
  border-color: var(--line-strong);
  transform: translateY(-1px);
}

.batch-option.active {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.batch-option.active .ro-radio {
  border-color: var(--accent);
}

.batch-option.active .ro-radio::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: var(--accent);
  animation: ro-pop 0.25s var(--ease);
}

.batch-option .ro-radio {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border: 1.5px solid var(--line-strong);
  border-radius: 50%;
  position: relative;
  transition: all var(--dur) var(--ease);
}

.batch-option .ro-label {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--ink);
}

/* 批量自定义输入行 */
.batch-custom {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
}

.batch-custom-num {
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

.batch-custom-num:focus {
  outline: none;
  border-color: var(--accent);
}

.batch-custom-unit {
  min-height: 36px;
  padding: 0 8px;
  border: 1px solid var(--line-strong);
  border-radius: calc(var(--radius-sm) - 2px);
  background: var(--bg);
  color: var(--ink);
  font-size: 12px;
  cursor: pointer;
}

.batch-custom-hint {
  width: 100%;
  margin: 2px 0 0;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
}

.batch-actions {
  display: flex;
  gap: 10px;
}

.batch-actions .btn {
  flex: 1;
}

.batch-applied {
  margin-top: 14px;
  text-align: center;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--accent);
}

/* 推送开关卡片 */
.push-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  margin-bottom: 36px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
}

.push-card[data-state='on'] {
  border-color: color-mix(in srgb, var(--accent) 40%, transparent);
  background: linear-gradient(180deg, var(--accent-soft), var(--surface));
}

.push-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid color-mix(in srgb, var(--accent) 26%, transparent);
}

.push-body {
  flex: 1;
  min-width: 0;
}

.push-eyebrow {
  margin-bottom: 4px;
}

.push-title {
  font-family: var(--serif);
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0.06em;
  margin-bottom: 6px;
}

.push-desc {
  font-size: 11px;
  line-height: 1.8;
  letter-spacing: 0.06em;
  color: var(--ink-soft);
}

.push-hint {
  margin-top: 4px;
  font-size: 10px;
  line-height: 1.8;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
}

.push-error {
  margin-top: 6px;
  font-size: 11px;
  letter-spacing: 0.05em;
  color: #c0392b;
}

.push-sent {
  margin-top: 6px;
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--accent);
}

.push-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  margin-top: 2px;
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

  .push-card {
    flex-direction: column;
    padding: 16px;
  }

  .push-actions {
    width: 100%;
  }

  .push-actions .btn {
    flex: 1;
  }
}
</style>
