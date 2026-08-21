<script setup>
// ============================================================
// Push Task Detail：调度详情 + 事件信息 + 发送时间线 + 设备任务明细
// 数据源：GET /api/admin/push/schedules/{id} + GET /api/admin/push/tasks?scheduleId=
// ============================================================
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ProgressBar from '../components/ProgressBar.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { adminGet } from '../api'
import { fmtNum, fmtDateTime, fmtTime, timeAgo } from '../utils/format'
import { t } from '../i18n'

const route = useRoute()
const router = useRouter()

const detail = ref(null)
const tasks = ref(null)
const loading = ref(true)
const error = ref('')
const taskStatus = ref('')
const page = ref(0)
const PAGE_SIZE = 15

const id = computed(() => route.params.id)

async function loadDetail() {
  try {
    detail.value = await adminGet(`/api/admin/push/schedules/${id.value}`)
  } catch (e) {
    error.value = e.message
  }
}

async function loadTasks() {
  try {
    const params = { scheduleId: id.value, page: page.value, size: PAGE_SIZE }
    if (taskStatus.value) params.status = taskStatus.value
    tasks.value = await adminGet('/api/admin/push/tasks', params)
  } catch (e) {
    if (!error.value) error.value = e.message
  }
}

onMounted(() => {
  Promise.all([loadDetail(), loadTasks()]).finally(() => (loading.value = false))
})

watch(id, () => {
  loading.value = true
  error.value = ''
  detail.value = null
  tasks.value = null
  Promise.all([loadDetail(), loadTasks()]).finally(() => (loading.value = false))
})

function onStatusChange() {
  page.value = 0
  loadTasks()
}

// ---- 派生 ----
const s = computed(() => detail.value?.schedule || {})
const ev = computed(() => detail.value?.event || {})
const timeline = computed(() => detail.value?.timeline || [])

const progressTone = computed(() =>
  s.value.status === 'SUCCESS' ? 'success' : s.value.status === 'FAILED' ? 'error' : s.value.status === 'RETRY' ? 'warning' : 'brand'
)

const infoRows = computed(() => [
  { label: t('pdetail.scheduleId'), value: `#${s.value.id}`, mono: true },
  { label: t('pdetail.eventId'), value: s.value.eventId || '-', mono: true },
  { label: t('pdetail.offset'), value: s.value.offsetMinutes === 0 ? t('common.onTime') : t('common.minBefore', { n: s.value.offsetMinutes }) },
  { label: t('pdetail.created'), value: fmtDateTime(s.value.createdAt) },
  { label: t('pdetail.triggerAt'), value: fmtDateTime(s.value.triggerAt) },
  { label: t('pdetail.processedAt'), value: fmtDateTime(s.value.processedAt) },
  { label: t('pdetail.finishedAt'), value: fmtDateTime(s.value.finishedAt) },
  { label: t('pdetail.retryCount'), value: s.value.retryCount ?? '-' },
  { label: t('pdetail.nextRetry'), value: fmtDateTime(s.value.nextRetryAt) }
])

