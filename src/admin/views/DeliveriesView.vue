<script setup>
// ============================================================
// 推送记录：投递日志查询
// 数据源：GET /api/admin/push/deliveries?result&q&page&size
// result: SUCCESS / FAILED / EXPIRED；q 模糊匹配 deviceId / endpoint
// ============================================================
import { ref, computed, onMounted } from 'vue'
import Icon from '../components/Icon.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { adminGet } from '../api'
import { fmtDateTime, timeAgo } from '../utils/format'
import { t } from '../i18n'

const page = ref(0)
const size = 20
const resultFilter = ref('')
const q = ref('')
const data = ref(null)
const loading = ref(true)
const error = ref('')

const RESULT_OPTS = ['SUCCESS', 'FAILED', 'EXPIRED']

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size }
    if (resultFilter.value) params.result = resultFilter.value
    if (q.value.trim()) params.q = q.value.trim()
    data.value = await adminGet('/api/admin/push/deliveries', params)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onFilter() {
  page.value = 0
  load()
}

const rows = computed(() => data.value?.content || [])

const resultBadge = (r) =>
  r === 'SUCCESS' ? 'a-badge-success' : r === 'FAILED' ? 'a-badge-error' : 'a-badge-warning'

const endpointShort = (ep) => {
  if (!ep) return '-'
  try {
    const u = new URL(ep)
    return `${u.hostname}${u.pathname.length > 24 ? u.pathname.slice(0, 24) + '…' : u.pathname}`
  } catch {
    return ep.length > 40 ? ep.slice(0, 40) + '…' : ep
  }
}
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('del.title') }}</h2>
        <div class="sub">{{ t('del.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn" :disabled="loading" @click="load">
          <Icon name="refresh" :size="14" :class="{ spin: loading }" /> {{ t('common.refresh') }}
        </button>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="a-filter-bar">
      <div class="a-search-box">
        <Icon name="search" :size="15" />
        <input
          v-model="q"
          class="a-input"
          :placeholder="t('del.searchPh')"
          style="padding-left: 34px"
          @keydown.enter="onFilter"
        />
      </div>
      <select v-model="resultFilter" class="a-select" @change="onFilter">
        <option value="">{{ t('del.allResults') }}</option>
        <option v-for="r in RESULT_OPTS" :key="r" :value="r">{{ r }}</option>
      </select>
      <button class="a-btn a-btn-sm" @click="onFilter">{{ t('common.search') }}</button>
      <span class="a-filter-hint">{{ t('del.hint', { n: data?.totalElements || 0 }) }}</span>
    </div>

    <div v-if="error && !data" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <div class="a-card">
      <div class="a-card-body" style="padding-top: 12px">
        <SkeletonTable v-if="loading && !data" :rows="8" :cols="5" />
        <div v-else-if="!rows.length" class="a-table-wrap">
          <EmptyState
            icon="file-text"
            :title="t('del.noLogs')"
            :message="t('del.noLogsMsg')"
          />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('del.colSentAt') }}</th>
                <th>{{ t('del.colDevice') }}</th>
                <th>{{ t('del.colEndpoint') }}</th>
                <th>{{ t('del.colHttp') }}</th>
                <th>{{ t('del.colResult') }}</th>
                <th>{{ t('del.colError') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in rows" :key="d.id">
                <td>
                  <div class="a-cell-main">{{ fmtDateTime(d.sentAt, true) }}</div>
                  <div class="a-cell-sub">{{ timeAgo(d.sentAt) }}</div>
                </td>
                <td>
                  <div class="a-cell-main a-mono" style="font-size: 12px">{{ d.deviceId }}</div>
                  <div class="a-cell-sub">{{ t('del.logId', { id: d.id }) }}</div>
                </td>
                <td>
                  <div class="a-cell-sub" :title="d.endpoint">{{ endpointShort(d.endpoint) }}</div>
                </td>
                <td>
                  <span class="a-mono" :class="d.httpStatus >= 200 && d.httpStatus < 300 ? 'ok' : d.httpStatus === 0 ? 'zero' : 'bad'">
                    {{ d.httpStatus || t('del.none') }}
                  </span>
                </td>
                <td>
                  <span class="a-badge" :class="resultBadge(d.result)">{{ d.result }}</span>
                </td>
                <td style="max-width: 260px">
                  <div v-if="d.errorMessage" class="a-cell-sub" style="color: var(--a-error)" :title="d.errorMessage">
                    {{ d.errorMessage }}
                  </div>
                  <span v-else class="a-cell-sub">-</span>
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
.a-filter-hint {
  font-size: 12.5px;
  color: var(--a-text-2);
  margin-left: auto;
  white-space: nowrap;
}
.ok { color: var(--a-success); font-weight: 600; }
.bad { color: var(--a-error); font-weight: 600; }
.zero { color: var(--a-warning); font-weight: 600; }
.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
