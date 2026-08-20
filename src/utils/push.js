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
// 设备所有权凭证（subscribe 时由后端签发 HMAC 签名，写请求携带防越权）
const CREDENTIAL_KEY = 'idolcal-device-credential'
const CREDENTIAL_HEADER = 'X-Device-Token'

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

// ---- 设备标识与所有权凭证（无登录体系，匿名 UUID 作设备锚点）----

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

/** 设备凭证（subscribe 响应签发）；未开启过推送或凭证已失效时为 null */
export function getDeviceCredential() {
  return localStorage.getItem(CREDENTIAL_KEY)
}

function saveDeviceCredential(credential) {
  if (credential) localStorage.setItem(CREDENTIAL_KEY, credential)
}

/** 凭证失效（后端 401）时清除，用户重新开启推送即可换发 */
export function clearDeviceCredential() {
  localStorage.removeItem(CREDENTIAL_KEY)
}

/** 写请求凭证头：X-Device-Token: <deviceId>.<signature>；无凭证返回 null */
function credentialHeader(deviceId) {
  const credential = getDeviceCredential()
  return credential ? `${deviceId}.${credential}` : null
}

// ---- 内部请求封装 ----

/** 结构化推送 API 错误（含 HTTP 状态 / 可重试标记，便于展示与自动重试判断） */
export class PushApiError extends Error {
  constructor(message, { path, status, retryable = false } = {}) {
    super(message)
    this.name = 'PushApiError'
    this.path = path
    this.status = status
    this.retryable = retryable
  }
}

const REQUEST_TIMEOUT_MS = 10_000
const MAX_RETRIES = 1 // 网络抖动 / 超时 / 5xx / 429 重试 1 次；4xx（除 429）直接失败

async function api(path, options = {}) {
  let lastError
  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), REQUEST_TIMEOUT_MS)
    try {
      const res = await fetch(`${API_BASE}${path}`, { ...options, signal: ctrl.signal })
      if (res.ok) return res.json()
      let message = `HTTP ${res.status}`
      try {
        const body = await res.json()
        if (body && body.message) message = body.message
      } catch {
        /* 非 JSON 错误体：保留状态码描述 */
      }
      throw new PushApiError(message, {
        path,
        status: res.status,
        retryable: res.status >= 500 || res.status === 429
      })
    } catch (e) {
      if (e instanceof PushApiError) {
        if (!e.retryable || attempt === MAX_RETRIES) throw e
        lastError = e
      } else if (attempt === MAX_RETRIES) {
        // 网络错误 / 超时（AbortError）：重试耗尽后抛结构化错误
        const timedOut = e && e.name === 'AbortError'
        throw new PushApiError(timedOut ? `请求超时（${REQUEST_TIMEOUT_MS / 1000}s）` : '网络请求失败', {
          path,
          retryable: true
        })
      } else {
        lastError = e
      }
    } finally {
      clearTimeout(timer)
    }
  }
  throw lastError
}

/** 附带设备凭证头的写请求（凭证缺失时直接失败，由调用方提示重新开启推送） */
async function authedApi(deviceId, path, options = {}) {
  const header = credentialHeader(deviceId)
  if (!header) {
    throw new PushApiError('设备凭证缺失，请重新开启推送', { path, retryable: false })
  }
  try {
    return await api(path, {
      ...options,
      headers: { ...(options.headers || {}), [CREDENTIAL_HEADER]: header }
    })
  } catch (e) {
    // 凭证已失效（密钥轮换 / 服务重启）：清除，用户重新开启推送即可换发
    if (e instanceof PushApiError && e.status === 401) clearDeviceCredential()
    throw e
  }
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

/** CryptoKey 二进制 → base64 字符串（后端存 p256dh / auth）；分块转换避免大数组展开栈溢出 */
function keyToBase64(key) {
  const bytes = new Uint8Array(key)
  let bin = ''
  const CHUNK = 0x8000 // String.fromCharCode.apply 安全块大小（32K）
  for (let i = 0; i < bytes.length; i += CHUNK) {
    bin += String.fromCharCode.apply(null, bytes.subarray(i, i + CHUNK))
  }
  return btoa(bin)
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
  const { credential } = await api('/api/push/subscribe', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deviceId,
      endpoint: sub.endpoint,
      p256dh: keyToBase64(sub.getKey('p256dh')),
      auth: keyToBase64(sub.getKey('auth'))
    })
  })
  // 保存后端签发的设备凭证（重新订阅 / 凭证换发时更新）
  saveDeviceCredential(credential)
}

/** 关闭推送：先通知后端删除订阅（需设备凭证），再浏览器侧退订 */
export async function unsubscribePush(deviceId) {
  const reg = swRegPromise ? await swRegPromise.catch(() => null) : null
  const sub = reg ? await reg.pushManager.getSubscription() : null
  if (sub) {
    try {
      await authedApi(deviceId, '/api/push/subscribe', {
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
  await authedApi(deviceId, '/api/push/reminders', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId, reminders: items })
  })
}

/** 发送测试通知（需已开启推送） */
export async function sendTestPush(deviceId) {
  await authedApi(deviceId, '/api/push/send-test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId })
  })
}
