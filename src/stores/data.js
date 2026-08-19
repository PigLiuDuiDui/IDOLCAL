// ============================================================
// 数据 Store：后端 API 唯一数据源，本地 src/data/*.js 为离线兜底
// 启动时 loadAll() 拉取 /api/{events,artists,comebacks,tutorials,meta}；
// 后端不可用时（source === 'local'）回退到本地快照数据。
// 管理端（后端 CRUD）修改数据后调用 refresh() 重新拉取。
// ============================================================
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  events as localEvents,
  EVENT_TYPES as LOCAL_EVENT_TYPES,
  TYPE_MARKER as LOCAL_TYPE_MARKER,
  TYPE_LABEL as LOCAL_TYPE_LABEL,
  STATUS as LOCAL_STATUS,
  SOURCE_LEVELS as LOCAL_SOURCE_LEVELS
} from '../data/events'
import { artists as localArtists } from '../data/artists'
import { comebacks as localComebacks, COMEBACK_STAGES as LOCAL_COMEBACK_STAGES } from '../data/comebacks'
import { tutorialBoards as localTutorials } from '../data/tutorials'

/** API 基础路径：dev 走 Vite proxy（/api → 8080），生产可配 VITE_API_BASE */
const API_BASE = import.meta.env.VITE_API_BASE || ''

async function getJson(path) {
  const res = await fetch(`${API_BASE}${path}`)
  if (!res.ok) throw new Error(`API ${path} → ${res.status}`)
  return res.json()
}

export const useDataStore = defineStore('data', () => {
  // ---- 状态（初始为本地快照，拉取成功后替换）----
  const events = ref(localEvents)
  const artists = ref(localArtists)
  const comebacks = ref(localComebacks)
  const tutorials = ref(localTutorials)
  const eventTypes = ref(LOCAL_EVENT_TYPES)
  const statuses = ref(LOCAL_STATUS)
  const sourceLevels = ref(LOCAL_SOURCE_LEVELS)
  const comebackStages = ref(LOCAL_COMEBACK_STAGES)

  /** 'api' 数据来自后端 / 'local' 为本地兜底 */
  const source = ref('local')
  const loading = ref(false)
  const error = ref(null)

  // ---- 派生数据（与组件原用的导出保持同名，改动面最小）----

  /** 活动按 日期 → 时间 升序 */
  const eventsSorted = computed(() =>
    [...events.value].sort((a, b) => {
      const ta = a.time || '00:00'
      const tb = b.time || '00:00'
      return a.date === b.date ? ta.localeCompare(tb) : a.date.localeCompare(b.date)
    })
  )

  const TYPE_MARKER = computed(() => {
    if (source.value === 'api') {
      return Object.fromEntries(eventTypes.value.map((t) => [t.id, t.marker]))
    }
    return LOCAL_TYPE_MARKER
  })

  const TYPE_LABEL = computed(() => {
    if (source.value === 'api') {
      return Object.fromEntries(eventTypes.value.map((t) => [t.id, t.label]))
    }
    return LOCAL_TYPE_LABEL
  })

  /** 当前展示艺人（后端 current 标记；本地兜底为第一个） */
  const currentArtist = computed(() => artists.value.find((a) => a.current) || artists.value[0] || null)

  /** 当前回归专题（列表第一个；本地兜底同构） */
  const currentComeback = computed(() => comebacks.value[0] || null)

  const readyBoards = computed(() => tutorials.value.filter((b) => b.status === 'ready'))
  const comingBoards = computed(() => tutorials.value.filter((b) => b.status === 'coming'))

  function getEventById(id) {
    return events.value.find((e) => e.id === id) || null
  }

  function getArtist(id) {
    return artists.value.find((a) => a.id === id) || currentArtist.value
  }

  function getComeback(id) {
    return comebacks.value.find((c) => c.id === id) || currentComeback.value
  }

  // ---- 数据拉取 ----

  /** 全量拉取；任一接口失败则整体回退本地（保持页面可用） */
  async function loadAll() {
    if (loading.value) return
    loading.value = true
    error.value = null
    try {
      const [ev, ar, cb, tu, meta] = await Promise.all([
        getJson('/api/events'),
        getJson('/api/artists'),
        getJson('/api/comebacks'),
        getJson('/api/tutorials'),
        getJson('/api/meta')
      ])
      events.value = ev
      artists.value = ar
      comebacks.value = cb
      tutorials.value = tu
      if (meta.eventTypes) eventTypes.value = meta.eventTypes
      if (meta.statuses) statuses.value = meta.statuses
      if (meta.sourceLevels) sourceLevels.value = meta.sourceLevels
      if (meta.comebackStages) comebackStages.value = meta.comebackStages
      source.value = 'api'
    } catch (e) {
      error.value = e
      source.value = 'local'
      console.warn('[data] API 不可用，使用本地快照数据：', e.message)
    } finally {
      loading.value = false
    }
  }

  return {
    events,
    eventsSorted,
    artists,
    comebacks,
    tutorials,
    /** 别名：与旧版 src/data/tutorials 的 tutorialBoards 同名 */
    tutorialBoards: tutorials,
    eventTypes,
    statuses,
    sourceLevels,
    comebackStages,
    TYPE_MARKER,
    TYPE_LABEL,
    currentArtist,
    currentComeback,
    readyBoards,
    comingBoards,
    getEventById,
    getArtist,
    getComeback,
    source,
    loading,
    error,
    loadAll
  }
})
