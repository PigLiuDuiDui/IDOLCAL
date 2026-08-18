<template>
  <nav class="bottom-nav" :aria-label="t('bottomNav.label')">
    <RouterLink
      v-for="item in navItems"
      :key="item.to"
      :to="item.to"
      class="bn-link"
      active-class="active"
    >
      <span class="bn-icon">{{ item.icon }}</span>
      <span class="bn-label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup>
// 移动端底部轻量导航：Schedule / Timeline / Archive / About
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const NAV_ITEMS = [
  { to: '/', key: 'schedule', icon: '▦' },
  { to: '/timeline', key: 'timeline', icon: '│' },
  { to: '/archive', key: 'archive', icon: '◫' },
  { to: '/about', key: 'about', icon: 'i' }
]

const navItems = computed(() => NAV_ITEMS.map((item) => ({ ...item, label: t(`nav.${item.key}`) })))
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  background: rgba(250, 249, 247, 0.94);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid var(--line);
  padding-bottom: env(safe-area-inset-bottom);
}

.bn-link {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-height: 56px; /* 点击区域 ≥ 44px */
  text-decoration: none;
  color: var(--ink-faint);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  transition: color var(--dur) var(--ease);
}

.bn-icon {
  font-size: 13px;
  line-height: 1;
}

.bn-link.active {
  color: var(--accent);
}

.bn-link.active .bn-icon {
  transform: translateY(-1px);
}

/* 桌面端隐藏 */
@media (min-width: 901px) {
  .bottom-nav {
    display: none;
  }
}
</style>
