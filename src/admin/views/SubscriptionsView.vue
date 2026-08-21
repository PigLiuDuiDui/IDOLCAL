<script setup>
// ============================================================
// 订阅管理：Push 订阅（浏览器投递地址）分页
// 数据源：GET /api/admin/subscriptions?page&size + GET /api/admin/overview
// ============================================================
import { ref, computed, onMounted } from 'vue'
import Icon from '../components/Icon.vue'
import StatusBadge from '../components/StatusBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { adminGet } from '../api'
import { fmtNum, fmtDateTime, timeAgo } from '../utils/format'
import { t } from '../i18n'

const page = ref(0)
const size = 20
const data = ref(null)
const overview = ref(null)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, ov] = await Promise.all([
      adminGet('/api/admin/subscriptions', { page: page.value, size }),
      adminGet('/api/admin/overview')
    ])
    data.value = list
    overview.value = ov
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)

const rows = computed(() => data.value?.content || [])

const endpointHost = (ep) => {
  if (!ep) return '-'
  try {
    return new URL(ep).hostname
  } catch {
    return ep.length > 36 ? ep.slice(0, 36) + '…' : ep
  }
}

const kpis = computed(() => [
  { label: t('subs.subscriptions'), value: data.value?.totalElements || 0, icon: 'link', sub: t('subs.pushEndpoints') },
  { label: t('subs.pushEnabled'), value: overview.value?.users?.pushEnabled || 0, icon: 'smartphone', sub: t('subs.distinctDevices') },
  { label: t('subs.orphaned'), value: rows.value.filter((r) => r.status === 'ORPHAN').length, icon: 'alert-triangle', sub: t('subs.orphanHint') }
])
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('subs.title') }}</h2>
        <div class="sub">{{ t('subs.sub') }}</div>
      </div>
    </div>

    <!-- KPI -->
    <div class="a-grid a-grid-kpi" style="margin-bottom: 16px">
      <div v-for="k in kpis" :key="k.label" class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ k.label }}</span>
          <span class="a-kpi-icon"><Icon :name="k.icon" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ fmtNum(k.value) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ k.sub }}</span></div>
      </div>
    </div>

    <div v-if="error && !data" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <div class="a-card">
      <div class="a-card-body" style="padding-top: 12px">
        <SkeletonTable v-if="loading && !data" :rows="6" :cols="6" />
        <div v-else-if="!rows.length" class="a-table-wrap">
          <EmptyState
            icon="link"
            :title="t('subs.noSubs')"
            :message="t('subs.noSubsMsg')"
          />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('subs.colId') }}</th>
                <th>{{ t('subs.colDevice') }}</th>
                <th>{{ t('subs.colEndpoint') }}</th>
                <th>{{ t('subs.colPlatform') }}</th>
                <th>{{ t('subs.colStatus') }}</th>
                <th>{{ t('subs.colCreated') }}</th>
                <th>{{ t('subs.colLastActive') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in rows" :key="s.id">
                <td class="a-mono" style="font-size: 12px">#{{ s.id }}</td>
                <td>
                  <div class="a-cell-main a-mono" style="font-size: 12px">{{ s.deviceId }}</div>
                </td>
                <td>
                  <div class="a-cell-sub" :title="s.endpoint">{{ endpointHost(s.endpoint) }}</div>
                </td>
                <td>{{ s.platform || '-' }}</td>
                <td><StatusBadge :status="s.status" /></td>
                <td>{{ fmtDateTime(s.createdAt) }}</td>
                <td>
                  <div class="a-cell-main">{{ timeAgo(s.lastActiveAt) }}</div>
                  <div class="a-cell-sub">{{ fmtDateTime(s.lastActiveAt) }}</div>
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
