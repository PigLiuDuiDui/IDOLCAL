<script setup>
// ============================================================
// Push 设备：匿名用户锚点管理
// 数据源：GET /api/admin/devices?q&status&page&size
// 写操作：DELETE /api/admin/devices/{deviceId}（单设备）
//         POST /api/admin/devices/clean-expired（批量清理 EXPIRED，危险操作二次确认）
// 状态语义：ACTIVE=有订阅；EXPIRED=无订阅且 90 天未活动；INACTIVE=无订阅但近期活跃
// ============================================================
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import StatusBadge from '../components/StatusBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import PaginationBar from '../components/PaginationBar.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { adminApi, adminGet } from '../api'
import { toast } from '../toast'
import { fmtNum, fmtDateTime, timeAgo } from '../utils/format'
import { t } from '../i18n'

const route = useRoute()
const router = useRouter()

const page = ref(0)
const size = 20
const q = ref('')
const statusFilter = ref(route.query.status || '')
const data = ref(null)
const loading = ref(true)
const error = ref('')

// 状态计数（q 为空时准确）
const counts = ref({ ACTIVE: null, INACTIVE: null, EXPIRED: null })

async function loadCounts() {
  if (q.value.trim()) return
  const [active, inactive, expired] = await Promise.all([
    adminGet('/api/admin/devices', { status: 'ACTIVE', page: 0, size: 1 }),
    adminGet('/api/admin/devices', { status: 'INACTIVE', page: 0, size: 1 }),
    adminGet('/api/admin/devices', { status: 'EXPIRED', page: 0, size: 1 })
  ])
  counts.value = {
    ACTIVE: active.totalElements,
    INACTIVE: inactive.totalElements,
    EXPIRED: expired.totalElements
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size }
    if (q.value.trim()) params.q = q.value.trim()
    if (statusFilter.value) params.status = statusFilter.value
    data.value = await adminGet('/api/admin/devices', params)
    await loadCounts()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onSearch() {
  page.value = 0
  load()
}

function onStatusChange() {
  page.value = 0
  router.replace({ path: '/admin/devices', query: statusFilter.value ? { status: statusFilter.value } : {} })
  load()
}

const rows = computed(() => data.value?.content || [])
const total = computed(() => data.value?.totalElements || 0)

// ---- 删除 ----
const delConfirm = ref({ open: false, deviceId: '' })
const deleting = ref(false)

function askDelete(d) {
  delConfirm.value = { open: true, deviceId: d.deviceId }
}

async function doDelete() {
  deleting.value = true
  try {
    await adminApi.del(`/api/admin/devices/${delConfirm.value.deviceId}`)
    toast.success(t('dev.deleted'))
    delConfirm.value.open = false
    await load()
  } catch (e) {
    toast.error(e.message || t('dev.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

// ---- 批量清理 ----
const cleanConfirm = ref({ open: false })
const cleaning = ref(false)

function askClean() {
  if (!(counts.value.EXPIRED > 0)) {
    toast.info(t('dev.noExpiredNow'))
    return
  }
  cleanConfirm.value = { open: true }
}

async function doClean() {
  cleaning.value = true
  try {
    const res = await adminApi.post('/api/admin/devices/clean-expired', {})
    toast.success(t('dev.cleaned', { n: res.removed }))
    cleanConfirm.value = false
    await load()
  } catch (e) {
    toast.error(e.message || t('dev.cleanFailed'))
  } finally {
    cleaning.value = false
  }
}
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('dev.title') }}</h2>
        <div class="sub">{{ t('dev.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn a-btn-danger" :disabled="loading" @click="askClean">
          <Icon name="trash" :size="14" />
          {{ t('dev.cleanExpired') }}
          <span v-if="counts.EXPIRED > 0" class="a-badge a-badge-error" style="margin-left: 6px">{{ counts.EXPIRED }}</span>
        </button>
      </div>
    </div>

    <!-- KPI -->
    <div class="a-grid a-grid-kpi" style="margin-bottom: 16px">
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('dev.totalDevices') }}</span>
          <span class="a-kpi-icon"><Icon name="smartphone" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ fmtNum(total) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ q ? t('common.filteredResult') : t('common.allDevices') }}</span></div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('dev.active') }}</span>
          <span class="a-kpi-icon"><Icon name="check" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ counts.ACTIVE === null ? '-' : fmtNum(counts.ACTIVE) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('dev.hasSubscription') }}</span></div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('dev.inactive') }}</span>
          <span class="a-kpi-icon"><Icon name="clock" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ counts.INACTIVE === null ? '-' : fmtNum(counts.INACTIVE) }}</div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('dev.recentlyActive') }}</span></div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('dev.expired') }}</span>
          <span class="a-kpi-icon"><Icon name="alert-triangle" :size="15" /></span>
        </div>
        <div class="a-kpi-value" :style="counts.EXPIRED > 0 ? { color: 'var(--a-warning)' } : {}">
          {{ counts.EXPIRED === null ? '-' : fmtNum(counts.EXPIRED) }}
        </div>
        <div class="a-kpi-foot"><span class="a-trend flat">{{ t('dev.cleanable') }}</span></div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="a-filter-bar">
      <div class="a-search-box">
        <Icon name="search" :size="15" />
        <input
          v-model="q"
          class="a-input"
          :placeholder="t('dev.searchPh')"
          style="padding-left: 34px"
          @keydown.enter="onSearch"
        />
      </div>
      <select v-model="statusFilter" class="a-select" @change="onStatusChange">
        <option value="">{{ t('dev.allStatus') }}</option>
        <option value="ACTIVE">ACTIVE ({{ counts.ACTIVE === null ? '-' : counts.ACTIVE }})</option>
        <option value="INACTIVE">INACTIVE ({{ counts.INACTIVE === null ? '-' : counts.INACTIVE }})</option>
        <option value="EXPIRED">EXPIRED ({{ counts.EXPIRED === null ? '-' : counts.EXPIRED }})</option>
      </select>
      <button class="a-btn a-btn-sm" @click="onSearch">{{ t('common.search') }}</button>
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
            :icon="statusFilter === 'EXPIRED' ? 'check' : 'smartphone'"
            :title="statusFilter === 'EXPIRED' ? t('dev.noExpired') : t('dev.noDevices')"
            :message="statusFilter === 'EXPIRED' ? t('dev.noExpiredMsg') : t('dev.adjustFilters')"
          />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('dev.colDevice') }}</th>
                <th>{{ t('dev.colUserId') }}</th>
                <th>{{ t('dev.colSubs') }}</th>
                <th>{{ t('dev.colStatus') }}</th>
                <th>{{ t('dev.colCreated') }}</th>
                <th>{{ t('dev.colLastActive') }}</th>
                <th style="text-align: right">{{ t('dev.colActions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in rows" :key="d.id">
                <td>
                  <div class="a-cell-main a-mono" style="font-size: 12px">{{ d.deviceId }}</div>
                  <div class="a-cell-sub">{{ d.platform || t('dev.unknownPlatform') }}</div>
                </td>
                <td class="a-mono" style="font-size: 12px">{{ d.userId || '-' }}</td>
                <td>
                  <span v-if="d.subscriptions > 0" class="a-badge a-badge-info">{{ d.subscriptions }}</span>
                  <span v-else class="a-cell-sub">-</span>
                </td>
                <td><StatusBadge :status="d.status" /></td>
                <td>{{ fmtDateTime(d.createdAt) }}</td>
                <td>
                  <div class="a-cell-main">{{ timeAgo(d.lastActiveAt) }}</div>
                  <div class="a-cell-sub">{{ fmtDateTime(d.lastActiveAt) }}</div>
                </td>
                <td>
                  <div class="a-cell-actions" style="justify-content: flex-end">
                    <button class="a-icon-btn danger" :title="t('dev.deleteBtn')" @click="askDelete(d)">
                      <Icon name="trash" :size="15" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <PaginationBar :page="page" :size="size" :total="total" @change="page = $event; load()" />
        </div>
      </div>
    </div>

    <!-- 单设备删除确认 -->
    <ConfirmDialog
      :open="delConfirm.open"
      :title="t('dev.deleteTitle')"
      :message="t('dev.deleteMsg', { id: delConfirm.deviceId })"
      :confirm-text="t('common.delete')"
      :loading="deleting"
      @confirm="doDelete"
      @cancel="delConfirm.open = false"
    />

    <!-- 批量清理确认 -->
    <ConfirmDialog
      :open="cleanConfirm.open"
      :title="t('dev.cleanTitle')"
      :message="t('dev.cleanMsg', { n: counts.EXPIRED || 0 })"
      :confirm-text="t('dev.cleanAll')"
      :loading="cleaning"
      @confirm="doClean"
      @cancel="cleanConfirm.open = false"
    />
  </div>
</template>
