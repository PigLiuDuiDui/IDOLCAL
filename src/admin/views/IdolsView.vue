<script setup>
// ============================================================
// 偶像管理：GET/POST/PUT/DELETE /api/artists（标准 CRUD）
// 列表 + 搜索/筛选 + 抽屉表单（含三语 intro）+ 删除二次确认
// ============================================================
import { ref, computed, onMounted } from 'vue'
import Icon from '../components/Icon.vue'
import StatusBadge from '../components/StatusBadge.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonTable from '../components/SkeletonTable.vue'
import DrawerPanel from '../components/DrawerPanel.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { adminApi } from '../api'
import { toast } from '../toast'
import { t } from '../i18n'

const artists = ref(null) // null = loading
const error = ref('')
const q = ref('')
const eraFilter = ref('')
const drawerOpen = ref(false)
const saving = ref(false)
const form = ref(emptyForm())
const editingId = ref(null)
const confirm = ref({ open: false, id: null, name: '' })
const deleting = ref(false)

const PAGE_SIZE = 10
const page = ref(0)

function emptyForm() {
  return {
    id: '',
    name: '',
    subName: '',
    year: '',
    era: '',
    eraPeriod: '',
    accent: '#a62f2f',
    accentSoft: '',
    heroImage: '',
    sourceTag: '',
    intro: { en: '', 'zh-CN': '', ko: '' },
    current: false
  }
}

async function load() {
  error.value = ''
  try {
    artists.value = await adminApi.get('/api/artists')
  } catch (e) {
    error.value = e.message
    artists.value = []
  }
}

onMounted(load)

// ---- 筛选与分页（数据量小，客户端处理） ----
const filtered = computed(() => {
  const list = artists.value || []
  const kw = q.value.trim().toLowerCase()
  return list.filter((a) => {
    if (kw && !(`${a.id} ${a.name} ${a.subName || ''}`.toLowerCase().includes(kw))) return false
    if (eraFilter.value && (a.era || '') !== eraFilter.value) return false
    return true
  })
})

const eras = computed(() => [...new Set((artists.value || []).map((a) => a.era).filter(Boolean))])
const paged = computed(() => {
  const start = page.value * PAGE_SIZE
  return filtered.value.slice(start, start + PAGE_SIZE)
})
const total = computed(() => filtered.value.length)

const kpis = computed(() => {
  const list = artists.value || []
  return {
    total: list.length,
    current: list.filter((a) => a.current).length,
    withIntro: list.filter((a) => a.intro && Object.values(a.intro).some((t) => t)).length
  }
})

// ---- 抽屉表单 ----
function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  drawerOpen.value = true
}

function openEdit(a) {
  editingId.value = a.id
  form.value = {
    id: a.id,
    name: a.name || '',
    subName: a.subName || '',
    year: a.year || '',
    era: a.era || '',
    eraPeriod: a.eraPeriod || '',
    accent: a.accent || '#a62f2f',
    accentSoft: a.accentSoft || '',
    heroImage: a.heroImage || '',
    sourceTag: a.sourceTag || '',
    intro: {
      en: a.intro?.en || '',
      'zh-CN': a.intro?.['zh-CN'] || '',
      ko: a.intro?.ko || ''
    },
    current: !!a.current
  }
  drawerOpen.value = true
}

async function save() {
  if (saving.value) return
  if (!form.value.name.trim() || !form.value.era.trim()) {
    toast.warning(t('idols.required'))
    return
  }
  if (!editingId.value && !form.value.id.trim()) {
    toast.warning(t('idols.idRequired'))
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value }
    if (editingId.value) payload.id = editingId.value
    if (!payload.accentSoft) delete payload.accentSoft
    if (!payload.heroImage) payload.heroImage = null
    if (editingId.value) {
      await adminApi.put(`/api/artists/${editingId.value}`, payload)
      toast.success(t('idols.updated'))
    } else {
      await adminApi.post('/api/artists', payload)
      toast.success(t('idols.created'))
    }
    drawerOpen.value = false
    await load()
  } catch (e) {
    toast.error(e.message || t('idols.saveFailed'))
  } finally {
    saving.value = false
  }
}

