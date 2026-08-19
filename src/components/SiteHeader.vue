<template>
  <header class="site-header">
    <div class="container header-inner">
      <RouterLink to="/" class="brand">
        <span class="brand-name">EVAN</span>
        <span class="brand-sub">OFFICIAL SCHEDULE</span>
      </RouterLink>

      <nav class="desktop-nav" :aria-label="t('nav.label')">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          active-class="active"
        >
          <span class="nav-index">{{ item.index }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="header-tools">
        <TimezoneSwitcher />
        <LocaleSwitcher />
      </div>
    </div>
  </header>
</template>

<script setup>
// 顶部导航：仅桌面端显示，移动端使用底部导航
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitcher from './LocaleSwitcher.vue'
import TimezoneSwitcher from './TimezoneSwitcher.vue'

const { t } = useI18n()

const NAV_ITEMS = [
  { to: '/', key: 'schedule', index: '01' },
  { to: '/comeback', key: 'comeback', index: '02' },
  { to: '/reminders', key: 'reminders', index: '03' },
  { to: '/timeline', key: 'timeline', index: '04' },
  { to: '/archive', key: 'archive', index: '05' },
  { to: '/about', key: 'about', index: '06' }
]

const navItems = computed(() => NAV_ITEMS.map((item) => ({ ...item, label: t(`nav.${item.key}`) })))
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(250, 249, 247, 0.86);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 12px;
  text-decoration: none;
  color: var(--ink);
}

.brand-name {
  font-family: var(--serif);
  font-size: 20px;
  letter-spacing: 0.18em;
  font-weight: 600;
}

.brand-sub {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--ink-faint);
  text-transform: uppercase;
}

.desktop-nav {
  display: flex;
  gap: 8px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 16px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--ink-soft);
  text-decoration: none;
  border-radius: var(--radius-sm);
  transition: color var(--dur) var(--ease), background var(--dur) var(--ease);
}

.nav-link:hover {
  color: var(--ink);
  background: var(--surface-alt);
}

.nav-link.active {
  color: var(--accent);
}

.nav-index {
  font-size: 8px;
  color: var(--ink-faint);
  letter-spacing: 0.1em;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 移动端：隐藏桌面导航，仅保留品牌 + 语言切换 */
@media (max-width: 900px) {
  .desktop-nav {
    display: none;
  }

  .header-inner {
    height: 56px;
    justify-content: space-between;
  }

  .header-tools {
    gap: 6px;
  }
}
</style>
