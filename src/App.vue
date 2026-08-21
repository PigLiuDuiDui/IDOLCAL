<template>
  <!-- 管理后台（/admin/**）使用独立布局，隐藏主站顶部/底部导航与全局抽屉 -->
  <div class="app-shell" :class="{ 'is-admin': isAdmin }">
    <SiteHeader v-if="!isAdmin" />

    <main class="app-main">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <BottomNav v-if="!isAdmin" />
    <EventDetailDrawer v-if="!isAdmin" />
  </div>
</template>

<script setup>
// 根组件：应用骨架 + 顶部导航 + 移动底部导航 + 全局活动详情面板
// admin 路由下全部隐藏（AdminLayout 提供独立全屏壳）
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import BottomNav from './components/BottomNav.vue'
import EventDetailDrawer from './components/EventDetailDrawer.vue'
import { useDataStore } from './stores/data'

const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))

const data = useDataStore()

// 启动时从后端拉取全量数据（失败自动回退本地快照，页面始终可用）
onMounted(() => data.loadAll())
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
  /* admin 无底部导航，去掉占位 */
  .app-shell.is-admin .app-main {
    padding-bottom: 0;
  }
}
</style>
