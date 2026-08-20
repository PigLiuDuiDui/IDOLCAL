<template>
  <Teleport to="body">
    <Transition name="subscribe">
      <div v-if="open" class="subscribe-overlay" @click.self="close">
        <div class="subscribe-panel" role="dialog" aria-modal="true" :aria-label="t('subscribe.title')">
          <button class="subscribe-close" type="button" :aria-label="t('subscribe.close')" @click="close">
            ×
          </button>

          <p class="eyebrow subscribe-eyebrow">{{ t('subscribe.eyebrow') }}</p>
          <h2 class="subscribe-title">{{ t('subscribe.title') }}</h2>
          <p class="subscribe-desc">{{ t('subscribe.desc', { artist: artist.name }) }}</p>

          <div class="subscribe-link">
            <code class="subscribe-link-code">{{ webcalUrl }}</code>
            <button class="subscribe-copy" type="button" @click="copy">
              {{ copied ? t('subscribe.copied') : copyFailed ? t('subscribe.copyFail') : t('subscribe.copy') }}
            </button>
          </div>

          <!-- 下载提醒设置：已单独设置的提醒优先，其余用统一默认 -->
          <div class="subscribe-alarm">
            <p class="subscribe-alarm-label">{{ t('subscribe.alarmLabel') }}</p>
            <div class="subscribe-alarm-options" role="radiogroup" :aria-label="t('subscribe.alarmLabel')">
              <button
                v-for="opt in alarmPresets"
                :key="opt.id"
                type="button"
                role="radio"
                :aria-checked="alarmSelected === opt.id"
                class="subscribe-alarm-option"
                :class="{ active: alarmSelected === opt.id }"
                @click="alarmSelected = opt.id"
              >
                {{ t(`reminder.options.${opt.id}`) }}
              </button>
              <button
                type="button"
                role="radio"
                :aria-checked="alarmSelected === 'custom'"
                class="subscribe-alarm-option"
                :class="{ active: alarmSelected === 'custom' }"
                @click="alarmSelected = 'custom'"
              >
                {{ t('reminder.options.custom') }}
              </button>
              <button
                type="button"
                role="radio"
                :aria-checked="alarmSelected === 'none'"
                class="subscribe-alarm-option"
                :class="{ active: alarmSelected === 'none' }"
                @click="alarmSelected = 'none'"
              >
                {{ t('subscribe.alarmNone') }}
              </button>
            </div>

            <div v-if="alarmSelected === 'custom'" class="subscribe-alarm-custom">
              <input
                v-model="alarmNum"
                class="subscribe-alarm-num"
                type="number"
                min="1"
                :max="alarmMax"
                step="1"
                inputmode="numeric"
                :aria-label="t('reminder.options.custom')"
              />
              <select v-model="alarmUnit" class="subscribe-alarm-unit" :aria-label="t('reminder.units.hour')">
                <option value="minute">{{ t('reminder.units.minute') }}</option>
                <option value="hour">{{ t('reminder.units.hour') }}</option>
                <option value="day">{{ t('reminder.units.day') }}</option>
              </select>
              <p class="subscribe-alarm-hint">{{ t('reminder.customHint') }}</p>
            </div>

            <p class="subscribe-alarm-static">{{ t('subscribe.alarmStatic') }}</p>
          </div>

          <div class="subscribe-actions">
            <button type="button" class="subscribe-download" @click="downloadIcs">
              {{ t('subscribe.download') }}
            </button>
          </div>

          <ol class="subscribe-steps">
            <li>{{ t('subscribe.stepIos') }}</li>
            <li>{{ t('subscribe.stepAndroid') }}</li>
          </ol>

          <p class="subscribe-hint">{{ isLocal ? t('subscribe.localHint') : t('subscribe.deployHint') }}</p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
// 日历订阅弹窗：webcal 订阅链接（自动适配部署域名）+ .ics 动态下载（可自定义提醒）
// 下载时每个活动优先使用已单独设置的提醒（提醒页），未设置的用面板统一默认
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useRemindersStore } from '../stores/reminders'
import { REMINDER_OFFSETS, CUSTOM_OFFSET_MIN, CUSTOM_OFFSET_MAX, reminderOffsetMinutes } from '../utils/time'
import { generateIcs } from '../utils/ics'

defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

const { t } = useI18n()
const data = useDataStore()
const reminders = useRemindersStore()
const artist = computed(() => data.currentArtist)

// ---- 下载提醒设置（默认 1 小时前；'none' 不加提醒） ----
const alarmSelected = ref('1h')
const alarmNum = ref('1')
const alarmUnit = ref('hour')
const alarmPresets = computed(() => REMINDER_OFFSETS.filter((o) => !o.custom))

const alarmMinutes = computed(() => {
  if (alarmSelected.value === 'none') return null
  if (alarmSelected.value === 'custom') {
    const n = Number(alarmNum.value)
    if (!Number.isInteger(n) || n <= 0) return null
    const mult = { minute: 1, hour: 60, day: 1440 }[alarmUnit.value]
    if (!mult) return null
    const m = n * mult
    return m >= CUSTOM_OFFSET_MIN && m <= CUSTOM_OFFSET_MAX ? m : null
  }
  const map = { '1d': 1440, '3h': 180, '1h': 60, '30m': 30, start: 0 }
  return map[alarmSelected.value] ?? null
})

const alarmMax = computed(() => {
  const mult = { minute: 1, hour: 60, day: 1440 }[alarmUnit.value]
  return Math.floor(CUSTOM_OFFSET_MAX / mult)
})

/** 每事件提醒分钟数：已单独设置的优先，否则用面板统一默认 */
function alarmOf(event) {
  const r = reminders.reminderOf(event.id)
  if (r) return reminderOffsetMinutes(r)
  return alarmMinutes.value
}

