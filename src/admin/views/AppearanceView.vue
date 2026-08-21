<script setup>
// ============================================================
// 外观与布局：合并原 Theme / Style / Layout / Sidebar / Header 五个页面
// - 左侧分组导航（外观：主题/样式 · 布局：侧边栏/顶部栏），本地切换
// - 所有修改写入 theme.js draft → 全后台即时预览
// - 底部提供「后台缩略预览」，而非组件展示台
// ============================================================
import { ref, computed } from 'vue'
import Icon from '../components/Icon.vue'
import { t } from '../i18n'
import { draft, PRIMARY_PRESETS } from '../theme'

// ---- 左侧分组导航 ----
const SECTIONS = [
  {
    group: 'sys.app.groups.appearance',
    items: [
      { id: 'theme', labelKey: 'sys.app.theme', icon: 'palette' },
      { id: 'style', labelKey: 'sys.app.style', icon: 'pen-tool' }
    ]
  },
  {
    group: 'sys.app.groups.layout',
    items: [
      { id: 'sidebar', labelKey: 'sys.app.sidebar', icon: 'sidebar' },
      { id: 'header', labelKey: 'sys.app.header', icon: 'header' }
    ]
  }
]
const active = ref('theme')

const MODES = [
  { id: 'light', labelKey: 'sys.theme.light', icon: 'sun' },
  { id: 'dark', labelKey: 'sys.theme.dark', icon: 'moon' },
  { id: 'system', labelKey: 'sys.theme.system', icon: 'monitor' }
]

const RADIUS_OPTS = [
  { id: 'none', labelKey: 'sys.style.radiusNone' },
  { id: 'small', labelKey: 'sys.style.radiusSmall' },
  { id: 'standard', labelKey: 'sys.style.radiusStandard' },
  { id: 'large', labelKey: 'sys.style.radiusLarge' }
]

const CARD_OPTS = [
  { id: 'default', labelKey: 'sys.style.cardDefault' },
  { id: 'shadow-light', labelKey: 'sys.style.cardShadowLight' },
  { id: 'shadow-strong', labelKey: 'sys.style.cardShadowStrong' },
  { id: 'bordered', labelKey: 'sys.style.cardBordered' },
  { id: 'borderless', labelKey: 'sys.style.cardBorderless' }
]

const SHADOW_OPTS = [
  { id: 'none', labelKey: 'sys.style.shadowNone' },
  { id: 'light', labelKey: 'sys.style.shadowLight' },
  { id: 'medium', labelKey: 'sys.style.shadowMedium' },
  { id: 'strong', labelKey: 'sys.style.shadowStrong' }
]

const HEADER_TOGGLES = [
  { key: 'show', labelKey: 'sys.header.show', descKey: 'sys.header.showDesc' },
  { key: 'fixed', labelKey: 'sys.header.fixed', descKey: 'sys.header.fixedDesc' },
  { key: 'breadcrumb', labelKey: 'sys.header.breadcrumb', descKey: 'sys.header.breadcrumbDesc' },
  { key: 'pageTitle', labelKey: 'sys.header.pageTitle', descKey: 'sys.header.pageTitleDesc' }
]

const isCustom = computed(() => !PRIMARY_PRESETS.some((p) => p.color === draft.primaryColor))

function pickPreset(color) {
  draft.primaryColor = color
}

/** slider 填充比例 */
function sliderFill(val, min, max) {
  return { '--fill': `${Math.min(100, Math.max(0, ((val - min) / (max - min)) * 100))}%` }
}

/** 缩略预览：侧边栏宽度跟随设置（按比例缩小） */
const previewSidebarW = computed(() => {
  const collapsed = draft.layout.sidebarCollapsible && draft.layout.sidebarDefault === 'collapsed'
  const base = collapsed ? draft.layout.sidebarCollapsedWidth : draft.layout.sidebarWidth
  return `${Math.max(26, Math.round(base * 0.3))}px`
})

const chartBars = [38, 52, 44, 70, 62, 84, 58]
</script>

