import { defineStore } from 'pinia'
import { useDataStore } from './data'
import { REMINDER_OFFSETS, getReminderInstant, CUSTOM_OFFSET_MIN, CUSTOM_OFFSET_MAX } from '../utils/time'

// ============================================================
// 提醒状态管理（浏览器本地，为未来 PWA / Web Push 预留字段）
// 数据结构：
// {
//   [eventId]: {
//     eventId, offset: '1d'|'3h'|'1h'|'30m'|'start'|'custom',
//     offsetMinutes: null | number,  // offset='custom' 时为开始前分钟数
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
    items: load(),
    // Web Push 状态：'unknown' 未检测 / 'on' 已开启 / 'off' 未开启 /
    // 'unsupported' 浏览器不支持 / 'ios-guide' iOS 需先添加到主屏幕
    pushState: 'unknown',
    pushBusy: false,
    pushError: ''
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
    /** 设置 / 修改提醒（offsetId 为空或非法则取消；custom 需传 offsetMinutes） */
    setReminder(eventId, offsetId, offsetMinutes) {
      const event = eventOf(eventId)
      if (!event) return
      const valid =
        offsetId === 'custom'
          ? Number.isInteger(offsetMinutes) && offsetMinutes >= CUSTOM_OFFSET_MIN && offsetMinutes <= CUSTOM_OFFSET_MAX
          : REMINDER_OFFSETS.some((o) => o.id === offsetId)
      if (!offsetId || !valid) {
        this.cancelReminder(eventId)
        return
      }
      this.items[eventId] = {
        eventId,
        offset: offsetId,
        offsetMinutes: offsetId === 'custom' ? offsetMinutes : null,
        createdAt: Date.now(),
        channel: 'browser', // Web Push 通道（已上线）
        notified: false, // 预留：已触发通知标记
        subscriptionId: null // 预留：Web Push 订阅标识
      }
      this.persist()
      this.syncPushReminders().catch(() => {})
    },
    cancelReminder(eventId) {
      if (this.items[eventId]) {
        delete this.items[eventId]
        this.persist()
        this.syncPushReminders().catch(() => {})
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
      this.syncPushReminders().catch(() => {})
    },

    // ---- Web Push：能力检测 / 开关 / 提醒同步（utils/push 动态加载，避免环境依赖）----

    /** 检测浏览器能力与当前订阅状态（不请求权限） */
    async initPush() {
      const push = await import('../utils/push')
      if (!push.isPushSupported()) {
        this.pushState = 'unsupported'
        return
      }
      if (push.isIOS() && !push.isIOSPushReady()) {
        this.pushState = 'ios-guide'
        return
      }
      try {
        const reg = await push.registerServiceWorker().catch(() => null)
        const sub = reg ? await reg.pushManager.getSubscription() : null
        this.pushState = sub ? 'on' : 'off'
      } catch {
        this.pushState = 'off'
      }
    },

    /** 开启推送：请求授权 → 订阅 → 上报后端 → 同步提醒。用户拒绝返回 false */
    async enablePush() {
      const push = await import('../utils/push')
      this.pushBusy = true
      this.pushError = ''
      try {
        const sub = await push.subscribePush(push.getDeviceId())
        if (!sub) {
          this.pushState = 'off'
          return false
        }
        this.pushState = 'on'
        await this.syncPushReminders()
        return true
      } catch (e) {
        this.pushState = 'off'
        this.pushError = e && e.message ? e.message : String(e)
        return false
      } finally {
        this.pushBusy = false
      }
    },

    /** 关闭推送：后端删除订阅 + 浏览器侧退订 */
    async disablePush() {
      const push = await import('../utils/push')
      this.pushBusy = true
      this.pushError = ''
      try {
        await push.unsubscribePush(push.getDeviceId())
        this.pushState = 'off'
      } catch (e) {
        this.pushError = e && e.message ? e.message : String(e)
      } finally {
        this.pushBusy = false
      }
    },

    /** 将当前提醒列表全量同步到服务端（仅开启推送时生效） */
    async syncPushReminders() {
      if (this.pushState !== 'on') return
      const push = await import('../utils/push')
      await push.syncReminders(push.getDeviceId(), this.list)
    }
  }
})

// 简化比较：Instant 相减转毫秒
function TemporalUntil(instant) {
  return Number(instant.epochMilliseconds)
}
