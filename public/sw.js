/* IdolCal Service Worker：Web Push 通知 + PWA 离线缓存
 * ============================================================
 * 职责：
 *  1. Web Push 通知接收与点击跳转（原功能保留）
 *  2. 离线缓存：install 预缓存页面与构建资源；fetch 采用
 *     stale-while-revalidate 策略（网络优先，失败回退缓存，成功时后台更新缓存）
 *  3. 新版本发布后 activate 清理旧缓存
 *
 * 构建时注入：dist/sw.js 由 scripts/gen-sw-manifest.mjs 在 vite build 之后
 * 用实际 assets 清单替换下方 __PRECACHE_ASSETS__ 占位符；
 * 源码 public/sw.js 中的占位符不会被运行时读取（仅作模板）。
 */

/* 缓存版本：发布新版本（assets hash 变化）时递增，activate 会清理旧版本缓存 */
const CACHE_VERSION = 'idolcal-v1'
const APP_SHELL_CACHE = `${CACHE_VERSION}-shell`
const RUNTIME_CACHE = `${CACHE_VERSION}-runtime`

/* 基础预缓存清单（构建后脚本会合并 assets 资源） */
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/icon-192.png',
  '/calendar.ics',
  /* __PRECACHE_ASSETS__ 占位符会被构建脚本替换为实际 JS/CSS 清单 */
]

/* 只缓存同源 GET 请求（API 请求不做离线缓存，前端有本地快照回退） */
function isCacheable(req) {
  return req.method === 'GET' && new URL(req.url).origin === self.location.origin
}

/* ---------------- 生命周期 ---------------- */

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(APP_SHELL_CACHE)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key.startsWith('idolcal-') && key !== APP_SHELL_CACHE && key !== RUNTIME_CACHE)
            .map((key) => caches.delete(key))
        )
      )
      .then(() => self.clients.claim())
  )
})

/* ---------------- fetch：stale-while-revalidate（网络优先，回退缓存） ---------------- */

self.addEventListener('fetch', (event) => {
  const req = event.request
  if (!isCacheable(req)) return

  event.respondWith(
    fetch(req)
      .then((res) => {
        if (res && res.ok) {
          const copy = res.clone()
          // 缓存目标区分壳资源与运行时资源，避免版本升级后壳缓存残留
          const cacheName = req.url.includes('/assets/') || req.url === self.location.origin + '/' || req.url.endsWith('/index.html')
            ? APP_SHELL_CACHE
            : RUNTIME_CACHE
          caches.open(cacheName).then((cache) => cache.put(req, copy))
        }
        return res
      })
      .catch(() =>
        caches.match(req).then((cached) => {
          if (cached) return cached
          // 回退首页（SPA 路由刷新场景：/about 离线时给 index.html）
          if (req.mode === 'navigate') return caches.match('/index.html')
          return Response.error()
        })
      )
  )
})

/* ---------------- Web Push 通知（原功能保留） ---------------- */

// 推送事件：payload 为 JSON（后端 WebPushService 组装 title / body / icon / badge / url / eventId）
self.addEventListener('push', (event) => {
  let data = null
  try {
    data = event.data ? event.data.json() : null
  } catch {
    data = null // 非 JSON payload：仅发默认标题
  }
  const options = {
    body: data && data.body ? data.body : '',
    icon: data && data.icon ? data.icon : undefined,
    badge: data && data.badge ? data.badge : undefined,
    tag: data && data.tag ? data.tag : 'idolcal',
    renotify: true,
    data: { url: data && data.url ? data.url : '/' }
  }
  event.waitUntil(self.registration.showNotification(data && data.title ? data.title : 'IdolCal', options))
})

// 点击通知：聚焦已有窗口并跳转，否则打开新窗口
self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = (event.notification.data && event.notification.data.url) || '/'
  event.waitUntil(
    self.clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((list) => {
        for (const client of list) {
          if ('focus' in client) {
            client.focus()
            client.navigate(url).catch(() => {})
            return
          }
        }
        return self.clients.openWindow(url)
      })
  )
})
