<template>
  <article
    class="event-card"
    :class="{ compact: compact, featured: featured }"
    @click="ui.openEvent(event.id)"
  >
    <div class="card-date">
      <span class="card-date-day">{{ shortDate(event.date) }}</span>
      <span v-if="event.endDate" class="card-date-end">– {{ shortDate(event.endDate) }}</span>
    </div>

    <div class="card-body">
      <div class="card-meta">
        <span class="type-marker" :data-type="event.type" :data-marker="TYPE_MARKER[event.type]">
          {{ t(`types.${event.type}`) }}
        </span>
        <span v-if="featured" class="status-tag" :data-status="event.status">{{ t(`status.${event.status}`) }}</span>
      </div>

      <h3 class="card-title">{{ text(event.title) }}</h3>

      <div class="card-info">
        <span v-if="event.time" class="info-item">{{ event.time }} {{ event.timezone }}</span>
        <span v-if="event.location" class="info-item">{{ text(event.location) }}</span>
      </div>
    </div>

    <div class="card-side">
      <span v-if="!featured" class="status-tag" :data-status="event.status">{{ t(`status.${event.status}`) }}</span>
      <span class="card-arrow" aria-hidden="true">→</span>
    </div>
  </article>
</template>

<script setup>
// 小型 Editorial Card：极细边框 / 微妙圆角 / 大量留白 / hover 微动画
import { useI18n } from 'vue-i18n'
import { useUiStore } from '../stores/ui'
import { TYPE_MARKER } from '../data/events'
import { shortDate } from '../utils/date'
import { useText } from '../i18n'

defineProps({
  event: { type: Object, required: true },
  compact: { type: Boolean, default: false },
  featured: { type: Boolean, default: false }
})

const { t } = useI18n()
const text = useText()
const ui = useUiStore()
</script>

<style scoped>
.event-card {
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

/* hover：极轻的位移与边框加深，不使用强烈阴影 */
.event-card:hover {
  border-color: var(--line-strong);
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(25, 25, 25, 0.06);
}

.card-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 86px;
  padding: 20px 14px;
  border-right: 1px solid var(--line);
  background: var(--surface-alt);
}

.card-date-day {
  font-family: var(--serif);
  font-size: 24px;
  letter-spacing: 0.06em;
  color: var(--ink);
}

.card-date-end {
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--ink-faint);
}

.card-body {
  flex: 1;
  padding: 20px 22px;
  min-width: 0;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.card-title {
  font-family: var(--serif);
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0.03em;
  line-height: 1.35;
  margin-bottom: 8px;
  overflow-wrap: break-word;
}

.card-info {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
}

.info-item {
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--ink-faint);
}

.card-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  padding: 20px 20px 20px 8px;
}

.card-arrow {
  font-size: 14px;
  color: var(--ink-faint);
  transition: transform var(--dur) var(--ease), color var(--dur) var(--ease);
}

.event-card:hover .card-arrow {
  transform: translateX(4px);
  color: var(--accent);
}

/* 紧凑模式（Upcoming 时间线使用） */
.event-card.compact {
  border: none;
  border-radius: 0;
  border-bottom: 1px solid var(--line);
  background: transparent;
}

.event-card.compact:hover {
  background: var(--surface-alt);
  box-shadow: none;
  transform: none;
}

.event-card.compact .card-date {
  min-width: 64px;
  padding: 16px 12px;
  background: transparent;
  border-right-color: transparent;
}

.event-card.compact .card-date-day {
  font-size: 19px;
}

.event-card.compact .card-body {
  padding: 14px 8px;
}

.event-card.compact .card-side {
  padding: 14px 4px 14px 8px;
}

.event-card.compact .card-title {
  font-size: 15px;
  margin-bottom: 4px;
}

.event-card.compact .card-meta {
  margin-bottom: 4px;
}

/* 强调模式（最近一个活动 NEXT） */
.event-card.featured {
  border-color: color-mix(in srgb, var(--accent) 42%, var(--line));
}

.event-card.featured .card-date {
  background: var(--accent-soft);
}

.event-card.featured .card-date-day {
  color: var(--accent);
}

@media (max-width: 640px) {
  .card-date {
    min-width: 70px;
    padding: 16px 10px;
  }

  .card-body {
    padding: 16px 14px;
  }

  .card-title {
    font-size: 15px;
  }

  .card-side {
    padding: 16px 12px 16px 4px;
  }
}
</style>
