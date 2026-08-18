<template>
  <div class="locale-switcher" role="group" :aria-label="t('common.language')">
    <button
      v-for="l in LOCALES"
      :key="l.code"
      type="button"
      class="ls-btn"
      :class="{ active: current === l.code }"
      :title="l.label"
      :aria-pressed="current === l.code"
      @click="setLocale(l.code)"
    >
      {{ l.short }}
    </button>
  </div>
</template>

<script setup>
// 语言切换：EN / 中文 / 한국어，选择持久化到 localStorage
import { useI18n } from 'vue-i18n'
import { LOCALES, setLocale } from '../i18n'

const { t, locale } = useI18n()
const current = locale
</script>

<style scoped>
.locale-switcher {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
}

.ls-btn {
  min-height: 28px;
  padding: 0 10px;
  border: none;
  border-radius: 999px;
  background: transparent;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}

.ls-btn:hover {
  color: var(--ink);
}

.ls-btn.active {
  background: var(--ink);
  color: #fff;
}
</style>
