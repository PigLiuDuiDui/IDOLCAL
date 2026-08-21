<script setup>
// ============================================================
// 管理后台布局：Sidebar（桌面可折叠 240/64 · 移动 Drawer）+ Header + 内容区
// - 「系统管理」子菜单：点击始终可展开/收起；进入子页面自动保持展开
// - 布局/主题设置全部来自 theme.js draft（实时预览）
// ============================================================
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from './components/Icon.vue'
import ToastContainer from './components/ToastContainer.vue'
import { authState, clearAuth } from './api'
import { adminGet } from './api'
import { t, adminLocale, setAdminLocale } from './i18n'
import { draft } from './theme'
import './admin.css'

const route = useRoute()
const router = useRouter()

/** 登录页：不渲染侧边栏/顶栏（登录页自带全屏布局） */
const isLogin = computed(() => !!route.meta.public)

const sidebarOpen = ref(false)
const systemOnline = ref(true)
const attentionCount = ref(0)
const overview = ref(null)

// ---- 菜单结构（文案走 i18n key） ----
const GROUPS = [
  {
    title: 'nav.overview',
    items: [
      { to: '/admin/dashboard', label: 'nav.dashboard', icon: 'dashboard' }
    ]
  },
  {
    title: 'nav.content',
    items: [
      { to: '/admin/idols', label: 'nav.idols', icon: 'star' },
      { to: '/admin/events', label: 'nav.events', icon: 'calendar' },
      { to: '/admin/meta', label: 'nav.meta', icon: 'layers' }
    ]
  },
  {
    title: 'nav.users',
    items: [
      { to: '/admin/users', label: 'nav.userList', icon: 'users' },
      { to: '/admin/subscriptions', label: 'nav.subscriptions', icon: 'link' },
      { to: '/admin/devices', label: 'nav.devices', icon: 'smartphone' }
    ]
  },
  {
    title: 'nav.push',
    items: [
      { to: '/admin/push', label: 'nav.pushCenter', icon: 'send' },
      { to: '/admin/push/deliveries', label: 'nav.pushRecords', icon: 'file-text' },
      { to: '/admin/push?status=FAILED', label: 'nav.failedDevices', icon: 'alert-triangle' }
    ]
  },
  {
    title: 'nav.system',
    items: [
      { to: '/admin/monitor', label: 'nav.monitor', icon: 'activity' },
      { to: '/admin/audit', label: 'nav.audit', icon: 'clock' }
    ]
  }
]

/** 可展开子菜单（系统管理：系统设置 / 外观与布局） */
const SUBMENUS = [
  {
    key: 'system',
    title: 'nav.systemManage',
    icon: 'settings',
    children: [
      { to: '/admin/settings/system', label: 'nav.sysSystem', icon: 'settings' },
      { to: '/admin/settings/appearance', label: 'nav.sysAppearance', icon: 'palette' }
    ]
  }
]

// 路由名 → i18n key（页面标题）
const TITLE_KEYS = {
  'admin-login': 'login.signIn',
  'admin-dashboard': 'nav.dashboard',
  'admin-idols': 'nav.idols',
  'admin-events': 'nav.events',
  'admin-meta': 'nav.meta',
  'admin-users': 'nav.userList',
  'admin-subscriptions': 'nav.subscriptions',
  'admin-devices': 'nav.devices',
  'admin-push': 'nav.pushCenter',
  'admin-push-task': 'pdetail.defaultTitle',
  'admin-push-deliveries': 'nav.pushRecords',
  'admin-monitor': 'nav.monitor',
  'admin-audit': 'nav.audit',
  'admin-settings-system': 'nav.sysSystem',
  'admin-settings-appearance': 'nav.sysAppearance'
}

const pageTitle = computed(() => t(TITLE_KEYS[route.name] || route.meta.title || 'nav.dashboard'))
const pageGroup = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/push/tasks')) return t('layout.taskDetail')
  if (path.startsWith('/admin/settings')) return t('nav.systemManage')
  const match = GROUPS.find((g) => g.items.some((i) => path.startsWith(i.to.split('?')[0])))
  return match ? t(match.title) : ''
})

const initial = computed(() => (authState.username || 'A').charAt(0).toUpperCase())

// ---- 侧边栏折叠（桌面端） ----
const COLLAPSED_KEY = 'idolcal-admin-sidebar-collapsed'
function readFlag(key, def) {
  try {
    const v = localStorage.getItem(key)
    if (v === null) return def
    return v === '1'
  } catch {
    return def
  }
}
function writeFlag(key, v) {
  try {
    localStorage.setItem(key, v ? '1' : '0')
  } catch { /* 隐私模式 */ }
}

const collapsed = ref(readFlag(COLLAPSED_KEY, draft.layout.sidebarDefault === 'collapsed'))
watch(collapsed, (v) => writeFlag(COLLAPSED_KEY, v))