const TASK_STATUS_OPTS = ['PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'RETRY']

const taskRows = computed(() => tasks.value?.content || [])
</script>

<template>
  <div>
    <div class="a-page-head">
      <div style="display: flex; align-items: center; gap: 10px">
        <button class="a-icon-btn" :title="t('pdetail.back')" @click="router.push('/admin/push')">
          <Icon name="arrow-left" :size="16" />
        </button>
        <div>
          <h2>{{ s.eventTitle || t('pdetail.defaultTitle') }} <span class="a-mono" style="font-size: 13px; color: var(--a-text-3)">#{{ s.id }}</span></h2>
          <div class="sub">{{ ev.title }} · {{ ev.artist }} · {{ ev.date }} {{ ev.time }} {{ ev.timezone }}</div>
        </div>
      </div>
      <div class="a-page-actions">
        <StatusBadge v-if="s.status" :status="s.status" />
      </div>
    </div>

    <div v-if="error && !detail" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="loadDetail">{{ t('common.retry') }}</button>
    </div>

    <template v-if="detail">
      <!-- 顶部：事件卡 + 调度信息 -->
      <div class="a-grid a-grid-main-side" style="margin-bottom: 16px">
        <!-- 事件信息 -->
        <div class="a-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('pdetail.event') }}</h3>
              <div class="desc">{{ t('pdetail.eventDesc') }}</div>
            </div>
            <span v-if="ev.isOfficial" class="a-badge a-badge-success">{{ t('status.official') }}</span>
          </div>
          <div class="a-card-body">
            <div class="a-event-hero">
              <div class="a-event-title">{{ ev.title }}</div>
              <div class="a-event-en">{{ ev.titleEn }}</div>
              <div class="a-event-meta">
                <span class="a-badge a-badge-brand">{{ ev.type }}</span>
                <span class="a-badge" :class="ev.status === 'CONFIRMED' ? 'a-badge-success' : ev.status === 'TBA' ? 'a-badge-warning' : 'a-badge-slate'">{{ ev.status }}</span>
              </div>
              <div class="a-event-datetime">
                <Icon name="calendar" :size="14" /> {{ ev.date }} {{ ev.time || '' }}
                <span class="a-cell-sub">{{ ev.timezone || 'KST' }}</span>
              </div>
              <div class="a-event-artist"><Icon name="star" :size="14" /> {{ ev.artist }}</div>
            </div>
          </div>
        </div>

        <!-- 调度信息 -->
        <div class="a-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('pdetail.schedule') }}</h3>
              <div class="desc">{{ t('pdetail.scheduleDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body" style="padding-top: 10px">
            <div class="a-stat-list">
              <div v-for="r in infoRows" :key="r.label" class="a-stat-item">
                <span class="label">{{ r.label }}</span>
                <span class="value" :class="{ 'a-mono': r.mono }" style="font-size: 12.5px">{{ r.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 进度 + 时间线 -->
      <div class="a-grid a-grid-main-side" style="margin-bottom: 16px">
        <!-- 进度总览 -->
        <div class="a-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('pdetail.progress') }}</h3>
              <div class="desc">{{ t('pdetail.progressDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body" style="padding-top: 12px">
            <ProgressBar :value="s.progress" :tone="progressTone" />
            <div class="a-progress-stats">
              <div class="a-progress-stat">
                <b class="ok">{{ fmtNum(s.success) }}</b>
                <span>{{ t('pdetail.success') }}</span>
              </div>
              <div class="a-progress-stat">
                <b class="bad">{{ fmtNum(s.failed) }}</b>
                <span>{{ t('pdetail.failed') }}</span>
              </div>
              <div class="a-progress-stat">
                <b class="warn">{{ fmtNum(s.expired) }}</b>
                <span>{{ t('pdetail.expired') }}</span>
              </div>
              <div class="a-progress-stat">
                <b>{{ fmtNum(s.pending) }}</b>
                <span>{{ t('pdetail.pending') }}</span>
              </div>
              <div class="a-progress-stat">
                <b>{{ fmtNum(s.target) }}</b>
                <span>{{ t('pdetail.target') }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 发送时间线 -->
        <div class="a-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('pdetail.timeline') }}</h3>
              <div class="desc">{{ t('pdetail.timelineDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body" style="padding-top: 12px">
            <div v-if="!timeline.length" class="a-empty" style="padding: 36px 0">
              <div class="a-empty-icon"><Icon name="clock" :size="22" /></div>
              <p>{{ t('pdetail.noBatches') }}</p>
            </div>
            <div v-else class="a-timeline" style="max-height: 300px; overflow-y: auto">
              <div v-for="(b, i) in timeline" :key="i" class="a-timeline-item" :class="b.failed > 0 ? 'warning' : 'success'">
                <div class="time">{{ fmtTime(b.ts) }} <span class="a-cell-sub">{{ t('pdetail.batch', { n: b.batch }) }}</span></div>
                <div class="title">
                  <b class="ok">{{ fmtNum(b.success) }}</b> {{ t('pdetail.success') }}
                  <template v-if="b.failed"> · <b class="bad">{{ fmtNum(b.failed) }}</b> {{ t('pdetail.failed') }}</template>
                  <template v-if="b.expired"> · <b class="warn">{{ fmtNum(b.expired) }}</b> {{ t('pdetail.expired') }}</template>
                </div>
                <div class="detail">{{ timeAgo(b.ts) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 设备任务明细 -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('pdetail.deviceTasks') }}</h3>
            <div class="desc">{{ t('pdetail.deviceTasksDesc', { n: tasks?.totalElements || 0 }) }}</div>
          </div>
          <select v-model="taskStatus" class="a-select" style="width: 160px" @change="onStatusChange">
            <option value="">{{ t('push.allStatus') }}</option>
            <option v-for="st in TASK_STATUS_OPTS" :key="st" :value="st">{{ st }}</option>
          </select>
        </div>
        <div class="a-card-body" style="padding-top: 12px">
          <SkeletonTable v-if="loading && !tasks" :rows="5" :cols="5" />
          <div v-else-if="!taskRows.length" class="a-table-wrap">
            <EmptyState icon="smartphone" :title="t('pdetail.noDeviceTasks')" :message="t('pdetail.noDeviceTasksMsg')" />
          </div>
          <div v-else class="a-table-wrap">
            <table class="a-table">
              <thead>
                <tr>
                  <th>{{ t('pdetail.colDevice') }}</th>
                  <th>{{ t('pdetail.colStatus') }}</th>
                  <th>{{ t('pdetail.colAttempts') }}</th>
                  <th>{{ t('pdetail.colCreated') }}</th>
                  <th>{{ t('pdetail.colSentAt') }}</th>
                  <th>{{ t('pdetail.colError') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in taskRows" :key="task.id">
                  <td>
                    <div class="a-cell-main a-mono" style="font-size: 12px">{{ task.deviceId }}</div>
                    <div class="a-cell-sub">{{ t('pdetail.taskId', { id: task.id }) }}</div>
                  </td>
                  <td><StatusBadge :status="t.status" /></td>
                  <td>
                    <div class="a-cell-main">{{ task.retryCount }}</div>
                    <div v-if="task.nextRetryAt" class="a-cell-sub">{{ t('pdetail.retry', { ago: timeAgo(task.nextRetryAt) }) }}</div>
                  </td>
                  <td>{{ fmtDateTime(task.createdAt) }}</td>
                  <td>{{ fmtDateTime(task.sentAt) }}</td>
                  <td style="max-width: 240px">
                    <div v-if="task.errorMessage" class="a-cell-sub" style="color: var(--a-error)" :title="task.errorMessage">
                      {{ task.errorMessage }}
                    </div>
                    <span v-else class="a-cell-sub">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <PaginationBar :page="page" :size="PAGE_SIZE" :total="tasks?.totalElements || 0" @change="page = $event; loadTasks()" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.a-event-hero {
  padding: 4px 0;
}
.a-event-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--a-text);
}
.a-event-en {
  font-size: 12.5px;
  color: var(--a-text-3);
  margin-top: 2px;
}
.a-event-meta {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}
.a-event-datetime,
.a-event-artist {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--a-text-2);
  margin-top: 10px;
  margin-right: 14px;
}

.a-progress-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-top: 14px;
}
.a-progress-stat {
  text-align: center;
  padding: 10px 4px;
  border: 1px solid var(--a-border);
  border-radius: 10px;
  background: var(--a-card-alt);
}
.a-progress-stat b {
  display: block;
  font-size: 16px;
  font-weight: 700;
}
.a-progress-stat span {
  font-size: 11.5px;
  color: var(--a-text-3);
}
.a-progress-stat .ok { color: var(--a-success); }
.a-progress-stat .bad { color: var(--a-error); }
.a-progress-stat .warn { color: var(--a-warning); }
</style>
