import { createRouter, createWebHashHistory } from 'vue-router'
// 首页视图同步加载（首屏必需），其余视图懒加载，避免首包包含全部页面代码
import ScheduleView from '../views/ScheduleView.vue'

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
    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue'), meta: { title: 'About' } }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · EVAN OFFICIAL SCHEDULE` : 'EVAN OFFICIAL SCHEDULE'
})

export default router