/** 动态生成 ICS 并触发浏览器下载 */
function downloadIcs() {
  if (!artist.value) return
  const text = generateIcs({
    events: data.events,
    artist: artist.value,
    typeLabel: data.TYPE_LABEL,
    alarmMinutesOf: alarmOf
  })
  const blob = new Blob([text], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${artist.value.name.toLowerCase()}-schedule.ics`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

const close = () => emit('close')

const base = import.meta.env.BASE_URL
const icsUrl = computed(() => `${window.location.origin}${base}calendar.ics`)
const webcalUrl = computed(() => icsUrl.value.replace(/^http(s)?:/i, 'webcal:'))
const isLocal = computed(() => ['localhost', '127.0.0.1'].includes(window.location.hostname))

const copied = ref(false)
const copyFailed = ref(false)

/** 兼容方案：隐藏 textarea + execCommand，安卓/微信浏览器均可用 */
function legacyCopy(text) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.setAttribute('readonly', '')
  ta.style.position = 'fixed'
  ta.style.left = '-9999px'
  ta.style.top = '0'
  document.body.appendChild(ta)
  ta.select()
  ta.setSelectionRange(0, text.length)
  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch {
    ok = false
  }
  document.body.removeChild(ta)
  return ok
}

async function copy() {
  let ok = false
  if (navigator.clipboard && window.isSecureContext) {
    // 安全上下文（HTTPS）：优先用 Clipboard API
    try {
      await navigator.clipboard.writeText(webcalUrl.value)
      ok = true
    } catch {
      ok = legacyCopy(webcalUrl.value)
    }
  } else {
    // 非 HTTPS / 浏览器不支持：走兼容方案
    ok = legacyCopy(webcalUrl.value)
  }

  // 只有真正复制成功才提示已复制
  copied.value = ok
  copyFailed.value = !ok
  setTimeout(() => {
    copied.value = false
    copyFailed.value = false
  }, 2500)
}
</script>

<style scoped>
.subscribe-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(10, 10, 12, 0.55);
  backdrop-filter: blur(3px);
}

.subscribe-panel {
  position: relative;
  width: 100%;
  max-width: 460px;
  max-height: 85vh;
  overflow-y: auto;
  padding: 40px 36px 34px;
  background: var(--bg);
  color: var(--ink);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.28);
}

.subscribe-close {
  position: absolute;
  top: 14px;
  right: 16px;
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  color: var(--ink-faint);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  border-radius: 50%;
  transition: color var(--dur) var(--ease), background var(--dur) var(--ease);
}

.subscribe-close:hover {
  color: var(--ink);
  background: rgba(0, 0, 0, 0.05);
}

.subscribe-eyebrow {
  color: var(--accent);
  margin-bottom: 10px;
}

.subscribe-title {
  font-family: var(--serif);
  font-size: 26px;
  font-weight: 400;
  letter-spacing: 0.04em;
  margin-bottom: 12px;
}

.subscribe-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-soft);
  margin-bottom: 22px;
}

.subscribe-link {
  display: flex;
  align-items: stretch;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: calc(var(--radius) - 4px);
  overflow: hidden;
  margin-bottom: 12px;
}

.subscribe-link-code {
  flex: 1;
  min-width: 0;
  padding: 12px 14px;
  font-family: var(--mono);
  font-size: 11px;
  line-height: 1.5;
  color: var(--ink);
  background: rgba(0, 0, 0, 0.03);
  word-break: break-all;
}

.subscribe-copy {
  flex-shrink: 0;
  padding: 0 18px;
  border: none;
  background: var(--ink);
  color: var(--bg);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  cursor: pointer;
  transition: opacity var(--dur) var(--ease);
}

.subscribe-copy:hover {
  opacity: 0.82;
}

/* 下载提醒设置 */
.subscribe-alarm {
  margin-bottom: 20px;
  padding-top: 2px;
}

.subscribe-alarm-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--ink-faint);
  text-transform: uppercase;
  margin-bottom: 10px;
}

.subscribe-alarm-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.subscribe-alarm-option {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 10.5px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.subscribe-alarm-option:hover {
  border-color: var(--line-strong);
}

.subscribe-alarm-option.active {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent-ink);
}

.subscribe-alarm-custom {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--surface);
  margin-bottom: 10px;
}

.subscribe-alarm-num {
  width: 84px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--line-strong);
  border-radius: calc(var(--radius-sm) - 2px);
  background: var(--bg);
  color: var(--ink);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.subscribe-alarm-num:focus {
  outline: none;
  border-color: var(--accent);
}

.subscribe-alarm-unit {
  min-height: 34px;
  padding: 0 8px;
  border: 1px solid var(--line-strong);
  border-radius: calc(var(--radius-sm) - 2px);
  background: var(--bg);
  color: var(--ink);
  font-size: 11px;
  cursor: pointer;
}

.subscribe-alarm-hint {
  width: 100%;
  margin: 2px 0 0;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
}

.subscribe-alarm-static {
  font-size: 10px;
  line-height: 1.7;
  color: var(--ink-faint);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 10px;
}

.subscribe-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.subscribe-download {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--ink);
  background: transparent;
  border: 1px solid rgba(0, 0, 0, 0.22);
  border-radius: 999px;
  text-decoration: none;
  cursor: pointer;
  transition: border-color var(--dur) var(--ease), color var(--dur) var(--ease);
}

.subscribe-download:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.subscribe-steps {
  margin: 0 0 14px;
  padding-left: 20px;
  font-size: 12px;
  line-height: 1.9;
  color: var(--ink-soft);
}

.subscribe-hint {
  font-size: 11px;
  line-height: 1.7;
  color: var(--accent);
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding-top: 14px;
}

/* 弹出动画 */
.subscribe-enter-active,
.subscribe-leave-active {
  transition: opacity 0.25s var(--ease);
}

.subscribe-enter-active .subscribe-panel,
.subscribe-leave-active .subscribe-panel {
  transition: transform 0.25s var(--ease), opacity 0.25s var(--ease);
}

.subscribe-enter-from,
.subscribe-leave-to {
  opacity: 0;
}

.subscribe-enter-from .subscribe-panel,
.subscribe-leave-to .subscribe-panel {
  transform: translateY(14px) scale(0.98);
  opacity: 0;
}

@media (max-width: 480px) {
  .subscribe-panel {
    padding: 34px 22px 28px;
  }
}
</style>
