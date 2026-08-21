<script setup>
// ============================================================
// 日历事件管理：GET/POST/PUT/DELETE /api/events
// List / Calendar 双视图 + 多维筛选 + 抽屉表单（三语字段）
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
import { fmtDate } from '../utils/format'
import { t } from '../i18n'

const events = ref(null) // null = loading
const error = ref('')
const view = ref('list') // list | calendar

// 筛选
const q = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const artistFilter = ref('')
const fromFilter = ref('')
const toFilter = ref('')

// 分页
const PAGE_SIZE = 15
const page = ref(0)

// 表单
const drawerOpen = ref(false)
const saving = ref(false)
const form = ref(emptyForm())
const editingId = ref(null)
const confirm = ref({ open: false, id: null, title: '' })
const deleting = ref(false)

// 元数据（类型/状态下拉）
const meta = ref(null)

// 日历视图
const calCursor = ref(new Date())
const calSelected = ref('')

function emptyForm() {
  return {
    id: '',
    artist: '',
    date: '',
    endDate: '',
    time: '',
    timezone: '',
    title: { en: '', 'zh-CN': '', ko: '' },
    type: 'RELEASE',
    status: 'CONFIRMED',
    location: { en: '', 'zh-CN': '', ko: '' },
    description: { en: '', 'zh-CN': '', ko: '' },
    image: '',
    sourceName: '',
    sourceUrl: '',
    isOfficial: true,
    onlineUrl: '',
    mapUrl: ''
  }
}

async function load() {
  error.value = ''
  try {
    const [ev, mt] = await Promise.all([
      adminApi.get('/api/events'),
      adminApi.get('/api/meta')
    ])
    events.value = ev
    meta.value = mt
  } catch (e) {
    error.value = e.message
    events.value = []
  }
}

onMounted(load)

// ---- 筛选 ----
const filtered = computed(() => {
  const list = events.value || []
  const kw = q.value.trim().toLowerCase()
  return list.filter((ev) => {
    if (kw) {
      const hay = `${ev.id} ${ev.artist} ${ev.title?.['zh-CN'] || ''} ${ev.title?.en || ''} ${ev.title?.ko || ''} ${ev.location?.['zh-CN'] || ''}`
      if (!hay.toLowerCase().includes(kw)) return false
    }
    if (typeFilter.value && ev.type !== typeFilter.value) return false
    if (statusFilter.value && ev.status !== statusFilter.value) return false
    if (artistFilter.value && ev.artist !== artistFilter.value) return false
    if (fromFilter.value && ev.date < fromFilter.value) return false
    return !(toFilter.value && ev.date > toFilter.value);

  })
})

const paged = computed(() => {
  const start = page.value * PAGE_SIZE
  return filtered.value.slice(start, start + PAGE_SIZE)
})

const eventTypes = computed(() => meta.value?.eventTypes || [])
const statuses = computed(() => Object.keys(meta.value?.statuses || {}))
const artists = computed(() => [...new Set((events.value || []).map((e) => e.artist).filter(Boolean))])

const typeLabel = (id) => {
  const t = (meta.value?.eventTypes || []).find((x) => x.id === id)
  return t?.label?.['zh-CN'] || t?.label?.en || id
}

const kpis = computed(() => {
  const list = events.value || []
  const now = new Date().toISOString().slice(0, 10)
  return {
    total: list.length,
    upcoming: list.filter((e) => e.date >= now && (e.status === 'CONFIRMED' || e.status === 'TBA')).length,
    official: list.filter((e) => e.isOfficial).length
  }
})

// ---- 日历视图 ----
const calYear = computed(() => calCursor.value.getFullYear())
const calMonth = computed(() => calCursor.value.getMonth()) // 0-based

