<script setup>
// ============================================================
// 操作日志：系统事件时间线
// 数据源：GET /api/admin/overview → activity（真实投递/调度/设备事件）
// 诚实标注：后端未启用管理员审计表，此页展示系统自身事件流而非管理操作记录
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import Icon from '../components/Icon.vue'
import { adminGet } from '../api'
import { fmtDateTime, timeAgo } from '../utils/format'
import { t } from '../i18n'

const overview = ref(null)
const loading = ref(true)
const error = ref('')
const refreshing = ref(false)
const typeFilter = ref('')

async function load() {
  if (refreshing.value) return
  refreshing.value = true
  error.value = ''
  try {
    overview.value = await adminGet('/api/admin/overview')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 30_000)
})
onBeforeUnmount(() => clearInterval(timer))
let timer = null

const activity = computed(() => {
  const list = overview.value?.activity || []
  if (!typeFilter.value) return list
  return list.filter((a) => a.type === typeFilter.value)
})

const counts = computed(() => {
  const list = overview.value?.activity || []
  return {
    delivery: list.filter((a) => a.type === 'delivery').length,
    schedule: list.filter((a) => a.type === 'schedule').length,
    device: list.filter((a) => a.type === 'device').length
  }
})

const TYPE_META = {
  delivery: { icon: 'send', labelKey: 'audit.typeDelivery' },
  schedule: { icon: 'clock', labelKey: 'audit.typeSchedule' },
  device: { icon: 'smartphone', labelKey: 'audit.typeDevice' }
}

const dotClass = (a) =>
  a.type === 'delivery'
    ? a.status === 'SUCCESS' ? 'success' : a.status === 'FAILED' ? 'error' : 'warning'
    : 'brand'
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('audit.title') }}</h2>
        <div class="sub">{{ t('audit.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn" :disabled="refreshing" @click="load">
          <Icon name="refresh" :size="14" :class="{ spin: refreshing }" />
          {{ refreshing ? t('common.refreshing') : t('common.refresh') }}
        </button>
      </div>
    </div>

    <!-- 诚实标注 -->
    <div class="a-note-bar">
      <Icon name="info" :size="15" />
      <div>
        <b>{{ t('audit.noteTitle') }}</b>{{ t('audit.note') }}
      </div>
    </div>

    <div v-if="error && !overview" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <!-- 筛选 -->
    <div class="a-filter-bar">
      <select v-model="typeFilter" class="a-select">
        <option value="">{{ t('audit.allEvents', { n: counts.delivery + counts.schedule + counts.device }) }}</option>
        <option value="delivery">{{ t('audit.delivery', { n: counts.delivery }) }}</option>
        <option value="schedule">{{ t('audit.schedule', { n: counts.schedule }) }}</option>
        <option value="device">{{ t('audit.device', { n: counts.device }) }}</option>
      </select>
    </div>

    <div class="a-card">
      <div class="a-card-head">
        <div>
          <h3>{{ t('audit.timeline') }}</h3>
          <div class="desc">{{ t('audit.latest', { n: activity.length }) }}</div>
        </div>
      </div>
      <div class="a-card-body" style="padding-top: 14px">
        <div v-if="loading && !overview" class="a-skeleton" style="height: 280px" />
        <div v-else-if="!activity.length" class="a-empty" style="padding: 48px 0">
          <div class="a-empty-icon"><Icon name="clock" :size="22" /></div>
          <h5>{{ t('audit.noEvents') }}</h5>
          <p>{{ t('audit.noEventsMsg') }}</p>
        </div>
        <div v-else class="a-timeline">
          <div v-for="(a, i) in activity" :key="i" class="a-timeline-item" :class="dotClass(a)">
            <div class="time">{{ fmtDateTime(a.ts, true) }}</div>
            <div class="title">
              <span class="a-badge" :class="a.type === 'delivery' ? 'a-badge-info' : a.type === 'schedule' ? 'a-badge-brand' : 'a-badge-purple'" style="margin-right: 8px">
                {{ TYPE_META[a.type] ? t(TYPE_META[a.type].labelKey) : a.type }}
              </span>
              {{ a.title }}
            </div>
            <div class="detail">{{ a.detail }} · {{ timeAgo(a.ts) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.a-note-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  background: var(--a-info-bg);
  border: 1px solid #bfdbfe;
  color: #1e40af;
  font-size: 12.5px;
  line-height: 1.7;
}
.a-note-bar svg {
  flex-shrink: 0;
  margin-top: 2px;
}
.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
