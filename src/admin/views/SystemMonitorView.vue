<script setup>
// ============================================================
// 系统监控：JVM / Push 线程池 / 数据库连接池 / Quartz / 运行时配置
// 数据源：GET /api/admin/system · 30s 自动刷新
// ============================================================
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import Icon from '../components/Icon.vue'
import { adminGet } from '../api'
import { fmtBytes, fmtUptime, fmtDateTime, fmtNum, timeAgo } from '../utils/format'
import { t } from '../i18n'

const sys = ref(null)
const loading = ref(true)
const error = ref('')
const refreshing = ref(false)

async function load() {
  if (refreshing.value) return
  refreshing.value = true
  error.value = ''
  try {
    sys.value = await adminGet('/api/admin/system')
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

const jvm = computed(() => sys.value?.jvm || {})
const worker = computed(() => sys.value?.pushWorker || {})
const db = computed(() => sys.value?.database || {})
const quartz = computed(() => sys.value?.quartz || {})
const config = computed(() => sys.value?.config || {})

// 内存使用率（used / max）
const memPct = computed(() => {
  if (!jvm.value.maxMemory) return 0
  return Math.round((jvm.value.usedMemory / jvm.value.maxMemory) * 100)
})

// 线程池使用率（active / poolSize）
const poolPct = computed(() => {
  if (!worker.value.poolSize) return 0
  return Math.round((worker.value.active / worker.value.poolSize) * 100)
})

const systemOnline = computed(() => !!(quartz.value.running && db.value.connected))

const meterTone = (pct) => (pct >= 85 ? 'error' : pct >= 60 ? 'warning' : 'success')

const CONFIG_ITEMS = computed(() => [
  { label: t('mon.authEnabled'), value: String(config.value.authEnabled ?? '-') },
  { label: t('mon.adminAccount'), value: config.value.adminUsername ?? '-' },
  { label: t('mon.jwtTtl'), value: config.value.jwtTtlHours != null ? `${config.value.jwtTtlHours}h` : '-' },
  { label: t('mon.loginMaxFailures'), value: config.value.loginMaxFailures ?? '-' },
  { label: t('mon.loginLock'), value: config.value.loginLockMinutes != null ? `${config.value.loginLockMinutes}m` : '-' },
  { label: t('mon.rateLimit'), value: String(config.value.rateLimitEnabled ?? '-') },
  { label: t('mon.cache'), value: String(config.value.cacheEnabled ?? '-') },
  { label: t('mon.sendConcurrency'), value: config.value.sendConcurrency ?? '-' },
  { label: t('mon.fanoutBatch'), value: config.value.fanoutBatchSize ?? '-' },
  { label: t('mon.processingTimeout'), value: config.value.processingTimeoutMs != null ? `${Math.round(config.value.processingTimeoutMs / 1000)}s` : '-' },
  { label: t('mon.logRetention'), value: config.value.deliveryLogRetentionDays != null ? `${config.value.deliveryLogRetentionDays}d` : '-' }
])
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('mon.title') }}</h2>
        <div class="sub">{{ t('mon.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn" :disabled="refreshing" @click="load">
          <Icon name="refresh" :size="14" :class="{ spin: refreshing }" />
          {{ refreshing ? t('common.refreshing') : t('common.refresh') }}
        </button>
      </div>
    </div>

    <div v-if="sys" class="a-system-banner" :class="{ warn: !systemOnline }">
      <Icon :name="systemOnline ? 'check' : 'alert-triangle'" :size="16" />
      <div>
        <b>{{ systemOnline ? t('mon.online') : t('mon.degraded') }}</b>
        <span v-if="!systemOnline">{{ t('mon.degradedMsg') }}</span>
      </div>
    </div>

    <div v-if="error && !sys" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <div v-if="sys" class="a-grid a-grid-2" style="gap: 16px">
      <!-- JVM -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('mon.jvm') }}</h3>
            <div class="desc">{{ t('mon.jvmDesc') }}</div>
          </div>
          <span class="a-kpi-icon"><Icon name="cpu" :size="15" /></span>
        </div>
        <div class="a-card-body">
          <div class="a-meter">
            <div class="a-meter-head">
              <span>{{ t('mon.heapUsage') }}</span>
              <b class="val">{{ memPct }}%</b>
            </div>
            <div class="a-progress-track" style="height: 8px">
              <div class="a-progress-fill" :class="meterTone(memPct)" :style="{ width: `${memPct}%` }" />
            </div>
            <div class="a-meter-foot">
              <span>{{ t('mon.used', { n: fmtBytes(jvm.usedMemory) }) }}</span>
              <span>{{ t('mon.total', { n: fmtBytes(jvm.totalMemory) }) }}</span>
              <span class="cap">{{ t('mon.max', { n: fmtBytes(jvm.maxMemory) }) }}</span>
            </div>
          </div>
          <div class="a-stat-list" style="margin-top: 14px">
            <div class="a-stat-item">
              <span class="label">{{ t('mon.threads') }}</span>
              <span class="value">{{ fmtNum(jvm.threads) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.cpuCores') }}</span>
              <span class="value">{{ fmtNum(jvm.cpuCores) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.uptime') }}</span>
              <span class="value">{{ fmtUptime(jvm.uptimeMs) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.startedAt') }}</span>
              <span class="value" style="font-size: 12.5px">{{ fmtDateTime(jvm.startTimeMs) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Push Worker -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('mon.pushWorker') }}</h3>
            <div class="desc">{{ t('mon.pushWorkerDesc') }}</div>
          </div>
          <span class="a-kpi-icon"><Icon name="send" :size="15" /></span>
        </div>
        <div class="a-card-body">
          <div class="a-meter">
            <div class="a-meter-head">
              <span>{{ t('mon.poolUtil') }}</span>
              <b class="val">{{ poolPct }}%</b>
            </div>
            <div class="a-progress-track" style="height: 8px">
              <div class="a-progress-fill" :class="meterTone(poolPct)" :style="{ width: `${poolPct}%` }" />
            </div>
            <div class="a-meter-foot">
              <span>{{ t('mon.active', { n: worker.active }) }}</span>
              <span class="cap">{{ t('mon.poolSize', { n: worker.poolSize }) }}</span>
            </div>
          </div>
          <div class="a-stat-list" style="margin-top: 14px">
            <div class="a-stat-item">
              <span class="label">{{ t('mon.corePool') }}</span>
              <span class="value">{{ fmtNum(worker.corePoolSize) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.queue') }}</span>
              <span class="value">{{ fmtNum(worker.queueSize) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.completed') }}</span>
              <span class="value">{{ fmtNum(worker.completed) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Database -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('mon.database') }}</h3>
            <div class="desc">{{ t('mon.databaseDesc') }}</div>
          </div>
          <span class="a-kpi-icon"><Icon name="database" :size="15" /></span>
        </div>
        <div class="a-card-body">
          <div class="a-db-status">
            <span v-if="db.connected" class="a-badge a-badge-success"><span class="dot" />{{ t('mon.connected') }}</span>
            <span v-else class="a-badge a-badge-error"><span class="dot" />{{ t('mon.down') }}</span>
            <code class="a-mono a-db-url">{{ db.url }}</code>
          </div>
          <div class="a-grid-2" style="display: grid; gap: 8px; margin-top: 12px">
            <div class="a-progress-stat">
              <b>{{ fmtNum(db.active) }}</b><span>{{ t('mon.activeShort') }}</span>
            </div>
            <div class="a-progress-stat">
              <b>{{ fmtNum(db.idle) }}</b><span>{{ t('mon.idle') }}</span>
            </div>
            <div class="a-progress-stat">
              <b>{{ fmtNum(db.total) }}</b><span>{{ t('mon.totalShort') }}</span>
            </div>
            <div class="a-progress-stat">
              <b>{{ fmtNum(db.awaiting) }}</b><span>{{ t('mon.awaiting') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Quartz -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('mon.quartz') }}</h3>
            <div class="desc">{{ t('mon.quartzDesc') }}</div>
          </div>
          <span class="a-kpi-icon"><Icon name="clock" :size="15" /></span>
        </div>
        <div class="a-card-body">
          <div class="a-db-status">
            <span v-if="quartz.running" class="a-badge a-badge-success"><span class="dot" />{{ t('mon.running') }}</span>
            <span v-else-if="quartz.standby" class="a-badge a-badge-warning"><span class="dot" />{{ t('mon.standby') }}</span>
            <span v-else class="a-badge a-badge-error"><span class="dot" />{{ t('mon.stopped') }}</span>
            <span v-if="quartz.error" class="a-cell-sub" style="color: var(--a-error)">{{ quartz.error }}</span>
          </div>
          <div class="a-stat-list" style="margin-top: 14px">
            <div class="a-stat-item">
              <span class="label">{{ t('mon.registeredJobs') }}</span>
              <span class="value">{{ fmtNum(quartz.jobs) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.executedJobs') }}</span>
              <span class="value">{{ fmtNum(quartz.executedJobs) }}</span>
            </div>
            <div class="a-stat-item">
              <span class="label">{{ t('mon.nextFire') }}</span>
              <span class="value" style="font-size: 12.5px">
                {{ quartz.nextFireAt ? fmtDateTime(quartz.nextFireAt) : '-' }}
              </span>
            </div>
            <div v-if="quartz.nextFireAt" class="a-stat-item">
              <span class="label">{{ t('mon.countdown') }}</span>
              <span class="value">{{ timeAgo(quartz.nextFireAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 运行时配置 -->
    <div v-if="sys" class="a-card" style="margin-top: 16px">
      <div class="a-card-head">
        <div>
          <h3>{{ t('mon.runtimeConfig') }}</h3>
          <div class="desc">{{ t('mon.runtimeConfigDesc') }}</div>
        </div>
      </div>
      <div class="a-card-body">
        <div class="a-grid-2" style="display: grid; gap: 2px 32px">
          <div v-for="c in CONFIG_ITEMS" :key="c.label" class="a-settings-item">
            <span class="name">{{ c.label }}</span>
            <span class="value a-mono">{{ c.value }}</span>
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
  border: 1px solid #bbf7d0;
  color: #166534;
  font-size: 13.5px;
}
.a-system-banner.warn {
  background: var(--a-warning-bg);
  border-color: #fde68a;
  color: #92400e;
}
.a-system-banner b {
  font-weight: 650;
}

.a-meter-head {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--a-text-2);
  margin-bottom: 6px;
}
.a-meter-head .val {
  color: var(--a-text);
  font-size: 14px;
}
.a-meter-foot {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--a-text-3);
  margin-top: 5px;
}
.a-meter-foot .cap {
  color: var(--a-text-2);
}

.a-db-status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.a-db-url {
  font-size: 11px;
  color: var(--a-text-2);
  background: var(--a-card-alt);
  border: 1px solid var(--a-border);
  border-radius: 6px;
  padding: 4px 8px;
  word-break: break-all;
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
  font-size: 15px;
  font-weight: 700;
}
.a-progress-stat span {
  font-size: 11.5px;
  color: var(--a-text-3);
}

.a-settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--a-border);
}
.a-settings-item:last-child {
  border-bottom: none;
}
.a-settings-item .name {
  font-size: 12.5px;
  color: var(--a-text-2);
}
.a-settings-item .value {
  font-size: 12.5px;
  color: var(--a-text);
}

.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
