<script setup>
// ============================================================
// 分类管理：GET /api/meta + PUT /api/meta/{key}
// 四大元数据：eventTypes / statuses / sourceLevels / comebackStages
// 注：eventTypes 变更会影响推送通知的类型标签（后端自动刷新）
// ============================================================
import { ref, computed, onMounted } from 'vue'
import Icon from '../components/Icon.vue'
import DrawerPanel from '../components/DrawerPanel.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import { adminApi } from '../api'
import { toast } from '../toast'
import { t } from '../i18n'

const meta = ref(null)
const error = ref('')
const saving = ref(false)

// ---- 编辑状态 ----
const editor = ref({ key: '', item: null, index: -1 }) // item: {id,label,marker} | {key,value} | {id,key}
const drawerOpen = ref(false)
const form = ref(null)
const confirm = ref({ open: false, title: '', fn: null })
const busy = ref(false)

async function load() {
  error.value = ''
  try {
    meta.value = await adminApi.get('/api/meta')
  } catch (e) {
    error.value = e.message
    meta.value = null
  }
}

onMounted(load)

const eventTypes = computed(() => meta.value?.eventTypes || [])
const statusEntries = computed(() => Object.entries(meta.value?.statuses || {}))
const sourceLevelEntries = computed(() => Object.entries(meta.value?.sourceLevels || {}))
const comebackStages = computed(() => meta.value?.comebackStages || [])

// ---- Event Types 编辑（数组型） ----
function openTypeCreate() {
  editor.value = { key: 'eventTypes', item: { id: '', label: { en: '', 'zh-CN': '', ko: '' }, marker: '●' }, index: -1 }
  form.value = JSON.parse(JSON.stringify(editor.value.item))
  drawerOpen.value = true
}

function openTypeEdit(item, index) {
  editor.value = { key: 'eventTypes', item, index }
  form.value = {
    id: item.id,
    label: { en: item.label?.en || '', 'zh-CN': item.label?.['zh-CN'] || '', ko: item.label?.ko || '' },
    marker: item.marker || '●'
  }
  drawerOpen.value = true
}

// ---- Statuses / SourceLevels 编辑（map 型） ----
function openMapCreate(key) {
  editor.value = { key, item: null, index: -1 }
  form.value = { key: '', value: '' }
  drawerOpen.value = true
}

function openMapEdit(k, v, index) {
  editor.value = { key: editor.value.key || 'statuses', item: null, index }
  form.value = { key: k, value: v }
  drawerOpen.value = true
}

// ---- Comeback Stages 编辑（数组型） ----
function openStageCreate() {
  editor.value = { key: 'comebackStages', item: null, index: -1 }
  form.value = { id: '', key: '' }
  drawerOpen.value = true
}

function openStageEdit(item, index) {
  editor.value = { key: 'comebackStages', item, index }
  form.value = { id: item.id, key: item.key }
  drawerOpen.value = true
}

// ---- 保存 ----
async function save() {
  if (saving.value) return
  const k = editor.value.key
  let next
  if (k === 'eventTypes') {
    if (!form.value.id.trim() || !form.value.label['zh-CN'].trim()) {
      toast.warning(t('meta.typeRequired')); return
    }
    next = JSON.parse(JSON.stringify(eventTypes.value))
    if (editor.value.index < 0) next.push(form.value)
    else next[editor.value.index] = form.value
  } else if (k === 'comebackStages') {
    if (!form.value.id.trim() || !form.value.key.trim()) {
      toast.warning(t('meta.stageRequired')); return
    }
    next = JSON.parse(JSON.stringify(comebackStages.value))
    if (editor.value.index < 0) next.push({ id: form.value.id, key: form.value.key })
    else next[editor.value.index] = { id: form.value.id, key: form.value.key }
  } else {
    // map 型
    if (!form.value.key.trim() || !form.value.value.trim()) {
      toast.warning(t('meta.mapRequired')); return
    }
    next = { ...(meta.value?.[k] || {}) }
    if (editor.value.index >= 0) delete next[editor.value.itemKey]
    next[form.value.key] = form.value.value
  }

  saving.value = true
  try {
    await adminApi.put(`/api/meta/${k}`, next)
    toast.success(t('meta.saved'))
    drawerOpen.value = false
    await load()
  } catch (e) {
    toast.error(e.message || t('meta.saveFailed'))
  } finally {
    saving.value = false
  }
}

// ---- 删除 ----
function askDelete(k, index, label) {
  confirm.value = { open: true, title: label, fn: () => removeItem(k, index) }
}

async function removeItem(k, index) {
  busy.value = true
  try {
    let next
    if (k === 'eventTypes' || k === 'comebackStages') {
      next = JSON.parse(JSON.stringify(k === 'eventTypes' ? eventTypes.value : comebackStages.value))
      next.splice(index, 1)
    } else {
      next = { ...(meta.value?.[k] || {}) }
      delete next[Object.keys(next)[index]]
    }
    await adminApi.put(`/api/meta/${k}`, next)
    toast.success(t('meta.deleted'))
    await load()
  } catch (e) {
    toast.error(e.message || t('meta.deleteFailed'))
  } finally {
    busy.value = false
    confirm.value.open = false
  }
}

