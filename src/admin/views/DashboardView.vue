<script setup>
// ============================================================
// Dashboard：运营 + 运维首屏
// 第一行：系统状态（● 正常 / 需要关注 + 最后检查时间 + 刷新）
// 第二行：今日推送 / 成功率 / 待处理 / 异常（4 KPI）
// 第三行：推送趋势（7 天）+ 实时任务（PROCESSING/PENDING/RETRY/FAILED）
// 第四行：最近异常（失败/重试事件流）
// 数据源：GET /api/admin/overview · 30s 自动刷新
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import StatCard from '../components/StatCard.vue'
import LineChart from '../components/LineChart.vue'
import EmptyState from '../components/EmptyState.vue'
import { adminGet } from '../api'
import { fmtNum, timeAgo, dayLabel } from '../utils/format'
import { t } from '../i18n'
import { tokens } from '../theme'

const router = useRouter()

const overview = ref(null)
const loading = ref(true)
const error = ref('')
const refreshing = ref(false)
const lastChecked = ref('')

// 图表色板跟随系统主题（theme.js tokens，响应式）
const COLORS = {
  primary: tokens.primary,
  green: tokens.success,
  amber: tokens.warning,
  red: tokens.error,
  blue: tokens.info,
  slate: '#94a3b8'
}

function nowTime() {
  return new Date().toTimeString().slice(0, 8)
}

