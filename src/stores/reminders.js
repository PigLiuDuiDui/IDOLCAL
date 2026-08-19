import { defineStore } from 'pinia'
import { useDataStore } from './data'
import { REMINDER_OFFSETS, getReminderInstant } from '../utils/time'

// ============================================================
// 提醒状态管理（浏览器本地，为未来 PWA / Web Push 预留字段）
// 数据结构：
// {
//   [eventId]: {
//     eventId, offset: '1d'|'3h'|'1h'|'30m'|'start',
//     createdAt, channel: 'browser',   // 未来：'push'
//     notified: false,                 // 未来：是否已触发过通知
//     subscriptionId: null             // 未来：Web Push 订阅标识
//   }
// }
// ============================================================

const STORAGE_KEY = 'idolcal-reminders'

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}
  } catch {
    return {}
  }
}

// 事件查询走数据 Store（API 数据源；本地快照兜底）
function eventOf(id) {
  return useDataStore().getEventById(id)
}

export const useRemindersStore = defineStore('reminders', {
  state: () => ({
    items: load()
  }),
  getters: {
    count(state) {
      return Object.keys(state.items).length
    },
    /** 按提醒触发时间升序排列的提醒列表（含事件数据） */
    list(state) {
      return Object.values(state.items)
        .map((r) => ({ ...r, event: eventOf(r.eventId) }))
        .filter((r) => r.event)
        .sort((a, b) => {
          const ia = getReminderInstant(a.event, a.offset)
          const ib = getReminderInstant(b.event, b.offset)
          if (!ia || !ib) return 0
          return TemporalUntil(ia) - TemporalUntil(ib)
        })
    }
  },
  actions: {
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.items))
    },
    /** 设置 / 修改提醒（offsetId 为空则取消） */
    setReminder(eventId, offsetId) {
      const event = eventOf(eventId)
      if (!event) return
      if (!offsetId || !REMINDER_OFFSETS.some((o) => o.id === offsetId)) {
        this.cancelReminder(eventId)
        return
      }
      this.items[eventId] = {
        eventId,
        offset: offsetId,
        createdAt: Date.now(),
        channel: 'browser', // 预留：未来 Web Push 通道
        notified: false, // 预留：已触发通知标记
        subscriptionId: null // 预留：Web Push 订阅标识
      }
      this.persist()
    },
    cancelReminder(eventId) {
      if (this.items[eventId]) {
        delete this.items[eventId]
        this.persist()
      }
    },
    /** 是否已设置提醒 */
    hasReminder(eventId) {
      return Boolean(this.items[eventId])
    },
    /** 某活动的提醒配置 */
    reminderOf(eventId) {
      return this.items[eventId] || null
    },
    /** 清理已过期的提醒（活动开始时间已过的） */
    prune() {
      for (const [eventId, r] of Object.entries(this.items)) {
        const event = eventOf(eventId)
        const instant = event ? getReminderInstant(event, r.offset) : null
        if (!instant || TemporalUntil(instant) < 0) delete this.items[eventId]
      }
      this.persist()
    }
  }
})

// 简化比较：Instant 相减转毫秒
function TemporalUntil(instant) {
  return Number(instant.epochMilliseconds)
}
