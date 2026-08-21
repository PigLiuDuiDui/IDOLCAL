import { createRouter, createWebHashHistory } from 'vue-router'
// 首页视图同步加载（首屏必需），其余视图懒加载，避免首包包含全部页面代码
import ScheduleView from '../views/ScheduleView.vue'
import { isAuthed } from '../admin/api'

const router = createRouter({
  // 使用 hash 模式，方便本地直接预览运行
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'schedule', component: ScheduleView, meta: { title: 'Schedule' } },
    // 通知点击直达：/#/event/{id} → 打开活动详情抽屉（ScheduleView 挂载时读取并打开）
    { path: '/event/:id', name: 'event', component: ScheduleView, meta: { title: 'Event' } },
    { path: '/comeback', name: 'comeback', component: () => import('../views/ComebackView.vue'), meta: { title: 'Comeback' } },
    { path: '/reminders', name: 'reminders', component: () => import('../views/RemindersView.vue'), meta: { title: 'Reminders' } },
    { path: '/timeline', name: 'timeline', component: () => import('../views/TimelineView.vue'), meta: { title: 'Timeline' } },
    { path: '/archive', name: 'archive', component: () => import('../views/ArchiveView.vue'), meta: { title: 'Archive' } },
    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue'), meta: { title: 'About' } },

    // ---- 管理后台（/admin/**，独立布局与样式） ----
    {
      path: '/admin',
      component: () => import('../admin/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: 'login', name: 'admin-login', component: () => import('../admin/views/AdminLogin.vue'), meta: { title: 'Sign in', public: true } },
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', name: 'admin-dashboard', component: () => import('../admin/views/DashboardView.vue'), meta: { title: 'Dashboard' } },
        { path: 'idols', name: 'admin-idols', component: () => import('../admin/views/IdolsView.vue'), meta: { title: 'Idols' } },
        { path: 'events', name: 'admin-events', component: () => import('../admin/views/EventsView.vue'), meta: { title: 'Calendar Events' } },
        { path: 'meta', name: 'admin-meta', component: () => import('../admin/views/MetaView.vue'), meta: { title: 'Meta' } },
        { path: 'users', name: 'admin-users', component: () => import('../admin/views/UsersView.vue'), meta: { title: 'Users' } },
        { path: 'subscriptions', name: 'admin-subscriptions', component: () => import('../admin/views/SubscriptionsView.vue'), meta: { title: 'Subscriptions' } },
        { path: 'devices', name: 'admin-devices', component: () => import('../admin/views/DevicesView.vue'), meta: { title: 'Push Devices' } },
        { path: 'push', name: 'admin-push', component: () => import('../admin/views/PushCenterView.vue'), meta: { title: 'Push Center' } },
        { path: 'push/tasks/:id', name: 'admin-push-task', component: () => import('../admin/views/PushTaskDetailView.vue'), meta: { title: 'Push Task' } },
        { path: 'push/deliveries', name: 'admin-push-deliveries', component: () => import('../admin/views/DeliveriesView.vue'), meta: { title: 'Push Records' } },
        { path: 'monitor', name: 'admin-monitor', component: () => import('../admin/views/SystemMonitorView.vue'), meta: { title: 'System Monitor' } },
        { path: 'audit', name: 'admin-audit', component: () => import('../admin/views/AuditLogsView.vue'), meta: { title: 'Audit Logs' } },
        // 系统管理：系统设置（只读运行时配置）/ 外观与布局（可编辑）
        {
          path: 'settings',
          name: 'admin-settings',
          component: () => import('../admin/components/SettingsShell.vue'),
          redirect: '/admin/settings/system',
          meta: { title: 'System Management' },
          children: [
            { path: 'system', name: 'admin-settings-system', component: () => import('../admin/views/SettingsView.vue'), meta: { title: 'System Settings' } },
            { path: 'appearance', name: 'admin-settings-appearance', component: () => import('../admin/views/AppearanceView.vue'), meta: { title: 'Appearance & Layout' } }
          ]
        }
      ]
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

// 管理后台守卫：/admin/** 未登录一律跳转登录页（login 除外）
router.beforeEach((to) => {
  if (to.path.startsWith('/admin')) {
    if (to.meta.public) {
      // 已登录访问登录页 → 直接进后台
      if (isAuthed()) return '/admin/dashboard'
    } else if (!isAuthed()) {
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
  }
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · EVAN OFFICIAL SCHEDULE` : 'EVAN OFFICIAL SCHEDULE'
})

export default router