/** 可折叠侧边栏开启时，默认状态遵循设置；关闭折叠功能时始终展开 */
const effectiveCollapsed = computed(() => {
  if (!draft.layout.sidebarCollapsible) return false
  return collapsed.value
})

// ---- 系统管理子菜单展开状态 ----
const SUBMENU_KEY = 'idolcal-admin-submenu-open'
const submenuOpen = ref(readFlag(SUBMENU_KEY, true))
watch(submenuOpen, (v) => writeFlag(SUBMENU_KEY, v))

const submenuActive = computed(() =>
  SUBMENUS.some((m) => m.children.some((c) => route.path.startsWith(c.to.split('?')[0])))
)

function toggleSubmenu() {
  // 点击始终切换展开/收起；进入子页面由 submenuActive 自动保持展开
  submenuOpen.value = !submenuOpen.value
}

// ---- 响应式 ----
const DESKTOP_QUERY = '(min-width: 993px)'
const isDesktop = ref(window.matchMedia(DESKTOP_QUERY).matches)

function onResize(e) {
  isDesktop.value = e.matches
  if (!e.matches) sidebarOpen.value = false
}

// ---- 系统健康轮询（Dashboard 关注的异常计数） ----
let timer = null
async function refreshHealth() {
  try {
    const data = await adminGet('/api/admin/overview')
    overview.value = data
    const byStatus = data?.push?.tasks?.byStatus || {}
    attentionCount.value = (byStatus.FAILED || 0) + (byStatus.RETRY || 0) + (byStatus.PROCESSING || 0)
  } catch {
    /* 401 已由 api 层处理跳转 */
  }
}

function logout() {
  clearAuth()
  router.push('/admin/login')
}

function goHome() {
  router.push('/')
}

function onNavClick(item) {
  if (item.to.startsWith('/admin/push?status=FAILED')) {
    router.push({ path: '/admin/push', query: { status: 'FAILED' } })
  } else {
    router.push(item.to)
  }
  sidebarOpen.value = false
}

/** 头部菜单按钮：桌面切换折叠 / 移动端打开 Drawer */
function onMenuClick() {
  if (isDesktop.value) {
    if (draft.layout.sidebarCollapsible) collapsed.value = !collapsed.value
  } else {
    sidebarOpen.value = true
  }
}

function toggleCollapsed() {
  if (draft.layout.sidebarCollapsible) collapsed.value = !collapsed.value
}

function isActive(to) {
  return route.path === to.split('?')[0]
}

onMounted(() => {
  if (!route.meta.public) {
    refreshHealth()
    timer = setInterval(refreshHealth, 30_000)
  }
  const mq = window.matchMedia(DESKTOP_QUERY)
  mq.addEventListener('change', onResize)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  window.matchMedia(DESKTOP_QUERY).removeEventListener('change', onResize)
})
</script>

