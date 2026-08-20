/* IdolCal Service Worker：Web Push 通知接收与点击跳转 */
// 生命周期：立即接管，避免旧 SW 阻塞新版本
self.addEventListener('install', () => self.skipWaiting())
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()))

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