<template>
  <div class="a-appearance">
    <!-- 左侧：分组导航 -->
    <nav class="a-appearance-nav" aria-label="appearance sections">
      <div v-for="(g, gi) in SECTIONS" :key="gi" class="a-appearance-group">
        <div class="a-appearance-group-title">{{ t(g.group) }}</div>
        <button
          v-for="item in g.items"
          :key="item.id"
          class="a-appearance-nav-item"
          :class="{ active: active === item.id }"
          @click="active = item.id"
        >
          <Icon :name="item.icon" :size="15" />
          {{ t(item.labelKey) }}
        </button>
      </div>
    </nav>

    <!-- 右侧：当前分组设置 + 实时预览 -->
    <div class="a-appearance-main">
      <!-- 主题 -->
      <div v-if="active === 'theme'" class="a-settings-sections">
        <div class="a-settings-section-head">
          <h3>{{ t('sys.app.theme') }}</h3>
          <div class="desc">{{ t('sys.app.themeDesc') }}</div>
        </div>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.theme.mode') }}</h3>
              <div class="desc">{{ t('sys.theme.modeDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div class="a-seg">
              <button
                v-for="m in MODES"
                :key="m.id"
                :class="{ active: draft.themeMode === m.id }"
                @click="draft.themeMode = m.id"
              >
                <Icon :name="m.icon" :size="14" /> {{ t(m.labelKey) }}
              </button>
            </div>
          </div>
        </section>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.theme.primary') }}</h3>
              <div class="desc">{{ t('sys.theme.primaryDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div class="a-opt-grid">
              <button
                v-for="p in PRIMARY_PRESETS"
                :key="p.id"
                class="a-color-chip"
                :class="{ active: draft.primaryColor === p.color }"
                :style="{ '--chip': p.color }"
                @click="pickPreset(p.color)"
              >
                <span class="swatch">
                  <Icon v-if="draft.primaryColor === p.color" name="check" :size="14" />
                </span>
                <span class="name">{{ t(p.labelKey) }}</span>
              </button>

              <label class="a-color-chip custom" :class="{ active: isCustom }" :style="{ '--chip': draft.primaryColor }">
                <span class="swatch">
                  <input v-model="draft.primaryColor" type="color" :aria-label="t('sys.theme.custom')" />
                  <Icon v-if="isCustom" name="check" :size="14" />
                </span>
                <span class="name">{{ t('sys.theme.custom') }}</span>
              </label>
            </div>
            <div v-if="isCustom" class="a-settings-row" style="margin-top: 6px">
              <div class="info">
                <div class="name">{{ t('sys.theme.customColor') }}</div>
              </div>
              <div class="ctrl">
                <label class="a-color">
                  <input v-model="draft.primaryColor" type="color" />
                  <input v-model="draft.primaryColor" class="hex" spellcheck="false" />
                </label>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 样式 -->
      <div v-else-if="active === 'style'" class="a-settings-sections">
        <div class="a-settings-section-head">
          <h3>{{ t('sys.app.style') }}</h3>
          <div class="desc">{{ t('sys.app.styleDesc') }}</div>
        </div>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.style.radius') }}</h3>
              <div class="desc">{{ t('sys.style.radiusHint') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div class="a-settings-row">
              <div class="info"><div class="name">{{ t('sys.style.radius') }}</div></div>
              <div class="ctrl">
                <div class="a-seg">
                  <button
                    v-for="o in RADIUS_OPTS"
                    :key="o.id"
                    :class="{ active: draft.radius === o.id }"
                    @click="draft.radius = o.id"
                  >{{ t(o.labelKey) }}</button>
                </div>
              </div>
            </div>
            <div class="a-radius-preview" style="margin-top: 14px">
              <div class="box">Card</div>
              <div class="box" style="border-radius: var(--a-r-btn)">Btn</div>
              <div class="box" style="border-radius: var(--a-r-input)">Input</div>
              <div class="box" style="border-radius: var(--a-r-badge)">Badge</div>
            </div>
          </div>
        </section>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.style.card') }}</h3>
              <div class="desc">{{ t('sys.style.cardDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div class="a-opt-grid">
              <button
                v-for="o in CARD_OPTS"
                :key="o.id"
                class="a-opt-card"
                :class="{ active: draft.cardStyle === o.id }"
                :data-card="o.id"
                @click="draft.cardStyle = o.id"
              >
                <span class="mini-card"><span class="bar" /><span class="bar main" /><span class="bar" /></span>
                <span class="name">{{ t(o.labelKey) }}</span>
              </button>
            </div>
          </div>
        </section>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.style.shadow') }}</h3>
              <div class="desc">{{ t('sys.style.shadowDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div class="a-settings-row">
              <div class="info"><div class="name">{{ t('sys.style.shadow') }}</div></div>
              <div class="ctrl">
                <div class="a-seg">
                  <button
                    v-for="o in SHADOW_OPTS"
                    :key="o.id"
                    :class="{ active: draft.shadow === o.id }"
                    @click="draft.shadow = o.id"
                  >{{ t(o.labelKey) }}</button>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 侧边栏 -->
      <div v-else-if="active === 'sidebar'" class="a-settings-sections">
        <div class="a-settings-section-head">
          <h3>{{ t('sys.app.sidebar') }}</h3>
          <div class="desc">{{ t('sys.app.sidebarDesc') }}</div>
        </div>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.layout.sidebar') }}</h3>
              <div class="desc">{{ t('sys.layout.sidebarDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div class="a-settings-row">
              <div class="info">
                <div class="name">{{ t('sys.layout.sidebarFixed') }}</div>
                <div class="desc">{{ t('sys.layout.sidebarFixedDesc') }}</div>
              </div>
              <div class="ctrl">
                <label class="a-switch">
                  <input v-model="draft.layout.sidebarFixed" type="checkbox" />
                  <span class="track" /><span class="thumb" />
                </label>
              </div>
            </div>

            <div class="a-settings-row">
              <div class="info">
                <div class="name">{{ t('sys.layout.sidebarCollapsible') }}</div>
                <div class="desc">{{ t('sys.layout.sidebarCollapsibleDesc') }}</div>
              </div>
              <div class="ctrl">
                <label class="a-switch">
                  <input v-model="draft.layout.sidebarCollapsible" type="checkbox" />
                  <span class="track" /><span class="thumb" />
                </label>
              </div>
            </div>

            <div class="a-settings-row">
              <div class="info">
                <div class="name">{{ t('sys.layout.sidebarDefault') }}</div>
                <div class="desc">{{ t('sys.layout.sidebarDefaultDesc') }}</div>
              </div>
              <div class="ctrl">
                <div class="a-seg">
                  <button
                    :class="{ active: draft.layout.sidebarDefault === 'expanded' }"
                    :disabled="!draft.layout.sidebarCollapsible"
                    @click="draft.layout.sidebarDefault = 'expanded'"
                  >{{ t('sys.layout.expanded') }}</button>
                  <button
                    :class="{ active: draft.layout.sidebarDefault === 'collapsed' }"
                    :disabled="!draft.layout.sidebarCollapsible"
                    @click="draft.layout.sidebarDefault = 'collapsed'"
                  >{{ t('sys.layout.collapsed') }}</button>
                </div>
              </div>
            </div>

            <div class="a-settings-row">
              <div class="info">
                <div class="name">{{ t('sys.layout.width') }}</div>
                <div class="desc">{{ t('sys.layout.widthDesc', { n: draft.layout.sidebarWidth }) }}</div>
              </div>
              <div class="ctrl">
                <input
                  v-model.number="draft.layout.sidebarWidth"
                  class="a-range"
                  type="range"
                  min="160"
                  max="360"
                  step="4"
                  :style="sliderFill(draft.layout.sidebarWidth, 160, 360)"
                />
                <span class="a-range-val">{{ draft.layout.sidebarWidth }}px</span>
              </div>
            </div>

            <div class="a-settings-row">
              <div class="info">
                <div class="name">{{ t('sys.layout.collapsedWidth') }}</div>
                <div class="desc">{{ t('sys.layout.collapsedWidthDesc', { n: draft.layout.sidebarCollapsedWidth }) }}</div>
              </div>
              <div class="ctrl">
                <input
                  v-model.number="draft.layout.sidebarCollapsedWidth"
                  class="a-range"
                  type="range"
                  min="48"
                  max="96"
                  step="2"
                  :disabled="!draft.layout.sidebarCollapsible"
                  :style="sliderFill(draft.layout.sidebarCollapsedWidth, 48, 96)"
                />
                <span class="a-range-val">{{ draft.layout.sidebarCollapsedWidth }}px</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 顶部栏 -->
      <div v-else class="a-settings-sections">
        <div class="a-settings-section-head">
          <h3>{{ t('sys.app.header') }}</h3>
          <div class="desc">{{ t('sys.app.headerDesc') }}</div>
        </div>

        <section class="a-card a-settings-card">
          <div class="a-card-head">
            <div>
              <h3>{{ t('sys.header.nav') }}</h3>
              <div class="desc">{{ t('sys.header.navDesc') }}</div>
            </div>
          </div>
          <div class="a-card-body">
            <div
              v-for="o in HEADER_TOGGLES"
              :key="o.key"
              class="a-settings-row"
              :class="{ muted: o.key !== 'show' && !draft.header.show }"
            >
              <div class="info">
                <div class="name">{{ t(o.labelKey) }}</div>
                <div class="desc">{{ t(o.descKey) }}</div>
              </div>
              <div class="ctrl">
                <label class="a-switch">
                  <input v-model="draft.header[o.key]" type="checkbox" />
                  <span class="track" /><span class="thumb" />
                </label>
              </div>
            </div>

            <div class="a-settings-row">
              <div class="info">
                <div class="name">{{ t('sys.header.height') }}</div>
                <div class="desc">{{ t('sys.header.heightDesc', { n: draft.header.height }) }}</div>
              </div>
              <div class="ctrl">
                <input
                  v-model.number="draft.header.height"
                  class="a-range"
                  type="range"
                  min="48"
                  max="80"
                  step="2"
                  :disabled="!draft.header.show"
                  :style="sliderFill(draft.header.height, 48, 80)"
                />
                <span class="a-range-val">{{ draft.header.height }}px</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 实时预览：后台缩略图（联动所有设置） -->
      <div class="a-settings-section-head" style="margin-top: 24px">
        <h3>{{ t('sys.app.preview') }}</h3>
        <div class="desc">{{ t('sys.app.previewDesc') }}</div>
      </div>
      <div class="a-card a-settings-card">
        <div class="a-card-body" style="padding: 16px">
          <div class="a-theme-preview">
            <!-- 缩略顶栏 -->
            <div class="tp-header">
              <div class="tp-h-left">
                <span class="tp-logo">IC</span>
                <span class="tp-h-title">IdolCal Admin</span>
              </div>
              <div class="tp-h-actions"><i /><i /><i /></div>
            </div>
            <div class="tp-body">
              <!-- 缩略侧边栏（宽度跟随设置） -->
              <div class="tp-sidebar" :style="{ width: previewSidebarW }">
                <span class="tp-menu-item active">{{ t('nav.dashboard') }}</span>
                <span class="tp-menu-item">{{ t('nav.idols') }}</span>
                <span class="tp-menu-item">{{ t('nav.events') }}</span>
                <span class="tp-menu-item">{{ t('nav.push') }}</span>
                <span class="tp-menu-item">{{ t('nav.systemManage') }}</span>
              </div>
              <!-- 缩略内容：KPI + 图表 + 表格 -->
              <div class="tp-main">
                <div class="tp-kpis">
                  <div v-for="n in 3" :key="n" class="tp-kpi">
                    <b>1,2{{ n }}8</b>
                    <i>KPI</i>
                  </div>
                </div>
                <div class="tp-chart">
                  <i v-for="(h, i) in chartBars" :key="i" :style="{ height: h + '%' }" />
                </div>
                <div class="tp-table">
                  <div v-for="n in 2" :key="n" class="tp-row"><span /><span /><span /></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 页面骨架：左导航 + 右内容 */
.a-appearance {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 20px;
  align-items: start;
}
.a-appearance-nav {
  position: sticky;
  top: calc(var(--a-header-height) + 20px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px 10px;
  background: var(--a-card);
  border: var(--a-card-border);
  border-radius: var(--a-r-card);
  box-shadow: var(--a-card-shadow);
}
.a-appearance-group-title {
  font-size: 11px;
  font-weight: 650;
  color: var(--a-text-3);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding: 0 10px;
  margin-bottom: 2px;
}
.a-appearance-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--a-text-2);
  font-size: 13px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}
.a-appearance-nav-item:hover {
  background: var(--a-card-alt);
  color: var(--a-text);
}
.a-appearance-nav-item.active {
  background: var(--a-primary-soft);
  color: var(--a-primary-ink);
  font-weight: 600;
}
.a-appearance-main {
  min-width: 0;
}

/* ---------- 后台缩略预览 ---------- */
.a-theme-preview {
  border: 1px solid var(--a-border);
  border-radius: calc(var(--a-r-card) + 4px);
  overflow: hidden;
  background: var(--a-bg);
  box-shadow: inset 0 0 0 1px rgba(127, 127, 127, 0.04);
}
.tp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 34px;
  padding: 0 12px;
  background: var(--a-header-bg);
  border-bottom: 1px solid var(--a-border);
}
.tp-h-left {
  display: flex;
  align-items: center;
  gap: 7px;
}
.tp-logo {
  width: 17px;
  height: 17px;
  border-radius: 5px;
  background: var(--a-primary);
  color: #fff;
  font-size: 8px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tp-h-title {
  font-size: 10.5px;
  font-weight: 650;
  color: var(--a-text);
}
.tp-h-actions {
  display: flex;
  gap: 5px;
}
.tp-h-actions i {
  width: 16px;
  height: 5px;
  border-radius: 999px;
  background: var(--a-border-strong);
}
.tp-body {
  display: flex;
  min-height: 210px;
}
.tp-sidebar {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 10px 8px;
  background: var(--a-sidebar-bg);
  border-right: 1px solid var(--a-sidebar-border);
  transition: width 0.25s var(--a-ease);
  overflow: hidden;
}
.tp-menu-item {
  height: 16px;
  padding: 0 7px;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 550;
  color: var(--a-sidebar-text2);
  display: flex;
  align-items: center;
  white-space: nowrap;
}
.tp-menu-item.active {
  background: var(--a-sidebar-active-bg);
  color: var(--a-sidebar-active-text);
  font-weight: 650;
}
.tp-main {
  flex: 1;
  min-width: 0;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tp-kpis {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
}
.tp-kpi {
  background: var(--a-card);
  border: var(--a-card-border);
  border-radius: var(--a-r-card);
  box-shadow: var(--a-card-shadow);
  padding: 7px 9px;
}
.tp-kpi b {
  display: block;
  font-size: 12px;
  font-weight: 750;
  color: var(--a-text);
  font-variant-numeric: tabular-nums;
}
.tp-kpi i {
  font-style: normal;
  font-size: 8px;
  color: var(--a-text-3);
}
.tp-chart {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 5px;
  padding: 10px;
  background: var(--a-card);
  border: var(--a-card-border);
  border-radius: var(--a-r-card);
  box-shadow: var(--a-card-shadow);
}
.tp-chart i {
  flex: 1;
  max-width: 26px;
  border-radius: 3px;
  background: var(--a-primary);
  opacity: 0.82;
  transition: height 0.25s var(--a-ease);
}
.tp-table {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.tp-row {
  display: flex;
  gap: 6px;
}
.tp-row span {
  flex: 1;
  height: 6px;
  border-radius: 2px;
  background: var(--a-border);
}
.tp-row span:first-child {
  background: var(--a-primary-soft);
}

/* ---------- 响应式 ---------- */
@media (max-width: 992px) {
  .a-appearance {
    grid-template-columns: 1fr;
  }
  .a-appearance-nav {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px 18px;
    padding: 10px 12px;
  }
  .a-appearance-group {
    display: contents;
  }
  .a-appearance-group-title {
    width: 100%;
    padding-top: 2px;
  }
}
</style>
