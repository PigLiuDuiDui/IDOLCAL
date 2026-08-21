<script setup>
// ============================================================
// 系统管理外壳：左侧分类导航（系统设置 / 外观与布局）+ 右侧内容
// - 仅「外观与布局」提供底部操作栏（系统设置为只读运行配置）
// - 实时预览（theme.js draft）→ 点击「保存设置」统一提交
// ============================================================
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import Icon from './Icon.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import { t } from '../i18n'
import { toast } from '../toast'
import { isDirty, saveSettings, restoreSaved, resetToDefault } from '../theme'
import '../settings.css'
const route = useRoute()

const NAV = [
  { to: '/admin/settings/system', labelKey: 'nav.sysSystem', icon: 'settings' },
  { to: '/admin/settings/appearance', labelKey: 'nav.sysAppearance', icon: 'palette' }
]

/** 仅外观与布局页支持编辑（系统设置为只读，隐藏操作栏） */
const editable = computed(() => route.path === '/admin/settings/appearance')

const showReset = ref(false)
const saving = ref(false)

const canReset = computed(() => isDirty.value)

function onSave() {
  if (!isDirty.value || saving.value) return
  saving.value = true
  // 本地持久化（无后端请求），稍作延迟模拟提交反馈
  setTimeout(() => {
    saveSettings()
    saving.value = false
    toast.success(t('sys.saved'))
  }, 260)
}

function onCancel() {
  if (!isDirty.value) return
  restoreSaved()
  toast.info(t('sys.reverted'))
}

function onResetConfirm() {
  showReset.value = false
  resetToDefault()
  toast.info(t('sys.restored'))
}
</script>

<template>
  <div class="a-settings-shell">
    <div class="a-settings-head">
      <h2>{{ t('sys.title') }}</h2>
      <div class="sub">{{ t('sys.sub') }}</div>
    </div>

    <div class="a-settings-body">
      <!-- 左侧：设置分类导航 -->
      <nav class="a-settings-nav" aria-label="settings">
        <router-link
          v-for="item in NAV"
          :key="item.to"
          :to="item.to"
          class="a-settings-nav-item"
          :class="{ active: route.path === item.to }"
        >
          <Icon :name="item.icon" :size="15" />
          {{ t(item.labelKey) }}
        </router-link>
      </nav>

      <!-- 右侧：对应设置内容 -->
      <div class="a-settings-content">
        <router-view />

      <!-- 底部操作栏（仅外观与布局） -->
        <div v-if="editable" class="a-settings-bar">
          <span v-if="isDirty" class="dirty-hint">
            <span class="dot" /> {{ t('sys.unsaved') }}
          </span>
          <button class="a-btn" :class="{ 'a-btn-danger': canReset }" :disabled="!canReset" @click="showReset = true">
            <Icon name="refresh" :size="14" /> {{ t('sys.reset') }}
          </button>
          <button class="a-btn" :disabled="!isDirty" @click="onCancel">{{ t('sys.cancel') }}</button>
          <button class="a-btn a-btn-primary" :disabled="!isDirty || saving" @click="onSave">
            <Icon v-if="saving" name="loader" :size="14" class="spin" />
            <Icon v-else name="check" :size="14" />
            {{ t('sys.save') }}
          </button>
        </div>
      </div>
    </div>

    <ConfirmDialog
      :open="showReset"
      :title="t('sys.resetTitle')"
      :message="t('sys.resetMsg')"
      :danger="false"
      :confirm-text="t('sys.reset')"
      @confirm="onResetConfirm"
      @cancel="showReset = false"
    />
  </div>
</template>

<style scoped>
.spin {
  animation: aSpin 0.9s linear infinite;
}
@keyframes aSpin {
  to { transform: rotate(360deg); }
}
</style>
