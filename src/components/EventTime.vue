<template>
  <span v-if="parts.local" class="event-time" :class="{ inline, invert }">
    <span class="et-local">
      {{ parts.local.time }} {{ parts.local.tz }}
      <i v-if="parts.dayShift !== 0" class="et-shift" :data-dir="parts.dayShift > 0 ? 'plus' : 'minus'">
        {{ parts.dayShift > 0 ? `+${parts.dayShift}` : parts.dayShift }}
      </i>
    </span>
    <span v-if="showOfficial && !isSameTime" class="et-official">
      {{ parts.official.date !== parts.local.date ? shortDate(parts.official.date) + ' · ' : '' }}{{ parts.official.time }} {{ parts.official.tz }} · {{ t('timezone.official') }}
    </span>
  </span>
</template>

<script setup>
// 统一时间显示：用户当地时间为主，官方时间为辅（所有页面共用，保证一致）
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { localizeEvent } from '../utils/time'
import { useTimezoneStore } from '../stores/timezone'
import { shortDate } from '../utils/date'

const props = defineProps({
  event: { type: Object, required: true },
  /** 是否显示官方时间副行 */
  showOfficial: { type: Boolean, default: true },
  /** 行内紧凑模式（单行） */
  inline: { type: Boolean, default: false },
  /** 深色背景（Hero 等） */
  invert: { type: Boolean, default: false }
})

const { t } = useI18n()
const timezone = useTimezoneStore()

const parts = computed(() => localizeEvent(props.event, timezone.displayZone))

// 本地与官方完全一致时（用户处于官方时区）只显示一行
const isSameTime = computed(
  () => parts.value.local && parts.value.official && parts.value.local.time === parts.value.official.time && parts.value.local.tz === parts.value.official.tz
)
</script>

<style scoped>
.event-time {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  vertical-align: bottom;
}

.event-time.inline {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.et-local {
  font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 深色背景适配 */
.event-time.invert .et-local {
  color: var(--ink-invert);
}

.event-time.invert .et-official {
  color: rgba(255, 255, 255, 0.52);
}

.et-shift {
  font-style: normal;
  font-size: 0.82em;
  font-weight: 700;
  margin-left: 4px;
  padding: 1px 5px;
  border-radius: 999px;
}

.et-shift[data-dir='plus'] {
  color: var(--accent);
  background: var(--accent-soft);
}

.et-shift[data-dir='minus'] {
  color: var(--ink-faint);
  background: var(--surface-alt);
}

.et-official {
  font-size: 0.85em;
  letter-spacing: 0.06em;
  color: var(--ink-faint);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>
