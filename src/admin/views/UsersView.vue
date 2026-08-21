<script setup>
// ============================================================
// 用户管理：账号分页 + 全局用户统计
// 数据源：GET /api/admin/users?q&page&size + GET /api/admin/overview
// 注：产品采用匿名设备模式，活跃用户以 Push 设备为锚点（见 devices 页）
// ============================================================
import { ref, computed, onMounted } from 'vue'
import Icon from '../components/Icon.vue'
import StatusBadge from '../components/StatusBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { adminGet } from '../api'
import { fmtNum, fmtDateTime } from '../utils/format'
import { t } from '../i18n'

const page = ref(0)
const size = 20
const q = ref('')
const data = ref(null)
const overview = ref(null)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size }
    if (q.value.trim()) params.q = q.value.trim()
    const [list, ov] = await Promise.all([
      adminGet('/api/admin/users', params),
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

function onSearch() {
  page.value = 0
  load()
}

const rows = computed(() => data.value?.content || [])
const users = computed(() => overview.value?.users || {})

const kpis = computed(() => [
  { label: t('users.totalUsers'), value: users.value.total, icon: 'users', sub: t('users.deviceAnchors') },
  { label: t('users.active24h'), value: users.value.active24h, icon: 'activity', sub: t('users.last24h') },
  { label: t('users.new7d'), value: users.value.new7d, icon: 'trending-up', sub: t('users.newThisWeek') },
  { label: t('users.pushEnabled'), value: users.value.pushEnabled, icon: 'smartphone', sub: t('users.activeSubs') }
])
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('users.title') }}</h2>
        <div class="sub">{{ t('users.sub') }}</div>
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

    <!-- 筛选 -->
    <div class="a-filter-bar">
      <div class="a-search-box">
        <Icon name="search" :size="15" />
        <input
          v-model="q"
          class="a-input"
          :placeholder="t('users.searchPh')"
          style="padding-left: 34px"
          @keydown.enter="onSearch"
        />
      </div>
      <button class="a-btn a-btn-sm" @click="onSearch">{{ t('common.search') }}</button>
      <span class="a-filter-hint">
        {{ t('users.hint', { n: data?.totalElements || 0 }) }}
      </span>
    </div>

    <div v-if="error && !data" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <div class="a-card">
      <div class="a-card-body" style="padding-top: 12px">
        <SkeletonTable v-if="loading && !data" :rows="5" :cols="5" />
        <div v-else-if="!rows.length" class="a-table-wrap">
          <EmptyState icon="users" :title="t('users.noFound')" :message="t('users.noFoundMsg')" />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('users.colId') }}</th>
                <th>{{ t('users.colUsername') }}</th>
                <th>{{ t('users.colRole') }}</th>
                <th>{{ t('users.colCreated') }}</th>
                <th>{{ t('users.colLastLogin') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in rows" :key="u.id">
                <td class="a-mono" style="font-size: 12px">#{{ u.id }}</td>
                <td>
                  <div style="display: flex; align-items: center; gap: 10px">
                    <div class="a-avatar" style="width: 28px; height: 28px; font-size: 11px">
                      {{ (u.username || '?').charAt(0).toUpperCase() }}
                    </div>
                    <b>{{ u.username || t('users.anonymous') }}</b>
                  </div>
                </td>
                <td><StatusBadge :status="u.role" /></td>
                <td>{{ fmtDateTime(u.createdAt) }}</td>
                <td>{{ fmtDateTime(u.lastLoginAt) }}</td>
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
</style>
