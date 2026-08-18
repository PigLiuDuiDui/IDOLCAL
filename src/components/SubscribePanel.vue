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

          <div class="subscribe-actions">
            <a class="subscribe-download" :href="icsUrl" :download="`${artist.name.toLowerCase()}-schedule.ics`">
              {{ t('subscribe.download') }}
            </a>
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
// 日历订阅弹窗：webcal 订阅链接（自动适配部署域名）+ .ics 下载备用
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { currentArtist } from '../data/artists'

defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

const { t } = useI18n()
const artist = currentArtist

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
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--ink);
  border: 1px solid rgba(0, 0, 0, 0.22);
  border-radius: 999px;
  text-decoration: none;
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
