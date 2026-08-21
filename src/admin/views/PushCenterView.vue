<script setup>
// ============================================================
// Push Center：Fan-out 调度监控
// 数据源：GET /api/admin/push/schedules（分页）+ GET /api/admin/overview
// 每 15s 自动刷新（PROCESSING 状态需要实时性）
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ProgressBar from '../components/ProgressBar.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { adminGet } from '../api'
import { fmtNum, fmtDateTime, timeAgo } from '../utils/format'
import { t } from '../i18n'

const route = useRoute()
const router = useRouter()

const page = ref(0)
const size = 12
// 状态筛选 Tab：全部 / 待处理 / 处理中 / 失败 / 重试（支持 URL query 直达）
const TABS = [
  { id: '', labelKey: 'common.all' },
  { id: 'PENDING', labelKey: 'push.pendingTasks' },
  { id: 'PROCESSING', labelKey: 'push.processing' },
  { id: 'FAILED', labelKey: 'push.tabFailed' },
  { id: 'RETRY', labelKey: 'push.tabRetry' }
]
const STATUS_OPTS = TABS.map((x) => x.id).filter(Boolean)
const statusFilter = ref(String(route.query.status || ''))
if (statusFilter.value && !STATUS_OPTS.includes(statusFilter.value)) statusFilter.value = ''

const data = ref(null) // Spring Page
const overview = ref(null)
const loading = ref(true)
const error = ref('')
const refreshing = ref(false)

async function load() {
  if (refreshing.value) return
  refreshing.value = true
  error.value = ''
  try {
    const params = { page: page.value, size }
    if (statusFilter.value) params.status = statusFilter.value
    const [list, ov] = await Promise.all([
      adminGet('/api/admin/push/schedules', params),
      adminGet('/api/admin/overview')
    ])
    data.value = list
    overview.value = ov
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 15_000)
})
onBeforeUnmount(() => clearInterval(timer))
let timer = null

function onFilter(id) {
  statusFilter.value = id
  page.value = 0
  router.replace({ path: '/admin/push', query: id ? { status: id } : {} })
  load()
}

// ---- 派生 ----
const byStatus = computed(() => overview.value?.push?.tasks?.byStatus || {})
const today = computed(() => overview.value?.push?.today || {})
const schedules = computed(() => data.value?.content || [])

const kpis = computed(() => {
  const s = byStatus.value
  return {
    total: data.value?.totalElements ?? 0,
    pending: s.PENDING || 0,
    processing: s.PROCESSING || 0,
    attention: (s.FAILED || 0) + (s.RETRY || 0)
  }
})

const progressTone = (status) =>
  status === 'SUCCESS' ? 'success' : status === 'FAILED' ? 'error' : status === 'RETRY' ? 'warning' : 'brand'

const triggerLabel = (s) => {
  if (s.status === 'SUCCESS' && s.finishedAt) return t('push.finished', { ago: timeAgo(s.finishedAt) })
  if (s.status === 'PENDING') return t('push.due', { ago: timeAgo(s.triggerAt) })
  return timeAgo(s.processedAt || s.triggerAt)
}

const statusCount = (st) => byStatus.value[st] || 0

