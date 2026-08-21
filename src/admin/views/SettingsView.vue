<script setup>
// ============================================================
// 系统设置：只读展示运行时配置（Configured via Environment）
// 数据源：GET /api/admin/system → config / database / quartz
// 后端无运行时修改接口，修改需改 application.yml 后重启
// 按「基础 / 推送 / 调度 / 安全 / 数据库」分组纵向展示
// ============================================================
import { ref, computed, onMounted } from 'vue'
import Icon from '../components/Icon.vue'
import { adminGet } from '../api'
import { fmtNum, fmtDateTime } from '../utils/format'
import { t } from '../i18n'

const sys = ref(null)
const loading = ref(true)
const error = ref('')

// 分组顺序（与用户信息架构一致）
const SECTION_IDS = ['general', 'push', 'scheduler', 'security', 'database']
const SECTION_TITLES = {
  general: 'settings.tabGeneral',
  push: 'settings.tabPush',
  scheduler: 'settings.tabScheduler',
  security: 'settings.tabSecurity',
  database: 'settings.tabDatabase'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    sys.value = await adminGet('/api/admin/system')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)

const config = computed(() => sys.value?.config || {})
const db = computed(() => sys.value?.database || {})
const quartz = computed(() => sys.value?.quartz || {})

// ---- 各分组配置项（只读） ----
const GROUPS = computed(() => ({
  general: [
    { label: t('settings.adminAccount'), value: config.value.adminUsername ?? '-', desc: t('settings.adminAccountDesc') },
    { label: t('settings.cacheEnabled'), value: String(config.value.cacheEnabled ?? '-'), desc: t('settings.cacheDesc') },
    { label: t('settings.rateLimit'), value: String(config.value.rateLimitEnabled ?? '-'), desc: t('settings.rateLimitDesc') }
  ],
  push: [
    { label: t('settings.sendConcurrency'), value: config.value.sendConcurrency ?? '-', desc: t('settings.sendConcurrencyDesc') },
    { label: t('settings.fanoutBatch'), value: config.value.fanoutBatchSize ?? '-', desc: t('settings.fanoutBatchDesc') },
    { label: t('settings.processingTimeout'), value: config.value.processingTimeoutMs != null ? `${Math.round(config.value.processingTimeoutMs / 1000)}s` : '-', desc: t('settings.processingTimeoutDesc') },
    { label: t('settings.logRetention'), value: config.value.deliveryLogRetentionDays != null ? t('settings.days', { n: config.value.deliveryLogRetentionDays }) : '-', desc: t('settings.logRetentionDesc') }
  ],
  scheduler: [
    { label: t('settings.schedulerState'), value: quartz.value.running ? t('mon.running') : quartz.value.standby ? t('mon.standby') : t('mon.stopped'), desc: t('settings.schedulerStateDesc') },
    { label: t('settings.registeredJobs'), value: fmtNum(quartz.value.jobs), desc: t('settings.registeredJobsDesc') },
    { label: t('settings.executedJobs'), value: fmtNum(quartz.value.executedJobs), desc: t('settings.executedJobsDesc') },
    { label: t('settings.nextFire'), value: quartz.value.nextFireAt ? fmtDateTime(quartz.value.nextFireAt) : '-', desc: t('settings.nextFireDesc') }
  ],
  security: [
    { label: t('settings.authEnabled'), value: String(config.value.authEnabled ?? '-'), desc: t('settings.authEnabledDesc') },
    { label: t('settings.jwtTtl'), value: config.value.jwtTtlHours != null ? t('settings.hours', { n: config.value.jwtTtlHours }) : '-', desc: t('settings.jwtTtlDesc') },
    { label: t('settings.loginMaxFailures'), value: config.value.loginMaxFailures ?? '-', desc: t('settings.loginMaxFailuresDesc') },
    { label: t('settings.loginLock'), value: config.value.loginLockMinutes != null ? t('settings.minutes', { n: config.value.loginLockMinutes }) : '-', desc: t('settings.loginLockDesc') }
  ],
  database: [
    { label: t('settings.dbStatus'), value: db.value.connected ? t('mon.connected') : t('mon.down'), desc: t('settings.dbStatusDesc') },
    { label: t('settings.dbActive'), value: `${fmtNum(db.value.active)} / ${fmtNum(db.value.total)}`, desc: t('settings.dbActiveDesc') },
    { label: t('settings.dbIdle'), value: fmtNum(db.value.idle), desc: t('settings.dbIdleDesc') },
    { label: t('settings.dbAwaiting'), value: fmtNum(db.value.awaiting), desc: t('settings.dbAwaitingDesc') },
    { label: t('settings.jdbcUrl'), value: db.value.url || '-', desc: t('settings.jdbcUrlDesc') }
  ]
}))
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('settings.title') }}</h2>
        <div class="sub">{{ t('settings.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn" :disabled="loading" @click="load">
          <Icon name="refresh" :size="14" :class="{ spin: loading }" /> {{ t('common.refresh') }}
        </button>
      </div>
    </div>

    <div v-if="error && !sys" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <!-- 只读说明 -->
    <div class="a-settings-note" style="margin-bottom: 20px">
      <Icon name="info" :size="14" />
      <span>
        {{ t('settings.noteBefore') }}
        <code class="a-mono">api/src/main/resources/application.yml</code>
        {{ t('settings.noteAfter') }}
      </span>
      <span class="a-badge a-badge-slate" style="margin-left: auto">{{ t('settings.readOnly') }}</span>
    </div>

    <!-- 分组配置（基础 / 推送 / 调度 / 安全 / 数据库） -->
    <div v-for="sid in SECTION_IDS" :key="sid" style="margin-bottom: 22px">
      <div class="a-settings-section-head">
        <h3>{{ t(SECTION_TITLES[sid]) }}</h3>
        <div class="desc">{{ t('settings.envDesc') }}</div>
      </div>
      <div class="a-card">
        <div class="a-card-body">
          <div v-if="loading && !sys" class="a-skeleton" style="height: 120px" />
          <div v-else>
            <div v-for="item in GROUPS[sid]" :key="item.label" class="a-settings-item">
              <div>
                <div class="name">{{ item.label }}</div>
                <div class="desc">{{ item.desc }}</div>
              </div>
              <code class="value a-mono">{{ item.value }}</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.a-settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 13px 4px;
  border-bottom: 1px solid var(--a-border);
}
.a-settings-item:last-child {
  border-bottom: none;
}
.a-settings-item .name {
  font-size: 13px;
  font-weight: 600;
  color: var(--a-text);
}
.a-settings-item .desc {
  font-size: 12px;
  color: var(--a-text-3);
  margin-top: 2px;
}
.a-settings-item .value {
  font-size: 12.5px;
  color: var(--a-text-2);
  background: var(--a-card-alt);
  border: 1px solid var(--a-border);
  border-radius: 6px;
  padding: 4px 10px;
  word-break: break-all;
  text-align: right;
  max-width: 60%;
}

.a-settings-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--a-info-bg);
  color: #1e40af;
  font-size: 12.5px;
}
.a-settings-note .a-mono {
  margin: 0 2px;
}

.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