<template>
  <div
    class="admin-app"
    :data-sidebar-fixed="draft.layout.sidebarFixed ? '1' : '0'"
  >
    <!-- 登录页：仅渲染内容（登录页自带全屏布局） -->
    <router-view v-if="isLogin" />
    <template v-else>
    <div class="a-layout">
      <!-- 移动端遮罩 -->
      <div v-if="sidebarOpen" class="a-sidebar-mask" @click="sidebarOpen = false" />

      <!-- Sidebar -->
      <aside class="a-sidebar" :class="{ open: sidebarOpen, collapsed: effectiveCollapsed }">
        <div class="a-sidebar-brand">
          <div class="a-sidebar-logo">IC</div>
          <div class="a-sidebar-brand-text">
            <b>IdolCal</b>
            <span>{{ t('layout.console') }}</span>
          </div>
        </div>

        <nav class="a-sidebar-nav">
          <div v-for="(g, gi) in GROUPS" :key="gi" class="a-sidebar-group">
            <div class="a-sidebar-group-title">{{ t(g.title) }}</div>
            <a
              v-for="item in g.items"
              :key="item.to"
              href="javascript:void(0)"
              class="a-sidebar-link"
              :class="{ 'router-link-active': isActive(item.to) }"
              :data-tip="t(item.label)"
              @click="onNavClick(item)"
            >
              <Icon :name="item.icon" :size="17" />
              <span class="lbl">{{ t(item.label) }}</span>
              <span
                v-if="item.to.startsWith('/admin/push?status=FAILED') && overview?.push?.tasks?.byStatus?.FAILED"
                class="a-badge a-badge-error"
                style="margin-left: auto"
              >{{ overview.push.tasks.byStatus.FAILED }}</span>
            </a>
          </div>

          <!-- 可展开子菜单（系统管理） -->
          <div v-for="sm in SUBMENUS" :key="sm.key" class="a-submenu">
            <div
              class="a-submenu-head"
              :class="{ open: submenuActive || submenuOpen }"
              :data-tip="t(sm.title)"
              role="button"
              tabindex="0"
              @click="toggleSubmenu"
              @keydown.enter="toggleSubmenu"
              @keydown.space.prevent="toggleSubmenu"
            >
              <Icon :name="sm.icon" :size="17" />
              <span class="lbl">{{ t(sm.title) }}</span>
              <Icon name="chevron-down" :size="14" class="chev" />
            </div>
            <div class="a-submenu-body" :class="{ open: submenuActive || submenuOpen }">
              <div class="a-submenu-body-inner">
                <a
                  v-for="c in sm.children"
                  :key="c.to"
                  href="javascript:void(0)"
                  class="a-sidebar-link a-submenu-link"
                  :class="{ 'router-link-active': isActive(c.to) }"
                  :data-tip="t(c.label)"
                  @click="onNavClick(c)"
                >
                  <Icon :name="c.icon" :size="16" />
                  <span class="lbl">{{ t(c.label) }}</span>
                </a>
              </div>
            </div>
          </div>
        </nav>

        <div class="a-sidebar-footer">
          <button class="a-sidebar-collapse-btn" :title="t('layout.collapse')" @click="toggleCollapsed">
            <Icon :name="effectiveCollapsed ? 'chevron-right' : 'chevron-left'" :size="15" />
            <span class="lbl">{{ t('layout.collapse') }}</span>
          </button>
          <div class="a-system-status">
            <span class="dot" :class="{ warn: attentionCount > 0 }" />
            {{ attentionCount > 0 ? t('layout.attention', { n: attentionCount }) : t('layout.systemOnline') }}
          </div>
          <div class="a-sidebar-user">
            <div class="a-avatar">{{ initial }}</div>
            <div class="a-sidebar-user-info">
              <b>{{ authState.username || 'admin' }}</b>
              <span>{{ authState.role || 'ADMIN' }}</span>
            </div>
            <button class="a-icon-btn" :title="t('layout.signOut')" @click="logout">
              <Icon name="logout" :size="16" />
            </button>
          </div>
        </div>
      </aside>

      <!-- 主区域 -->
      <div class="a-main">
        <header v-if="draft.header.show" class="a-header" :class="{ static: !draft.header.fixed }">
          <button class="a-icon-btn a-header-menu-btn" :title="t('layout.menu')" @click="onMenuClick">
            <Icon name="menu" :size="18" />
          </button>

          <div class="a-header-title" v-if="draft.header.pageTitle || draft.header.breadcrumb">
            <h1 v-if="draft.header.pageTitle">{{ pageTitle }}</h1>
            <div class="breadcrumb" v-if="draft.header.breadcrumb">
              <a href="javascript:void(0)" @click="goHome">IdolCal</a>
              <span v-if="pageGroup"> · {{ pageGroup }}</span>
              <span> · {{ pageTitle }}</span>
            </div>
          </div>

          <div class="a-header-actions">
            <div v-if="attentionCount > 0" class="a-header-alert show">
              <Icon name="alert-triangle" :size="14" />
              {{ t('layout.attentionShort', { n: attentionCount }) }}
            </div>
            <!-- 语言切换：中文 / EN -->
            <div class="a-locale-switch">
              <button :class="{ active: adminLocale === 'zh-CN' }" @click="setAdminLocale('zh-CN')">中文</button>
              <button :class="{ active: adminLocale === 'en' }" @click="setAdminLocale('en')">EN</button>
            </div>
            <button class="a-icon-btn" :title="t('layout.help')" @click="goHome">
              <Icon name="help-circle" :size="18" />
            </button>
            <button class="a-icon-btn a-badge-dot" :title="t('layout.notifications')" @click="router.push('/admin/push')">
              <Icon name="bell" :size="18" />
            </button>
            <div class="a-avatar" style="width: 30px; height: 30px; font-size: 12px; margin-left: 4px">{{ initial }}</div>
          </div>
        </header>

        <main class="a-content">
          <router-view />
        </main>
      </div>
    </div>
    </template>
    <ToastContainer />
  </div>
</template>

<style scoped>
.a-header .a-header-menu-btn {
  display: none;
}
@media (max-width: 992px) {
  .a-header .a-header-menu-btn {
    display: inline-flex;
  }
}

/* 语言切换器 */
.a-locale-switch {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  margin-left: 4px;
  border-radius: 8px;
  background: var(--a-primary-soft);
}
.a-locale-switch button {
  border: 0;
  background: transparent;
  color: var(--a-text-3);
  font-size: 12px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.a-locale-switch button:hover {
  color: var(--a-text-2);
}
.a-locale-switch button.active {
  background: var(--a-primary);
  color: #fff;
}
</style>