const calGrid = computed(() => {
  const first = new Date(calYear.value, calMonth.value, 1)
  const startDow = first.getDay() // 0=Sun
  const daysInMonth = new Date(calYear.value, calMonth.value + 1, 0).getDate()
  const cells = []
  for (let i = 0; i < startDow; i++) cells.push({ blank: true })
  for (let d = 1; d <= daysInMonth; d++) {
    const iso = `${calYear.value}-${String(calMonth.value + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const dayEvents = filtered.value.filter((e) => e.date === iso)
    cells.push({
      blank: false,
      day: d,
      iso,
      events: dayEvents,
      highlight: dayEvents.length > 0,
      selected: calSelected.value === iso
    })
  }
  return cells
})

const calEvents = computed(() =>
  filtered.value.filter((e) => e.date === calSelected.value)
)

function calNav(dir) {
  calCursor.value = new Date(calYear.value, calMonth.value + dir, 1)
}

// ---- 抽屉 ----
function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  if (artists.value.length === 1) form.value.artist = artists.value[0]
  drawerOpen.value = true
}

function openEdit(ev) {
  editingId.value = ev.id
  form.value = {
    id: ev.id,
    artist: ev.artist || '',
    date: ev.date || '',
    endDate: ev.endDate || '',
    time: ev.time || '',
    timezone: ev.timezone || '',
    title: { en: ev.title?.en || '', 'zh-CN': ev.title?.['zh-CN'] || '', ko: ev.title?.ko || '' },
    type: ev.type || 'RELEASE',
    status: ev.status || 'CONFIRMED',
    location: { en: ev.location?.en || '', 'zh-CN': ev.location?.['zh-CN'] || '', ko: ev.location?.ko || '' },
    description: { en: ev.description?.en || '', 'zh-CN': ev.description?.['zh-CN'] || '', ko: ev.description?.ko || '' },
    image: ev.image || '',
    sourceName: ev.sourceName || '',
    sourceUrl: ev.sourceUrl || '',
    isOfficial: !!ev.isOfficial,
    onlineUrl: ev.onlineUrl || '',
    mapUrl: ev.mapUrl || ''
  }
  drawerOpen.value = true
}

async function save() {
  if (saving.value) return
  const f = form.value
  if (!f.artist.trim() || !f.date || !f.title['zh-CN'].trim() && !f.title.en.trim()) {
    toast.warning(t('events.required'))
    return
  }
  saving.value = true
  try {
    const payload = { ...f }
    if (editingId.value) payload.id = editingId.value
    if (!payload.endDate) delete payload.endDate
    if (!payload.time) delete payload.time
    if (!payload.image) payload.image = null
    if (!payload.onlineUrl) delete payload.onlineUrl
    if (!payload.mapUrl) delete payload.mapUrl
    if (!payload.sourceName) delete payload.sourceName
    if (!payload.sourceUrl) delete payload.sourceUrl
    if (editingId.value) {
      await adminApi.put(`/api/events/${editingId.value}`, payload)
      toast.success(t('events.updated'))
    } else {
      await adminApi.post('/api/events', payload)
      toast.success(t('events.created'))
    }
    drawerOpen.value = false
    await load()
  } catch (e) {
    toast.error(e.message || t('events.saveFailed'))
  } finally {
    saving.value = false
  }
}

function askDelete(ev) {
  confirm.value = { open: true, id: ev.id, title: ev.title?.['zh-CN'] || ev.title?.en || ev.id }
}

async function doDelete() {
  deleting.value = true
  try {
    await adminApi.del(`/api/events/${confirm.value.id}`)
    toast.success(t('events.deleted'))
    confirm.value.open = false
    await load()
  } catch (e) {
    toast.error(e.message || t('events.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

function fmtEventDate(ev) {
  let s = ev.date
  if (ev.endDate && ev.endDate !== ev.date) s += ` ~ ${ev.endDate}`
  if (ev.time) s += ` ${ev.time}`
  return s
}
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('events.title') }}</h2>
        <div class="sub">{{ t('events.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn a-btn-primary" @click="openCreate">
          <Icon name="plus" :size="14" /> {{ t('events.createEvent') }}
        </button>
      </div>
    </div>

    <!-- KPI -->
    <div class="a-grid a-grid-kpi" style="margin-bottom: 16px">
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('events.total') }}</span>
          <span class="a-kpi-icon"><Icon name="calendar" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ kpis.total }}</div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('events.upcoming') }}</span>
          <span class="a-kpi-icon"><Icon name="zap" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ kpis.upcoming }}</div>
      </div>
      <div class="a-kpi">
        <div class="a-kpi-top">
          <span class="a-kpi-label">{{ t('events.official') }}</span>
          <span class="a-kpi-icon"><Icon name="shield" :size="15" /></span>
        </div>
        <div class="a-kpi-value">{{ kpis.official }}</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="a-filter-bar" style="flex-wrap: wrap">
      <div class="a-search-box">
        <Icon name="search" :size="15" />
        <input v-model="q" class="a-input" :placeholder="t('events.searchPh')" style="padding-left: 34px" />
      </div>
      <select v-model="typeFilter" class="a-select">
        <option value="">{{ t('events.allTypes') }}</option>
        <option v-for="x in eventTypes" :key="x.id" :value="x.id">{{ typeLabel(x.id) }}</option>
      </select>
      <select v-model="statusFilter" class="a-select">
        <option value="">{{ t('events.allStatus') }}</option>
        <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
      </select>
      <select v-model="artistFilter" class="a-select">
        <option value="">{{ t('events.allArtists') }}</option>
        <option v-for="a in artists" :key="a" :value="a">{{ a }}</option>
      </select>
      <input v-model="fromFilter" type="date" class="a-input" style="width: 140px" :title="t('events.from')" />
      <input v-model="toFilter" type="date" class="a-input" style="width: 140px" :title="t('events.to')" />
      <div class="a-seg" style="margin-left: auto">
        <button class="a-seg-btn" :class="{ active: view === 'list' }" @click="view = 'list'">
          <Icon name="file-text" :size="13" /> {{ t('events.viewList') }}
        </button>
        <button class="a-seg-btn" :class="{ active: view === 'calendar' }" @click="view = 'calendar'">
          <Icon name="calendar" :size="13" /> {{ t('events.viewCalendar') }}
        </button>
      </div>
    </div>

    <div v-if="error && !events" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <!-- List 视图 -->
    <div v-if="view === 'list'" class="a-card">
      <div class="a-card-body" style="padding-top: 12px">
        <SkeletonTable v-if="events === null" :rows="5" :cols="6" />
        <div v-else-if="!filtered.length" class="a-table-wrap">
          <EmptyState
            icon="calendar"
            :title="t('events.noFound')"
            :message="events.length ? t('events.adjustFilters') : t('events.clickCreate')"
            :action-text="events.length ? '' : t('events.createEvent')"
            @action="openCreate"
          />
        </div>
        <div v-else class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>{{ t('events.colDate') }}</th>
                <th>{{ t('events.colEvent') }}</th>
                <th>{{ t('events.colArtist') }}</th>
                <th>{{ t('events.colType') }}</th>
                <th>{{ t('events.colStatus') }}</th>
                <th style="text-align: right">{{ t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ev in paged" :key="ev.id">
                <td>
                  <div class="a-cell-main">{{ fmtEventDate(ev) }}</div>
                  <div class="a-cell-sub">{{ ev.timezone || 'KST' }}</div>
                </td>
                <td style="max-width: 360px">
                  <div class="a-cell-main">{{ ev.title?.['zh-CN'] || ev.title?.en || ev.id }}</div>
                  <div class="a-cell-sub">
                    {{ ev.id }}
                    <template v-if="ev.location?.['zh-CN']"> · {{ ev.location['zh-CN'] }}</template>
                  </div>
                </td>
                <td>{{ ev.artist }}</td>
                <td>
                  <span class="a-badge a-badge-brand">{{ typeLabel(ev.type) }}</span>
                </td>
                <td>
                  <StatusBadge :status="ev.status" />
                  <span v-if="ev.isOfficial" class="a-badge a-badge-success" style="margin-left: 4px">{{ t('status.official') }}</span>
                </td>
                <td>
                  <div class="a-cell-actions" style="justify-content: flex-end">
                    <button class="a-icon-btn" :title="t('common.edit')" @click="openEdit(ev)">
                      <Icon name="edit" :size="15" />
                    </button>
                    <button class="a-icon-btn danger" :title="t('common.delete')" @click="askDelete(ev)">
                      <Icon name="trash" :size="15" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <PaginationBar :page="page" :size="PAGE_SIZE" :total="filtered.length" @change="page = $event" />
        </div>
      </div>
    </div>

    <!-- Calendar 视图 -->
    <div v-else-if="events !== null" class="a-grid a-grid-main-side">
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('events.calMonth', { year: calYear, month: calMonth + 1 }) }}</h3>
            <div class="desc">{{ t('events.calDesc', { n: filtered.length }) }}</div>
          </div>
          <div style="display: flex; gap: 6px">
            <button class="a-icon-btn" @click="calNav(-1)"><Icon name="chevron-left" :size="15" /></button>
            <button class="a-btn a-btn-sm" @click="calCursor = new Date()">{{ t('events.today') }}</button>
            <button class="a-icon-btn" @click="calNav(1)"><Icon name="chevron-right" :size="15" /></button>
          </div>
        </div>
        <div class="a-card-body">
          <div class="a-cal-grid">
            <div v-for="w in ['SUN','MON','TUE','WED','THU','FRI','SAT']" :key="w" class="a-cal-dow">{{ w }}</div>
            <div
              v-for="(c, i) in calGrid"
              :key="i"
              class="a-cal-cell"
              :class="{ blank: c.blank, highlight: c.highlight, selected: c.selected }"
              @click="c.blank ? null : (calSelected = c.iso)"
            >
              <span class="a-cal-day">{{ c.day }}</span>
              <span v-if="c.events.length" class="a-cal-count">{{ c.events.length }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ calSelected ? fmtDate(new Date(calSelected + 'T00:00:00').getTime()) : t('events.selectDay') }}</h3>
            <div class="desc">{{ t('events.dayEvents') }}</div>
          </div>
        </div>
        <div class="a-card-body" style="padding-top: 12px">
          <div v-if="!calSelected" class="a-empty" style="padding: 40px 0">
            <div class="a-empty-icon"><Icon name="calendar" :size="22" /></div>
            <p>{{ t('events.clickDay') }}</p>
          </div>
          <div v-else-if="!calEvents.length" class="a-empty" style="padding: 40px 0">
            <div class="a-empty-icon"><Icon name="check" :size="22" /></div>
            <p>{{ t('events.noEventsDay') }}</p>
          </div>
          <div v-else class="a-cal-event-list">
            <div v-for="ev in calEvents" :key="ev.id" class="a-cal-event">
              <div class="a-cal-event-time">{{ ev.time || '--:--' }}</div>
              <div class="a-cal-event-body">
                <div class="a-cell-main">{{ ev.title?.['zh-CN'] || ev.title?.en || ev.id }}</div>
                <div class="a-cell-sub">{{ ev.artist }} · {{ typeLabel(ev.type) }} · {{ ev.status }}</div>
                <div style="display: flex; gap: 6px; margin-top: 6px">
                  <button class="a-btn a-btn-sm" @click="openEdit(ev)"><Icon name="edit" :size="12" /> {{ t('common.edit') }}</button>
                  <button class="a-btn a-btn-sm a-btn-danger" @click="askDelete(ev)"><Icon name="trash" :size="12" /> {{ t('common.delete') }}</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 抽屉表单 -->
    <DrawerPanel :open="drawerOpen" :title="editingId ? t('events.editTitle', { id: editingId }) : t('events.addTitle')" :width="680" @close="drawerOpen = false">
      <div class="a-form-grid">
        <div class="a-field">
          <label>{{ t('events.artistLabel') }}</label>
          <input v-model="form.artist" class="a-input" list="a-artists" placeholder="EVAN" />
          <datalist id="a-artists">
            <option v-for="a in artists" :key="a" :value="a" />
          </datalist>
        </div>
        <div class="a-field">
          <label>{{ t('events.dateLabel') }}</label>
          <input v-model="form.date" type="date" class="a-input" />
        </div>
        <div class="a-field">
          <label>{{ t('events.endDateLabel') }}</label>
          <input v-model="form.endDate" type="date" class="a-input" />
        </div>
        <div class="a-field">
          <label>{{ t('events.timeLabel') }}</label>
          <input v-model="form.time" class="a-input" placeholder="19:00" />
        </div>
        <div class="a-field">
          <label>{{ t('events.timezoneLabel') }}</label>
          <input v-model="form.timezone" class="a-input" placeholder="Asia/Seoul" />
        </div>
        <div class="a-field">
          <label>{{ t('events.typeLabel') }}</label>
          <select v-model="form.type" class="a-select">
            <option v-for="x in eventTypes" :key="x.id" :value="x.id">{{ typeLabel(x.id) }}</option>
          </select>
        </div>
        <div class="a-field">
          <label>{{ t('events.statusLabel') }}</label>
          <select v-model="form.status" class="a-select">
            <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="a-field">
          <label>{{ t('events.officialLabel') }}</label>
          <label class="a-switch">
            <input v-model="form.isOfficial" type="checkbox" />
            <span class="a-switch-track" />
          </label>
        </div>
      </div>

      <div style="margin-top: 18px; font-size: 12.5px; font-weight: 600; color: var(--a-text-2)">{{ t('events.titleSection') }}</div>
      <div class="a-form-grid" style="margin-top: 8px">
        <div class="a-field full"><label>中文</label><input v-model="form.title['zh-CN']" class="a-input" placeholder="中文标题" /></div>
        <div class="a-field full"><label>English</label><input v-model="form.title.en" class="a-input" placeholder="English title" /></div>
        <div class="a-field full"><label>한국어</label><input v-model="form.title.ko" class="a-input" placeholder="한국어 제목" /></div>
      </div>

      <div style="margin-top: 18px; font-size: 12.5px; font-weight: 600; color: var(--a-text-2)">{{ t('events.locationSection') }}</div>
      <div class="a-form-grid" style="margin-top: 8px">
        <div class="a-field full"><label>{{ t('events.locationLabel') }}</label><input v-model="form.location['zh-CN']" class="a-input" placeholder="地点（中文）" /></div>
        <div class="a-field full"><label>{{ t('events.descriptionLabel') }}</label><textarea v-model="form.description['zh-CN']" class="a-textarea" rows="3" placeholder="描述（中文）" /></div>
      </div>

      <div style="margin-top: 18px; font-size: 12.5px; font-weight: 600; color: var(--a-text-2)">{{ t('events.sourceSection') }}</div>
      <div class="a-form-grid" style="margin-top: 8px">
        <div class="a-field"><label>{{ t('events.sourceNameLabel') }}</label><input v-model="form.sourceName" class="a-input" placeholder="Official X" /></div>
        <div class="a-field"><label>{{ t('events.sourceUrlLabel') }}</label><input v-model="form.sourceUrl" class="a-input" placeholder="https://…" /></div>
        <div class="a-field full"><label>{{ t('events.imageUrlLabel') }}</label><input v-model="form.image" class="a-input" placeholder="https://…" /></div>
        <div class="a-field"><label>{{ t('events.onlineUrlLabel') }}</label><input v-model="form.onlineUrl" class="a-input" placeholder="https://…" /></div>
        <div class="a-field"><label>{{ t('events.mapUrlLabel') }}</label><input v-model="form.mapUrl" class="a-input" placeholder="https://…" /></div>
      </div>

      <template #footer>
        <button class="a-btn" :disabled="saving" @click="drawerOpen = false">{{ t('common.cancel') }}</button>
        <button class="a-btn a-btn-primary" :disabled="saving" @click="save">
          <Icon v-if="saving" name="loader" :size="14" class="spin" />
          {{ editingId ? t('common.saveChanges') : t('events.createEventBtn') }}
        </button>
      </template>
    </DrawerPanel>

    <!-- 删除确认 -->
    <ConfirmDialog
      :open="confirm.open"
      :title="t('events.deleteTitle')"
      :message="t('events.deleteMsg', { title: confirm.title })"
      :confirm-text="t('common.delete')"
      :loading="deleting"
      @confirm="doDelete"
      @cancel="confirm.open = false"
    />
  </div>
</template>

<style scoped>
.a-seg {
  display: inline-flex;
  border: 1px solid var(--a-border);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.a-seg-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  font-size: 12.5px;
  font-weight: 550;
  color: var(--a-text-2);
  background: transparent;
  border: none;
  cursor: pointer;
}
.a-seg-btn:hover {
  color: var(--a-text);
}
.a-seg-btn.active {
  background: var(--a-primary-soft);
  color: var(--a-primary-ink);
}

/* 日历网格 */
.a-cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.a-cal-dow {
  text-align: center;
  font-size: 11px;
  font-weight: 650;
  color: var(--a-text-3);
  padding: 6px 0;
  letter-spacing: 0.05em;
}
.a-cal-cell {
  position: relative;
  height: 56px;
  border: 1px solid var(--a-border);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fff;
  transition: all 0.12s;
}
.a-cal-cell:hover {
  border-color: var(--a-border-strong);
}
.a-cal-cell.blank {
  border-color: transparent;
  background: transparent;
  cursor: default;
}
.a-cal-cell.highlight {
  background: var(--a-primary-soft);
  border-color: #e8cfca;
}
.a-cal-cell.selected {
  border-color: var(--a-primary);
  box-shadow: 0 0 0 1px var(--a-primary);
}
.a-cal-day {
  font-size: 13px;
  font-weight: 600;
}
.a-cal-count {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 3px;
  border-radius: 8px;
  background: var(--a-primary);
  color: #fff;
  font-size: 10px;
  font-weight: 650;
  display: flex;
  align-items: center;
  justify-content: center;
}

.a-cal-event-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.a-cal-event {
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--a-border);
  border-radius: 10px;
  background: var(--a-card-alt);
}
.a-cal-event-time {
  font-family: var(--a-mono);
  font-size: 12.5px;
  color: var(--a-primary-ink);
  font-weight: 650;
  flex-shrink: 0;
  padding-top: 2px;
}
.a-cal-event-body {
  flex: 1;
  min-width: 0;
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
