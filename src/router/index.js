import { createRouter, createWebHashHistory } from 'vue-router'
import ScheduleView from '../views/ScheduleView.vue'
import TimelineView from '../views/TimelineView.vue'
import ArchiveView from '../views/ArchiveView.vue'
import AboutView from '../views/AboutView.vue'

const router = createRouter({
  // 使用 hash 模式，方便本地直接预览运行
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'schedule', component: ScheduleView, meta: { title: 'Schedule' } },
    { path: '/timeline', name: 'timeline', component: TimelineView, meta: { title: 'Timeline' } },
    { path: '/archive', name: 'archive', component: ArchiveView, meta: { title: 'Archive' } },
    { path: '/about', name: 'about', component: AboutView, meta: { title: 'About' } }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} · EVAN OFFICIAL SCHEDULE` : 'EVAN OFFICIAL SCHEDULE'
})

export default router
