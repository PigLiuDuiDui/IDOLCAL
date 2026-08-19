<template>
  <div class="tz-switcher" ref="rootRef">
    <button type="button" class="tz-btn" :aria-label="t('timezone.select')" @click="open = !open">
      <span class="tz-btn-icon" aria-hidden="true">◒</span>
      <span class="tz-btn-label">{{ label }}</span>
      <span class="tz-btn-caret" aria-hidden="true">▾</span>
    </button>

    <Teleport to="body">
      <Transition name="tz-pop">
        <div v-if="open" class="tz-overlay" @click.self="close">
          <div class="tz-panel" role="dialog" :aria-label="t('timezone.select')">
            <div class="tz-head">
              <span class="eyebrow">{{ t('timezone.eyebrow') }}</span>
              <button type="button" class="tz-close" :aria-label="t('timezone.close')" @click="close">✕</button>
            </div>

            <button
              type="button"
              class="tz-option"
              :class="{ active: timezone.isAuto }"
              @click="choose('auto')"
            >
              <span class="tz-option-name">{{ t('timezone.auto') }}</span>
              <span class="tz-option-sub">{{ t('timezone.autoHint') }}</span>
              <span class="tz-option-abbr">{{ autoAbbr }}</span>
            </button>

            <div class="tz-divider"></div>

            <div class="tz-scroll">
              <button
                v-for="zone in TIMEZONE_OPTIONS"
                :key="zone"
                type="button"
                class="tz-option"
                :class="{ active: timezone.zone === zone }"
                @click="choose(zone)"
              >
                <span class="tz-option-name">{{ zoneName(zone) }}</span>
                <span class="tz-option-sub">{{ zoneAbbr(zone) }}</span>
                <span class="tz-option-abbr">{{ zoneAbbr(zone) }}</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
// 时区选择器：自动检测（默认）或手动选择显示时区，持久化 localStorage
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { TIMEZONE_OPTIONS, tzAbbr, browserZone } from '../utils/time'
import { useTimezoneStore } from '../stores/timezone'

const { t } = useI18n()
const timezone = useTimezoneStore()
const open = ref(false)
const rootRef = ref(null)

// 按钮标签：当前显示时区缩写（auto 时显示浏览器时区缩写）
const label = computed(() => {
  const zone = timezone.displayZone
  const abbr = tzAbbr(zone)
  return timezone.isAuto ? `${abbr} · ${t('timezone.auto')}` : abbr
})

const autoAbbr = computed(() => tzAbbr(browserZone()))

function zoneName(zone) {
  return zone.replace(/_/g, ' ').replace('/', ' / ')
}

function zoneAbbr(zone) {
  return tzAbbr(zone)
}

function choose(zone) {
  timezone.setZone(zone)
  close()
}

function close() {
  open.value = false
}

// 点击外部关闭
function onDocClick(e) {
  if (open.value && rootRef.value && !rootRef.value.contains(e.target)) close()
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.tz-switcher {
  position: relative;
}

.tz-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--ink-soft);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  white-space: nowrap;
}

.tz-btn:hover {
  border-color: var(--line-strong);
  color: var(--ink);
}

.tz-btn-icon {
  font-size: 11px;
  color: var(--ink-faint);
}

.tz-btn-caret {
  font-size: 8px;
  color: var(--ink-faint);
}

/* 弹层 */
.tz-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(10, 10, 12, 0.28);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

.tz-panel {
  position: fixed;
  top: 76px;
  right: max(32px, calc((100vw - var(--maxw)) / 2));
  z-index: 301;
  width: 300px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--line-strong);
  border-radius: var(--radius);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.16);
}

.tz-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.tz-close {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  color: var(--ink-faint);
  font-size: 13px;
  cursor: pointer;
  border-radius: 50%;
  transition: all var(--dur) var(--ease);
}

.tz-close:hover {
  color: var(--ink);
  background: var(--surface-alt);
}

.tz-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 8px 12px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background var(--dur) var(--ease);
}

.tz-option:hover {
  background: var(--surface-alt);
}

.tz-option.active {
  background: var(--accent-soft);
}

.tz-option-name {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tz-option.active .tz-option-name {
  color: var(--accent);
}

.tz-option-sub {
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--ink-faint);
}

.tz-option-abbr {
  flex-shrink: 0;
  font-family: var(--mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--ink-soft);
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
}

.tz-option.active .tz-option-abbr {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 40%, transparent);
}

.tz-divider {
  height: 1px;
  background: var(--line);
  margin: 8px 0;
}

.tz-scroll {
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

/* 动画 */
.tz-pop-enter-active,
.tz-pop-leave-active {
  transition: opacity 0.2s var(--ease);
}

.tz-pop-enter-active .tz-panel,
.tz-pop-leave-active .tz-panel {
  transition: transform 0.22s var(--ease), opacity 0.22s var(--ease);
}

.tz-pop-enter-from,
.tz-pop-leave-to {
  opacity: 0;
}

.tz-pop-enter-from .tz-panel,
.tz-pop-leave-to .tz-panel {
  transform: translateY(-8px) scale(0.98);
  opacity: 0;
}

/* 移动端：面板改为底部 Sheet */
@media (max-width: 900px) {
  .tz-panel {
    top: auto;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    max-height: 75vh;
    border-radius: 16px 16px 0 0;
    padding-bottom: calc(20px + env(safe-area-inset-bottom));
  }

  .tz-btn-label {
    display: none;
  }
}
</style>
