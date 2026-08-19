<template>
  <section class="hero">
    <div class="hero-bg">
      <!-- 大字号水印式年份，营造杂志 Archive 感 -->
      <span class="hero-year" aria-hidden="true">{{ artist.year }}</span>
      <div class="hero-grid" aria-hidden="true"></div>
    </div>

    <div class="container hero-inner">
      <div class="hero-main">
        <p class="eyebrow hero-eyebrow">{{ t('hero.officialArchive', { year: artist.year }) }}</p>
        <h1 class="hero-name">{{ artist.name }}</h1>
        <p class="hero-sub">{{ artist.subName }}</p>

        <div class="hero-era">
          <span class="hero-era-label">{{ t('hero.currentEra') }}</span>
          <span class="hero-era-value">{{ artist.era }}</span>
          <span class="hero-era-period">{{ artist.eraPeriod }}</span>
        </div>
      </div>

      <div class="hero-next" v-if="nextEvent">
        <p class="eyebrow hero-next-eyebrow">{{ t('hero.nextEvent') }}</p>

        <button class="hero-next-card" type="button" @click="ui.openEvent(nextEvent.id)">
          <div class="next-top">
            <span class="next-date">{{ shortDate(nextEvent.date) }}</span>
            <span class="next-countdown">{{ countdownLabel(nextEvent.date) }}</span>
          </div>

          <h2 class="next-title">{{ text(nextEvent.title) }}</h2>

          <div class="next-meta">
            <span class="type-marker" :data-type="nextEvent.type" :data-marker="data.TYPE_MARKER[nextEvent.type]">
              {{ t(`types.${nextEvent.type}`) }}
            </span>
            <span v-if="nextEvent.time" class="next-time"><EventTime :event="nextEvent" inline invert /></span>
            <span v-if="nextEvent.location" class="next-loc">{{ text(nextEvent.location) }}</span>
          </div>
        </button>

        <p class="hero-next-hint">{{ t('hero.clickForDetails') }}</p>

        <button class="hero-subscribe" type="button" @click="subscribeOpen = true">
          {{ t('subscribe.button') }}
        </button>
      </div>
    </div>
  </section>

  <SubscribePanel :open="subscribeOpen" @close="subscribeOpen = false" />
</template>

<script setup>
// Hero：艺人身份 + 当前时期 + 下一活动倒计时
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataStore } from '../stores/data'
import { useUiStore } from '../stores/ui'
import { shortDate, countdownLabel, todayKey } from '../utils/date'
import { useText } from '../i18n'
import SubscribePanel from './SubscribePanel.vue'
import EventTime from './EventTime.vue'

const { t } = useI18n()
const text = useText()
const data = useDataStore()
const artist = computed(() => data.currentArtist)
const ui = useUiStore()

// 日历订阅弹窗开关
const subscribeOpen = ref(false)

// 下一个未来活动（含今天）
const nextEvent = computed(() => {
  const today = todayKey()
  return data.eventsSorted.find((e) => e.date >= today) || null
})
</script>

<style scoped>
.hero {
  position: relative;
  background: var(--bg-deep);
  color: var(--ink-invert);
  overflow: hidden;
}

/* 背景：深色 + 克制的细网格，避免图片墙 */
.hero-bg {
  position: absolute;
  inset: 0;
}

.hero-year {
  position: absolute;
  right: -0.05em;
  top: 50%;
  transform: translateY(-50%);
  font-family: var(--serif);
  font-size: clamp(180px, 26vw, 420px);
  line-height: 1;
  color: rgba(255, 255, 255, 0.045);
  letter-spacing: -0.02em;
  user-select: none;
  pointer-events: none;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(90deg, transparent, #000 60%, #000);
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 60%, #000);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 48px;
  min-height: 480px;
  padding-top: 88px;
  padding-bottom: 72px;
}

.hero-eyebrow {
  color: rgba(255, 255, 255, 0.5);
}

.hero-name {
  font-family: var(--serif);
  font-size: clamp(72px, 11vw, 148px);
  font-weight: 400;
  line-height: 0.95;
  letter-spacing: 0.06em;
  margin: 18px 0 6px;
}

.hero-sub {
  font-size: clamp(12px, 1.4vw, 16px);
  font-weight: 600;
  letter-spacing: 0.5em;
  color: rgba(255, 255, 255, 0.75);
}

.hero-era {
  margin-top: 40px;
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  padding-top: 18px;
  max-width: 520px;
}

.hero-era-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.3em;
  color: var(--accent-soft);
  border: 1px solid rgba(166, 47, 47, 0.65);
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(166, 47, 47, 0.16);
}

.hero-era-value {
  font-family: var(--serif);
  font-size: 26px;
  font-style: italic;
  letter-spacing: 0.06em;
}

.hero-era-period {
  font-size: 10px;
  letter-spacing: 0.24em;
  color: rgba(255, 255, 255, 0.45);
}

/* NEXT EVENT 卡片 */
.hero-next {
  align-self: flex-end;
  width: 100%;
  max-width: 380px;
}

.hero-next-eyebrow {
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 12px;
}

.hero-next-card {
  display: block;
  width: 100%;
  text-align: left;
  padding: 26px 28px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: var(--radius);
  color: var(--ink-invert);
  cursor: pointer;
  backdrop-filter: blur(4px);
  transition: border-color var(--dur) var(--ease), transform var(--dur) var(--ease),
    background var(--dur) var(--ease);
}

.hero-next-card:hover {
  border-color: rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.09);
  transform: translateY(-3px);
}

.next-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.next-date {
  font-family: var(--serif);
  font-size: 22px;
  letter-spacing: 0.1em;
}

.next-countdown {
  font-family: var(--mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: #fff;
  background: var(--accent);
  padding: 6px 12px;
  border-radius: 999px;
}

.next-title {
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 0.04em;
  margin-bottom: 16px;
  line-height: 1.25;
}

.next-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.66);
}

.hero-next-hint {
  margin-top: 10px;
  font-size: 10px;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.32);
  text-align: center;
}

/* 订阅日历按钮（Hero 下方，描边样式） */
.hero-subscribe {
  display: block;
  width: 100%;
  margin-top: 18px;
  padding: 12px 18px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(255, 255, 255, 0.85);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 999px;
  cursor: pointer;
  transition: border-color var(--dur) var(--ease), color var(--dur) var(--ease),
    background var(--dur) var(--ease);
}

.hero-subscribe:hover {
  border-color: var(--accent-soft);
  color: #fff;
  background: rgba(166, 47, 47, 0.18);
}

/* 移动端：Hero 缩小，纵向排列 */
@media (max-width: 900px) {
  .hero-inner {
    flex-direction: column;
    min-height: 0;
    padding-top: 56px;
    padding-bottom: 48px;
    gap: 40px;
  }

  .hero-name {
    font-size: 64px;
  }

  .hero-next {
    max-width: none;
  }

  .hero-grid {
    background-size: 48px 48px;
  }
}
</style>