const editorTitle = computed(() => {
  const k = editor.value.key
  const names = {
    eventTypes: ['meta.addType', 'meta.editType'],
    statuses: ['meta.addStatus', 'meta.editStatus'],
    sourceLevels: ['meta.addSource', 'meta.editSource'],
    comebackStages: ['meta.addStage', 'meta.editStage']
  }
  const pair = names[k] || ['common.add', 'common.edit']
  return t(editor.value.index < 0 ? pair[0] : pair[1])
})
</script>

<template>
  <div>
    <div class="a-page-head">
      <div>
        <h2>{{ t('meta.title') }}</h2>
        <div class="sub">{{ t('meta.sub') }}</div>
      </div>
      <div class="a-page-actions">
        <button class="a-btn" :disabled="!meta" @click="load">
          <Icon name="refresh" :size="14" /> {{ t('common.reload') }}
        </button>
      </div>
    </div>

    <div v-if="error && !meta" class="a-error-box">
      <Icon name="alert-triangle" :size="16" />
      <span>{{ error }}</span>
      <button class="a-btn a-btn-sm retry" @click="load">{{ t('common.retry') }}</button>
    </div>

    <div v-if="meta" class="a-grid a-grid-2" style="gap: 16px">
      <!-- Event Types -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('meta.eventTypes') }}</h3>
            <div class="desc">{{ t('meta.eventTypesDesc') }}</div>
          </div>
          <button class="a-btn-link" @click="openTypeCreate"><Icon name="plus" :size="13" /> {{ t('common.add') }}</button>
        </div>
        <div class="a-card-body" style="padding-top: 12px">
          <div class="a-table-wrap">
            <table class="a-table">
              <thead>
                <tr><th style="width: 44px">{{ t('meta.colMarker') }}</th><th>{{ t('meta.colId') }}</th><th>{{ t('meta.colZh') }}</th><th>{{ t('meta.colEn') }}</th><th>{{ t('meta.colKo') }}</th><th style="text-align: right">{{ t('common.actions') }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="(x, i) in eventTypes" :key="x.id">
                  <td style="font-size: 15px; color: var(--a-primary)">{{ x.marker }}</td>
                  <td class="a-mono" style="font-size: 12px">{{ x.id }}</td>
                  <td>{{ x.label?.['zh-CN'] }}</td>
                  <td class="a-cell-sub">{{ x.label?.en }}</td>
                  <td class="a-cell-sub">{{ x.label?.ko }}</td>
                  <td>
                    <div class="a-cell-actions" style="justify-content: flex-end">
                      <button class="a-icon-btn" :title="t('common.edit')" @click="openTypeEdit(x, i)"><Icon name="edit" :size="14" /></button>
                      <button class="a-icon-btn danger" :title="t('common.delete')" @click="askDelete('eventTypes', i, x.id)"><Icon name="trash" :size="14" /></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Statuses -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('meta.statuses') }}</h3>
            <div class="desc">{{ t('meta.statusesDesc') }}</div>
          </div>
          <button class="a-btn-link" @click="editor.key = 'statuses'; openMapCreate('statuses')"><Icon name="plus" :size="13" /> {{ t('common.add') }}</button>
        </div>
        <div class="a-card-body" style="padding-top: 12px">
          <div class="a-table-wrap">
            <table class="a-table">
              <thead>
                <tr><th>{{ t('meta.colKey') }}</th><th>{{ t('meta.colLabel') }}</th><th style="text-align: right">{{ t('common.actions') }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="([k, v], i) in statusEntries" :key="k">
                  <td class="a-mono" style="font-size: 12px">{{ k }}</td>
                  <td>{{ v }}</td>
                  <td>
                    <div class="a-cell-actions" style="justify-content: flex-end">
                      <button class="a-icon-btn" :title="t('common.edit')" @click="editor.key = 'statuses'; openMapEdit(k, v, i)"><Icon name="edit" :size="14" /></button>
                      <button class="a-icon-btn danger" :title="t('common.delete')" @click="askDelete('statuses', i, k)"><Icon name="trash" :size="14" /></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Source Levels -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('meta.sourceLevels') }}</h3>
            <div class="desc">{{ t('meta.sourceLevelsDesc') }}</div>
          </div>
          <button class="a-btn-link" @click="editor.key = 'sourceLevels'; openMapCreate('sourceLevels')"><Icon name="plus" :size="13" /> {{ t('common.add') }}</button>
        </div>
        <div class="a-card-body" style="padding-top: 12px">
          <div class="a-table-wrap">
            <table class="a-table">
              <thead>
                <tr><th>{{ t('meta.colKey') }}</th><th>{{ t('meta.colLabel') }}</th><th style="text-align: right">{{ t('common.actions') }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="([k, v], i) in sourceLevelEntries" :key="k">
                  <td class="a-mono" style="font-size: 12px">{{ k }}</td>
                  <td>{{ v }}</td>
                  <td>
                    <div class="a-cell-actions" style="justify-content: flex-end">
                      <button class="a-icon-btn" :title="t('common.edit')" @click="editor.key = 'sourceLevels'; openMapEdit(k, v, i)"><Icon name="edit" :size="14" /></button>
                      <button class="a-icon-btn danger" :title="t('common.delete')" @click="askDelete('sourceLevels', i, k)"><Icon name="trash" :size="14" /></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Comeback Stages -->
      <div class="a-card">
        <div class="a-card-head">
          <div>
            <h3>{{ t('meta.comebackStages') }}</h3>
            <div class="desc">{{ t('meta.comebackStagesDesc') }}</div>
          </div>
          <button class="a-btn-link" @click="openStageCreate"><Icon name="plus" :size="13" /> {{ t('common.add') }}</button>
        </div>
        <div class="a-card-body" style="padding-top: 12px">
          <div class="a-table-wrap">
            <table class="a-table">
              <thead>
                <tr><th>{{ t('meta.colId') }}</th><th>{{ t('meta.colKey') }}</th><th style="text-align: right">{{ t('common.actions') }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="(s, i) in comebackStages" :key="s.id">
                  <td class="a-mono" style="font-size: 12px">{{ s.id }}</td>
                  <td class="a-mono" style="font-size: 12px">{{ s.key }}</td>
                  <td>
                    <div class="a-cell-actions" style="justify-content: flex-end">
                      <button class="a-icon-btn" :title="t('common.edit')" @click="openStageEdit(s, i)"><Icon name="edit" :size="14" /></button>
                      <button class="a-icon-btn danger" :title="t('common.delete')" @click="askDelete('comebackStages', i, s.id)"><Icon name="trash" :size="14" /></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑抽屉（根据类型渲染不同表单） -->
    <DrawerPanel :open="drawerOpen" :title="editorTitle" :width="520" @close="drawerOpen = false">
      <template v-if="form">
        <!-- Event Type 表单 -->
        <div v-if="editor.key === 'eventTypes'" class="a-form-grid">
          <div class="a-field">
            <label>{{ t('meta.idLabel') }}</label>
            <input v-model="form.id" class="a-input" placeholder="RELEASE" />
            <div class="hint">{{ t('meta.idHint') }}</div>
          </div>
          <div class="a-field">
            <label>{{ t('meta.markerLabel') }}</label>
            <input v-model="form.marker" class="a-input" placeholder="●" maxlength="4" />
          </div>
          <div class="a-field full"><label>{{ t('meta.zhLabel') }}</label><input v-model="form.label['zh-CN']" class="a-input" placeholder="回归" /></div>
          <div class="a-field full"><label>{{ t('meta.enLabel') }}</label><input v-model="form.label.en" class="a-input" placeholder="RELEASE" /></div>
          <div class="a-field full"><label>{{ t('meta.koLabel') }}</label><input v-model="form.label.ko" class="a-input" placeholder="컴백" /></div>
        </div>

        <!-- Comeback Stage 表单 -->
        <div v-else-if="editor.key === 'comebackStages'" class="a-form-grid">
          <div class="a-field">
            <label>{{ t('meta.idLabel') }}</label>
            <input v-model="form.id" class="a-input" placeholder="concept-photo" />
          </div>
          <div class="a-field">
            <label>{{ t('meta.keyLabel') }}</label>
            <input v-model="form.key" class="a-input" placeholder="conceptPhoto" />
          </div>
        </div>

        <!-- Map 型表单（statuses / sourceLevels） -->
        <div v-else class="a-form-grid">
          <div class="a-field">
            <label>{{ t('meta.keyLabel') }}</label>
            <input v-model="form.key" class="a-input" placeholder="CONFIRMED" :disabled="editor.index >= 0" />
            <div v-if="editor.index >= 0" class="hint">{{ t('meta.keyHint') }}</div>
          </div>
          <div class="a-field">
            <label>{{ t('meta.valueLabel') }}</label>
            <input v-model="form.value" class="a-input" placeholder="CONFIRMED" />
          </div>
        </div>
      </template>

      <template #footer>
        <button class="a-btn" :disabled="saving" @click="drawerOpen = false">{{ t('common.cancel') }}</button>
        <button class="a-btn a-btn-primary" :disabled="saving" @click="save">
          <Icon v-if="saving" name="loader" :size="14" class="spin" />
          {{ t('common.save') }}
        </button>
      </template>
    </DrawerPanel>

    <ConfirmDialog
      :open="confirm.open"
      :title="t('meta.deleteTitle')"
      :message="t('meta.deleteMsg', { name: confirm.title })"
      :confirm-text="t('common.delete')"
      :loading="busy"
      @confirm="confirm.fn"
      @cancel="confirm.open = false"
    />
  </div>
</template>

<style scoped>
.a-icon-btn.danger:hover {
  color: var(--a-error);
}
.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