async function load() {
  if (refreshing.value) return
  refreshing.value = true
  error.value = ''
  try {
    overview.value = await adminGet('/api/admin/overview')
    lastChecked.value = nowTime()
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

// ---- 派生 ----
const push = computed(() => overview.value?.push || {})
const today = computed(() => push.value?.today || {})
const trend = computed(() => overview.value?.trend || [])
const byStatus = computed(() => push.value?.tasks?.byStatus || {})

/** 系统是否正常：存在失败或重试任务即需要关注 */
const systemOk = computed(() => {
  const s = byStatus.value
  return !((s.FAILED || 0) > 0 || (s.RETRY || 0) > 0)
})

const trendLabels = computed(() => trend.value.map((x) => dayLabel(x.day)))
const trendSeries = computed(() => [
  { name: t('dash.sent'), color: COLORS.primary, values: trend.value.map((x) => x.sent) },
  { name: t('dash.success'), color: COLORS.green, values: trend.value.map((x) => x.success) }
])

const pendingTotal = computed(() => (byStatus.value.PENDING || 0) + (byStatus.value.PROCESSING || 0))
const exceptionTotal = computed(() => (byStatus.value.FAILED || 0) + (byStatus.value.RETRY || 0))

/** 实时任务（按关注度排序：处理中 / 排队 / 重试 / 失败） */
const liveTasks = computed(() => [
  { key: 'PROCESSING', n: byStatus.value.PROCESSING || 0, color: COLORS.blue },
  { key: 'PENDING', n: byStatus.value.PENDING || 0, color: COLORS.slate },
  { key: 'RETRY', n: byStatus.value.RETRY || 0, color: COLORS.amber },
  { key: 'FAILED', n: byStatus.value.FAILED || 0, color: COLORS.red }
])

/** 最近异常：事件流中的失败 / 重试项 */
const recentExceptions = computed(() =>
  (overview.value?.activity || []).filter((a) => a.status === 'FAILED' || a.status === 'RETRY').slice(0, 6)
)

const kpiLoading = computed(() => loading.value && !overview.value)
</script>

<template>
  <div>
    <!-- 第一行：系统状态 -->
    <div v-if="overview" class="a-system-banner" :class="{ warn: !systemOk }">
      <Icon :name="systemOk ? 'check' : 'alert-triangle'" :size="16" />
      <div>
        <b>{{ systemOk ? t('dash.allSystemsOk') : t('dash.attentionRequired') }}</b>
        <span v-if="!systemOk">{{ t('dash.pushAttention') }}</span>
      </div>
      <span class="checked">
        {{ t('dash.lastChecked') }} {{ lastChecked }}
      </span>
      <button class="a-btn a-btn-sm" style="margin-left: auto" :disabled="refreshing" @click="load">
        <Icon name="refresh" :size="13" :class="{ spin: refreshing }" />
        {{ refreshing ? t('common.refreshing') : t('common.refresh') }}
      </button>
    </div>

    <div v-if="error && !overview" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <!-- 第二行：4 KPI（今日推送 / 成功率 / 待处理 / 异常） -->
    <div class="a-grid a-grid-kpi" style="margin-bottom: 16px">
      <StatCard
        :label="t('dash.todayPush')"
        :value="fmtNum(today.total)"
        :sub="t('dash.todaySub', { ok: fmtNum(today.success), fail: fmtNum(today.failed) })"
        icon="send"
        :loading="kpiLoading"
      />
      <StatCard
        :label="t('dash.pushSuccessRate')"
        :value="today ? `${today.successRate}%` : '-'"
        :sub="t('dash.successRateSub', { n: fmtNum(today.total) })"
        icon="zap"
        :loading="kpiLoading"
      />
      <StatCard
        :label="t('dash.pending')"
        :value="fmtNum(pendingTotal)"
        :sub="t('dash.pendingSub', { p: fmtNum(byStatus.PENDING || 0), r: fmtNum(byStatus.PROCESSING || 0) })"
        icon="clock"
        :loading="kpiLoading"
      />
      <StatCard
        :label="t('dash.exceptions')"
        :value="fmtNum(exceptionTotal)"
        :sub="t('dash.exceptionsSub', { f: fmtNum(byStatus.FAILED || 0), r: fmtNum(byStatus.RETRY || 0) })"
        icon="alert-triangle"
        :tone="exceptionTotal > 0 ? 'error' : ''"
        :loading="kpiLoading"
      />
    </div>

    <!-- 第三行：推送趋势 + 实时任务 -->
    <div class="a-grid a-grid-main-side" style="margin-bottom: 16px">
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('dash.deliveryTrend') }}</h3>
            <div class="desc">{{ t('dash.deliveryTrendDesc') }}</div>
          </div>
          <span class="a-badge a-badge-slate">{{ t('dash.days7') }}</span>
        </div>
        <div class="a-card-body">
          <LineChart
            :series="trendSeries"
            :labels="trendLabels"
            :height="240"
            :loading="loading && !overview"
          />
        </div>
      </div>

      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('dash.realtimeTasks') }}</h3>
            <div class="desc">{{ t('dash.todayResults') }}</div>
          </div>
          <button class="a-btn-link" @click="router.push('/admin/push')">
            {{ t('dash.viewPushCenter') }} <Icon name="chevron-right" :size="13" />
          </button>
        </div>
        <div class="a-card-body" style="padding-top: 10px">
          <div v-if="kpiLoading" class="a-skeleton" style="height: 200px" />
          <div v-else class="a-live-tasks">
            <button
              v-for="task in liveTasks"
              :key="task.key"
              class="a-live-task"
              :class="{ warn: task.key === 'FAILED' && task.n > 0 }"
              @click="router.push('/admin/push')"
            >
              <span class="dot" :style="{ background: task.color }" />
              <span class="lbl">{{ t('status.' + task.key) }}</span>
              <b>{{ fmtNum(task.n) }}</b>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 第四行：最近异常 -->
    <div class="a-card">
      <div class="a-card-head">
        <div>
          <h3>{{ t('dash.recentExceptions') }}</h3>
          <div class="desc">{{ t('dash.latestEvents') }}</div>
        </div>
        <button class="a-btn-link" @click="router.push('/admin/push')">
          {{ t('dash.viewPushCenter') }} <Icon name="chevron-right" :size="13" />
        </button>
      </div>
      <div class="a-card-body" style="padding-top: 14px">
        <div v-if="kpiLoading" class="a-skeleton" style="height: 140px" />
        <EmptyState
          v-else-if="!recentExceptions.length"
          icon="check"
          :title="t('dash.noExceptions')"
          :message="t('dash.noExceptionsMsg')"
        />
        <div v-else class="a-timeline">
          <div v-for="(a, i) in recentExceptions" :key="i" class="a-timeline-item error">
            <div class="time">{{ timeAgo(a.ts) }}</div>
            <div class="title">{{ a.title }}</div>
            <div class="detail">{{ a.detail }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.a-system-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  background: var(--a-success-bg);
  border: 1px solid var(--a-success);
  color: var(--a-success-strong);
  font-size: 13.5px;
}
.a-system-banner.warn {
  background: var(--a-warning-bg);
  border-color: var(--a-warning);
  color: var(--a-warning-strong);
}
.a-system-banner b {
  font-weight: 650;
}
.a-system-banner span {
  margin-left: 6px;
  opacity: 0.85;
}
.a-system-banner .checked {
  margin-left: 14px;
  padding-left: 14px;
  border-left: 1px solid currentColor;
  opacity: 0.75;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 实时任务列表 */
.a-live-tasks {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.a-live-task {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 12px;
  border: 1px solid var(--a-border);
  border-radius: var(--a-r-btn);
  background: var(--a-card);
  font-family: inherit;
  font-size: 13px;
  color: var(--a-text);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.a-live-task:hover {
  border-color: var(--a-border-strong);
  background: var(--a-card-alt);
}
.a-live-task .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.a-live-task .lbl {
  font-weight: 550;
  color: var(--a-text-2);
}
.a-live-task b {
  margin-left: auto;
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.a-live-task.warn {
  border-color: var(--a-error);
  background: var(--a-error-bg);
}
.a-live-task.warn .lbl {
  color: var(--a-error-strong);
}
.a-live-task.warn b {
  color: var(--a-error-strong);
}

.clickable {
  cursor: pointer;
}
.clickable:hover {
  background: var(--a-card-alt);
}

.spin {
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
