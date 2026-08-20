// ============================================================
// Web Push 前端工具：订阅 / 退订 / 设备标识 / 提醒同步
// - Android：Chrome / Edge / Firefox 原生支持，直接可用
// - iOS：Safari 16.4+，必须先「添加到主屏幕」（standalone）才能请求权限
// - 接口前缀与 data store 一致（VITE_API_BASE），dev 走 Vite proxy
// ============================================================
import { reminderOffsetMinutes } from './time'

const API_BASE = import.meta.env.VITE_API_BASE || ''
const SW_PATH = '/sw.js'
const DEVICE_KEY = 'idolcal-device-id'

// ---- 能力检测（Android / iOS 分流依据）----

/** 浏览器是否具备 Web Push 全套能力（SW + PushManager + Notification） */
export function isPushSupported() {
  return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

/** 是否 iOS 设备（含 iPadOS 13+ 桌面 UA 伪装） */
export function isIOS() {
  return (
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  )
}

/** 是否以独立窗口运行（iOS「添加到主屏幕」后为 true） */
export function isStandalone() {
  return window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true
}

/** iOS 上是否已满足 Web Push 前提（已添加到主屏幕） */
export function isIOSPushReady() {
  return isIOS() && isStandalone()
}

// ---- 设备标识（无登录体系，匿名 UUID 作设备锚点）----

export function getDeviceId() {
  let id = localStorage.getItem(DEVICE_KEY)
  if (!id) {
    id = crypto.randomUUID
      ? crypto.randomUUID()
      : `d-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem(DEVICE_KEY, id)
  }
  return id
}

// ---- 内部请求封装 ----

async function api(path, options) {
  const res = await fetch(`${API_BASE}${path}`, options)
  if (!res.ok) throw new Error(`Push API ${path} → ${res.status}`)
  return res.json()
}

// ---- 订阅 / 退订 ----

let swRegPromise = null

/** 注册 Service Worker（幂等：重复调用复用同一 Promise，失败后可重试） */
export function registerServiceWorker() {
  if (!swRegPromise) {
    swRegPromise = navigator.serviceWorker.register(SW_PATH).catch((err) => {
      swRegPromise = null
      throw err
    })
  }
  return swRegPromise
}

/** base64url → Uint8Array（applicationServerKey 需要） */
function urlBase64ToUint8Array(base64url) {
  const bin = atob(base64url.replace(/-/g, '+').replace(/_/g, '/'))
  return Uint8Array.from(bin, (c) => c.charCodeAt(0))
}

/** CryptoKey 二进制 → base64 字符串（后端存 p256dh / auth） */
function keyToBase64(key) {
  return btoa(String.fromCharCode(...new Uint8Array(key)))
}

/**
 * 开启推送：请求权限 → 注册 SW → 订阅 → 上报后端。
 * @returns {Promise<PushSubscription|null>} 成功返回订阅；用户拒绝授权返回 null
 */
export async function subscribePush(deviceId) {
  const permission = await Notification.requestPermission()
  if (permission !== 'granted') return null
  const reg = await registerServiceWorker()
  const existing = await reg.pushManager.getSubscription()
  if (existing) {
    await reportSubscription(deviceId, existing)
    return existing
  }
  const { key } = await api('/api/push/vapid-public-key')
  const sub = await reg.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(key)
  })
  await reportSubscription(deviceId, sub)
  return sub
}

async function reportSubscription(deviceId, sub) {
  await api('/api/push/subscribe', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deviceId,
      endpoint: sub.endpoint,
      p256dh: keyToBase64(sub.getKey('p256dh')),
      auth: keyToBase64(sub.getKey('auth'))
    })
  })
}

/** 关闭推送：先通知后端删除订阅，再浏览器侧退订 */
export async function unsubscribePush(deviceId) {
  const reg = swRegPromise ? await swRegPromise.catch(() => null) : null
  const sub = reg ? await reg.pushManager.getSubscription() : null
  if (sub) {
    try {
      await api('/api/push/subscribe', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId, endpoint: sub.endpoint })
      })
    } finally {
      await sub.unsubscribe().catch(() => {})
    }
  }
}

// ---- 提醒同步（全量覆盖该设备的服务端提醒任务）----

/**
 * 同步提醒列表到后端：每个条目只传 eventId + offsetMinutes。
 * 触发时刻由后端计算：event.start_at_utc - offsetMinutes（官方时区在后端统一换算，
 * 避免前端各自计算导致时区不一致）。
 * @param {Array<{eventId: string, offset: string, offsetMinutes: number|null, event: object}>} reminders
 */
export async function syncReminders(deviceId, reminders) {
  const items = reminders
    .map((r) => {
      const offsetMinutes = reminderOffsetMinutes(r)
      if (offsetMinutes === null) return null
      return { eventId: r.eventId, offsetMinutes }
    })
    .filter(Boolean)
  await api('/api/push/reminders', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId, reminders: items })
  })
}

/** 发送测试通知（需已开启推送） */
export async function sendTestPush(deviceId) {
  await api('/api/push/send-test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId })
  })
}