/** 全部任务数（含 SUCCESS 终态） */
const totalAll = computed(() =>
  ['PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'RETRY'].reduce((sum, s) => sum + (byStatus.value[s] || 0), 0)
)
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('push.title') }}</h2>
        <div class="sub">{{ t('push.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn" :disabled="refreshing" @click="load">
          <Icon name="refresh" :size="14" :class="{ spin: refreshing }" />
          {{ refreshing ? t('common.refreshing') : t('common.refresh') }}
        </button>
      </div>
    </div>

    <!-- KPI -->
    <div class="a-grid a-grid-kpi" style="margin-bottom: 16px">
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('push.totalSchedules') }}</span>
          <span class="a-kpi-icon"><Icon name="send" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ fmtNum(kpis.total) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('push.fanoutGroups') }}</span></div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('push.pendingTasks') }}</span>
          <span class="a-kpi-icon"><Icon name="clock" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ fmtNum(kpis.pending) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('push.waitingTrigger') }}</span></div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('push.processing') }}</span>
          <span class="a-kpi-icon"><Icon name="zap" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ fmtNum(kpis.processing) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('push.inFlight') }}</span></div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('push.needAttention') }}</span>
          <span class="a-kpi-icon"><Icon name="alert-triangle" :size="15" /></span>
        </div>
        <div class="a-kpi-value" :style="kpis.attention ? { color: 'var(--a-error)' } : {}">{{ fmtNum(kpis.attention) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('push.failedRetrying') }}</span></div>
      </div>
    </div>

    <!-- 状态筛选 Tab（带实时计数） -->
    <div class="a-filter-bar">
      <div class="a-tabs" role="tablist" aria-label="status filter">
        <button
          v-for="tb in TABS"
          :key="tb.id || 'all'"
          class="a-tab"
          :class="{ active: statusFilter === tb.id }"
          role="tab"
          :aria-selected="statusFilter === tb.id"
          @click="onFilter(tb.id)"
        >
          {{ t(tb.labelKey) }}
          <span class="cnt">{{ fmtNum(tb.id ? statusCount(tb.id) : totalAll) }}</span>
        </button>
      </div>
      <span class="a-filter-hint">
        {{ t('push.today') }}: <b>{{ fmtNum(today.total) }}</b> {{ t('push.sent') }} ·
        <b style="color: var(--a-success)">{{ today.success }}</b> {{ t('push.success') }} ·
        <b style="color: var(--a-error)">{{ today.failed }}</b> {{ t('push.failed') }} ·
        <b style="color: var(--a-warning)">{{ today.expired }}</b> {{ t('push.expired') }}
      </span>
    </div>

    <div v-if="error && !data" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <div class="a-card">
      <div class="a-card-body" style="padding-top: 12px">
        <SkeletonTable v-if="loading && !data" :rows="6" :cols="5" />
        <div v-else-if="!schedules.length" class="a-table-wrap">
          <EmptyState
            icon="send"
            :title="t('push.noSchedules')"
            :message="t('push.noSchedulesMsg')"
          />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('push.colTask') }}</th>
                <th>{{ t('push.colTrigger') }}</th>
                <th>{{ t('push.colStatus') }}</th>
                <th>{{ t('push.colProgress') }}</th>
                <th style="text-align: right">{{ t('push.colActions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in schedules" :key="s.id">
                <td style="max-width: 320px">
                  <div class="a-cell-main">{{ s.eventTitle }}</div>
                  <div class="a-cell-sub">
                    #{{ s.id }} · {{ s.eventId }} · {{ s.offsetMinutes === 0 ? t('common.onTime') : t('common.minBefore', { n: s.offsetMinutes }) }}
                  </div>
                </td>
                <td>
                  <div class="a-cell-main">{{ fmtDateTime(s.triggerAt) }}</div>
                  <div class="a-cell-sub">{{ triggerLabel(s) }}</div>
                </td>
                <td><StatusBadge :status="s.status" /></td>
                <td style="min-width: 180px">
                  <ProgressBar :value="s.progress" :tone="progressTone(s.status)" />
                  <div class="a-cell-sub" style="margin-top: 3px">
                    <b>{{ fmtNum(s.success) }}</b> / {{ fmtNum(s.target) }}
                    <template v-if="s.failed"> · <span style="color: var(--a-error)">{{ fmtNum(s.failed) }} {{ t('push.failed') }}</span></template>
                    <template v-if="s.expired"> · <span style="color: var(--a-warning)">{{ fmtNum(s.expired) }} {{ t('push.expired') }}</span></template>
                  </div>
                </td>
                <td>
                  <div class="a-cell-actions" style="justify-content: flex-end">
                    <button class="a-btn a-btn-sm" @click="router.push(`/admin/push/tasks/${s.id}`)">
                      {{ t('push.details') }} <Icon name="chevron-right" :size="13" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <PaginationBar :page="page" :size="size" :total="data?.totalElements || 0" @change="page = $event; load()" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 状态筛选 Tab */
.a-tabs {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border-radius: 10px;
  background: var(--a-card-alt);
  border: 1px solid var(--a-border);
  flex-wrap: wrap;
}
.a-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--a-text-2);
  font-size: 12.5px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.a-tab:hover {
  color: var(--a-text);
  background: var(--a-card);
}
.a-tab.active {
  background: var(--a-primary);
  color: #fff;
}
.a-tab .cnt {
  font-size: 11px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--a-border);
  color: var(--a-text-2);
}
.a-tab.active .cnt {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.a-filter-hint {
  font-size: 12.5px;
  color: var(--a-text-2);
  margin-left: auto;
  white-space: nowrap;
}
.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