// ---- 删除 ----
function askDelete(a) {
  confirm.value = { open: true, id: a.id, name: a.name }
}

async function doDelete() {
  deleting.value = true
  try {
    await adminApi.del(`/api/artists/${confirm.value.id}`)
    toast.success(t('idols.deleted'))
    confirm.value.open = false
    await load()
  } catch (e) {
    toast.error(e.message || t('idols.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

const totalOnPage = computed(() => paged.value.length)
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('idols.title') }}</h2>
        <div class="sub">{{ t('idols.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn a-btn-primary" @click="openCreate">
          <Icon name="plus" :size="14" /> {{ t('idols.addIdol') }}
        </button>
      </div>
    </div>

    <!-- KPI -->
    <div class="a-grid a-grid-kpi" style="margin-bottom: 16px">
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('idols.total') }}</span>
          <span class="a-kpi-icon"><Icon name="star" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ kpis.total }}</div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('idols.currentFeatured') }}</span>
          <span class="a-kpi-icon"><Icon name="zap" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ kpis.current }}</div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('idols.withIntro') }}</span>
          <span class="a-kpi-icon"><Icon name="file-text" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ kpis.withIntro }}</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="a-filter-bar">
      <div class="a-search-box">
        <Icon name="search" :size="15" />
        <input v-model="q" class="a-input" :placeholder="t('idols.searchPh')" style="padding-left: 34px" />
      </div>
      <select v-model="eraFilter" class="a-select">
        <option value="">{{ t('idols.allEras') }}</option>
        <option v-for="e in eras" :key="e" :value="e">{{ e }}</option>
      </select>
    </div>

    <!-- 表格 -->
    <div class="a-card">
      <div class="a-card-body" style="padding-top: 12px">
        <SkeletonTable v-if="artists === null" :rows="4" :cols="5" />
        <div v-else-if="!filtered.length" class="a-table-wrap">
          <EmptyState
            icon="star"
            :title="t('idols.noFound')"
            :message="artists.length ? t('idols.adjustFilters') : t('idols.clickAdd')"
            :action-text="artists.length ? '' : t('idols.addIdol')"
            @action="openCreate"
          />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('idols.colIdol') }}</th>
                <th>{{ t('idols.colEra') }}</th>
                <th>{{ t('idols.colYear') }}</th>
                <th>{{ t('idols.colSource') }}</th>
                <th>{{ t('idols.colStatus') }}</th>
                <th style="text-align: right">{{ t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in paged" :key="a.id">
                <td>
                  <div style="display: flex; align-items: center; gap: 10px">
                    <span
                      class="a-idol-dot"
                      :style="{ background: a.accent || '#a62f2f' }"
                    >{{ (a.name || '?').charAt(0) }}</span>
                    <div>
                      <div class="a-cell-main">{{ a.name }}</div>
                      <div class="a-cell-sub">{{ a.id }}{{ a.subName ? ' · ' + a.subName : '' }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="a-cell-main">{{ a.era }}</div>
                  <div class="a-cell-sub">{{ a.eraPeriod || '-' }}</div>
                </td>
                <td>{{ a.year || '-' }}</td>
                <td>{{ a.sourceTag || '-' }}</td>
                <td><StatusBadge :status="a.current ? 'current' : 'ARCHIVED'" /></td>
                <td>
                  <div class="a-cell-actions" style="justify-content: flex-end">
                    <button class="a-icon-btn" :title="t('common.edit')" @click="openEdit(a)">
                      <Icon name="edit" :size="15" />
                    </button>
                    <button class="a-icon-btn danger" :title="t('common.delete')" @click="askDelete(a)">
                      <Icon name="trash" :size="15" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <PaginationBar :page="page" :size="PAGE_SIZE" :total="total" @change="page = $event" />
        </div>
      </div>
    </div>

    <!-- 抽屉表单 -->
    <DrawerPanel :open="drawerOpen" :title="editingId ? t('idols.editTitle', { id: editingId }) : t('idols.addTitle')" :width="640" @close="drawerOpen = false">
      <div class="a-form-grid">
        <div class="a-field">
          <label>{{ t('idols.idLabel') }}</label>
          <input v-model="form.id" class="a-input" :placeholder="t('idols.idPh')" :disabled="!!editingId" />
          <div v-if="!editingId" class="hint">{{ t('idols.idHint') }}</div>
        </div>
        <div class="a-field">
          <label>{{ t('idols.nameLabel') }}</label>
          <input v-model="form.name" class="a-input" placeholder="EVAN" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.subNameLabel') }}</label>
          <input v-model="form.subName" class="a-input" placeholder="OFFICIAL SCHEDULE" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.yearLabel') }}</label>
          <input v-model="form.year" class="a-input" placeholder="2026" maxlength="4" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.eraLabel') }}</label>
          <input v-model="form.era" class="a-input" placeholder="DEATH OF ME" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.eraPeriodLabel') }}</label>
          <input v-model="form.eraPeriod" class="a-input" placeholder="AUG — NOV 2026" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.accentLabel') }}</label>
          <div style="display: flex; gap: 8px; align-items: center">
            <input v-model="form.accent" type="color" style="width: 42px; height: 36px; padding: 2px; border: 1px solid var(--a-border); border-radius: 8px; background: #fff" />
            <input v-model="form.accent" class="a-input" placeholder="#a62f2f" />
          </div>
        </div>
        <div class="a-field">
          <label>{{ t('idols.accentSoftLabel') }}</label>
          <input v-model="form.accentSoft" class="a-input" placeholder="#efe7e4" />
        </div>
        <div class="a-field full">
          <label>{{ t('idols.heroImageLabel') }}</label>
          <input v-model="form.heroImage" class="a-input" :placeholder="t('idols.heroImagePh')" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.sourceTagLabel') }}</label>
          <input v-model="form.sourceTag" class="a-input" placeholder="Official" />
        </div>
        <div class="a-field">
          <label>{{ t('idols.currentLabel') }}</label>
          <label class="a-switch">
            <input v-model="form.current" type="checkbox" />
            <span class="a-switch-track" />
          </label>
          <div class="hint">{{ t('idols.currentHint') }}</div>
        </div>
      </div>

      <div style="margin-top: 18px; font-size: 12.5px; font-weight: 600; color: var(--a-text-2)">{{ t('idols.introSection') }}</div>
      <div class="a-form-grid" style="margin-top: 8px">
        <div class="a-field full">
          <label>English</label>
          <textarea v-model="form.intro.en" class="a-textarea" rows="3" placeholder="EN…" />
        </div>
        <div class="a-field full">
          <label>中文</label>
          <textarea v-model="form.intro['zh-CN']" class="a-textarea" rows="3" placeholder="中文…" />
        </div>
        <div class="a-field full">
          <label>한국어</label>
          <textarea v-model="form.intro.ko" class="a-textarea" rows="3" placeholder="KO…" />
        </div>
      </div>

      <template #footer>
        <button class="a-btn" :disabled="saving" @click="drawerOpen = false">{{ t('common.cancel') }}</button>
        <button class="a-btn a-btn-primary" :disabled="saving" @click="save">
          <Icon v-if="saving" name="loader" :size="14" class="spin" />
          {{ editingId ? t('common.saveChanges') : t('idols.createIdol') }}
        </button>
      </template>
    </DrawerPanel>

    <!-- 删除确认 -->
    <ConfirmDialog
      :open="confirm.open"
      :title="t('idols.deleteTitle')"
      :message="t('idols.deleteMsg', { name: confirm.name })"
      :confirm-text="t('common.delete')"
      :loading="deleting"
      @confirm="doDelete"
      @cancel="confirm.open = false"
    />
  </div>
</template>

<style scoped>
.a-idol-dot {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.a-icon-btn.danger:hover {
  color: var(--a-error);
}

.a-switch {
  position: relative;
  display: inline-flex;
  cursor: pointer;
}
.a-switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.a-switch-track {
  width: 38px;
  height: 22px;
  border-radius: 11px;
  background: var(--a-border-strong);
  transition: background 0.15s;
  position: relative;
}
.a-switch-track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: transform 0.15s;
}
.a-switch input:checked + .a-switch-track {
  background: var(--a-primary);
}
.a-switch input:checked + .a-switch-track::after {
  transform: translateX(16px);
}

.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
