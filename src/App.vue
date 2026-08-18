<template>
  <div class="app-shell">
    <SiteHeader />

    <main class="app-main">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <BottomNav />
    <EventDetailDrawer />
  </div>
</template>

<script setup>
// 根组件：应用骨架 + 顶部导航 + 移动底部导航 + 全局活动详情面板
import SiteHeader from './components/SiteHeader.vue'
import BottomNav from './components/BottomNav.vue'
import EventDetailDrawer from './components/EventDetailDrawer.vue'
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
}

/* 移动端底部导航占位，避免内容被遮挡 */
@media (max-width: 900px) {
  .app-main {
    padding-bottom: 64px;
  }
}
</style>
