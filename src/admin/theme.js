// ============================================================
// 管理后台主题系统 —— Design Tokens 的唯一事实来源
// - draft 草稿：任何修改实时预览（watch 深度监听 → 写 CSS 变量）
// - 点击「保存设置」统一持久化到 localStorage
// - tokens：响应式色板，供图表等组件联动（主题变化即时更新）
// - 所有设置最终映射为 --a-* CSS 变量，写入 <html>，全后台生效
// ============================================================
import { reactive, computed, watch } from 'vue'

export const SETTINGS_KEY = 'idolcal-admin-settings'

// ---------- 主色预设 ----------
export const PRIMARY_PRESETS = [
  { id: 'brand', labelKey: 'sys.theme.primaryBrand', color: '#a62f2f' },
  { id: 'tech', labelKey: 'sys.theme.primaryTech', color: '#2563eb' },
  { id: 'deep', labelKey: 'sys.theme.primaryDeep', color: '#1e3a8a' },
  { id: 'teal', labelKey: 'sys.theme.primaryTeal', color: '#0d9488' },
  { id: 'purple', labelKey: 'sys.theme.primaryPurple', color: '#7c3aed' },
  { id: 'orange', labelKey: 'sys.theme.primaryOrange', color: '#ea580c' }
]

// ---------- 默认设置（刻意保持精简：这是产品后台，不是 UI Builder） ----------
export const DEFAULT_SETTINGS = {
  themeMode: 'light',           // light | dark | system
  primaryColor: '#a62f2f',      // 默认保持现有品牌色
  radius: 'standard',           // none | small | standard | large（标准 = 8px 基准）
  cardStyle: 'default',         // default | shadow-light | shadow-strong | bordered | borderless
  shadow: 'medium',             // none | light | medium | strong
  layout: {
    sidebarFixed: true,
    sidebarCollapsible: true,
    sidebarDefault: 'expanded', // expanded | collapsed
    sidebarWidth: 240,
    sidebarCollapsedWidth: 64
  },
  header: {
    show: true,
    fixed: true,
    breadcrumb: true,
    pageTitle: true,
    height: 64
  }
}

// ---------- 工具 ----------
const clone = (o) => JSON.parse(JSON.stringify(o))

function hexToRgb(hex) {
  const h = String(hex || '').replace('#', '')
  if (h.length === 3) return h.split('').map((c) => parseInt(c + c, 16))
  const n = parseInt(h, 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

/** 混合颜色：ratio 为 other 的占比（0-1） */
function mix(hex, other, ratio) {
  const a = hexToRgb(hex)
  const b = hexToRgb(other)
  const c = a.map((v, i) => Math.round(v + (b[i] - v) * ratio))
  return '#' + c.map((v) => v.toString(16).padStart(2, '0')).join('')
}

/** 主色半透明（暗色模式下的柔和背景 / 焦点环） */
function rgba(hex, alpha) {
  const [r, g, b] = hexToRgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function mergeDeep(base, over) {
  const out = { ...base }
  for (const k of Object.keys(over || {})) {
    if (over[k] && typeof over[k] === 'object' && !Array.isArray(over[k]) && base[k] && typeof base[k] === 'object') {
      out[k] = mergeDeep(base[k], over[k])
    } else if (over[k] !== undefined) {
      out[k] = over[k]
    }
  }
  return out
}

function loadSaved() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    if (!raw) return null
    // 只保留当前配置结构内的 key（清洗旧版本残留的废弃配置）
    return sanitize(DEFAULT_SETTINGS, JSON.parse(raw))
  } catch {
    return null
  }
}

/** 按默认结构白名单清洗：丢弃已删除的旧配置项 */
function sanitize(base, over) {
  const out = {}
  for (const k of Object.keys(base)) {
    const b = base[k]
    const o = over ? over[k] : undefined
    if (b && typeof b === 'object' && !Array.isArray(b) && o && typeof o === 'object' && !Array.isArray(o)) {
      out[k] = sanitize(b, o)
    } else if (o !== undefined) {
      out[k] = o
    } else {
      out[k] = clone(b)
    }
  }
  return out
}

// ---------- 状态 ----------
export const saved = reactive(loadSaved() || clone(DEFAULT_SETTINGS))
/** 草稿：设置页修改的实时预览对象，保存后才写入 saved */
export const draft = reactive(clone(saved))

export const isDirty = computed(() => JSON.stringify(draft) !== JSON.stringify(saved))

// ---------- 系统主题（跟随系统模式） ----------
const mq = typeof window !== 'undefined' && window.matchMedia
  ? window.matchMedia('(prefers-color-scheme: dark)')
  : null

function systemDark() {
  return mq ? mq.matches : false
}

/** 解析当前实际模式：system → 跟随操作系统 */
export function resolveMode() {
  const m = draft.themeMode
  if (m === 'system') return systemDark() ? 'dark' : 'light'
  return m === 'dark' ? 'dark' : 'light'
}

if (mq) {
  mq.addEventListener('change', () => {
    if (draft.themeMode === 'system') applySettings()
  })
}

// ---------- 响应式色板（图表等组件联动） ----------
export const tokens = reactive({
  mode: 'light',
  primary: '#a62f2f',
  primaryStrong: '#8f2828',
  primarySoft: '#f7efee',
  primaryInk: '#7c2222',
  success: '#16a34a',
  warning: '#d97706',
  error: '#dc2626',
  info: '#2563eb',
  text: '#1f2937',
  text2: '#6b7280',
  text3: '#9ca3af',
  border: '#e5e7eb',
  card: '#ffffff',
  bg: '#f8f9fb'
})

// ---------- 渲染参数表 ----------
const RADII = {
  none: [0, 0, 0, 0],
  small: [6, 4, 4, 3],
  standard: [12, 8, 8, 6],
  large: [16, 12, 12, 8]
}

const SHADOWS = {
  light: {
    none: ['none', 'none'],
    light: ['0 1px 2px rgba(16,24,40,0.04), 0 1px 3px rgba(16,24,40,0.06)', '0 4px 16px rgba(16,24,40,0.08), 0 2px 6px rgba(16,24,40,0.05)'],
    medium: ['0 2px 6px rgba(16,24,40,0.06), 0 4px 14px rgba(16,24,40,0.08)', '0 8px 28px rgba(16,24,40,0.10), 0 3px 10px rgba(16,24,40,0.07)'],
    strong: ['0 4px 14px rgba(16,24,40,0.10), 0 10px 32px rgba(16,24,40,0.12)', '0 14px 48px rgba(16,24,40,0.16), 0 4px 18px rgba(16,24,40,0.10)']
  },
  dark: {
    none: ['none', 'none'],
    light: ['0 1px 2px rgba(0,0,0,0.35)', '0 4px 16px rgba(0,0,0,0.42)'],
    medium: ['0 2px 6px rgba(0,0,0,0.38), 0 4px 14px rgba(0,0,0,0.30)', '0 8px 28px rgba(0,0,0,0.46)'],
    strong: ['0 4px 12px rgba(0,0,0,0.42), 0 10px 32px rgba(0,0,0,0.34)', '0 14px 48px rgba(0,0,0,0.52)']
  }
}

const CARD_STYLES = {
  default: 'border',          // 边框 + 默认阴影
  'shadow-light': 'shadowLight',
  'shadow-strong': 'shadowStrong',
  bordered: 'border',
  borderless: 'none'
}

// ---------- 构建完整变量集 ----------
function buildTokens(s, mode) {
  const dark = mode === 'dark'
  const p = s.primaryColor || '#a62f2f'

  const primary = p
  const primaryStrong = mix(p, '#000000', dark ? 0.18 : 0.12)
  const primarySoft = dark ? rgba(p, 0.16) : mix(p, '#ffffff', 0.93)
  const primaryInk = dark ? mix(p, '#ffffff', 0.34) : mix(p, '#000000', 0.62)
  const focusRing = rgba(p, dark ? 0.30 : 0.12)

  const neut = dark
    ? { bg: '#0e1320', card: '#171d2b', cardAlt: '#1d2434', text: '#e6eaf2', text2: '#a3adc0', text3: '#68738a', border: '#232c40', borderStrong: '#35405c' }
    : { bg: '#f8f9fb', card: '#ffffff', cardAlt: '#fafbfc', text: '#1f2937', text2: '#6b7280', text3: '#9ca3af', border: '#e5e7eb', borderStrong: '#d1d5db' }

  const states = dark
    ? {
        success: '#34d399', successBg: 'rgba(52,211,153,0.13)', successStrong: '#6ee7b7', warning: '#fbbf24', warningBg: 'rgba(251,191,36,0.13)', warningStrong: '#fcd34d',
        error: '#f87171', errorBg: 'rgba(248,113,113,0.13)', errorStrong: '#fca5a5', info: '#60a5fa', infoBg: 'rgba(96,165,250,0.13)', infoStrong: '#93c5fd',
        purple: '#a78bfa', purpleBg: 'rgba(167,139,250,0.13)', slate: '#94a3b8', slateBg: 'rgba(148,163,184,0.13)'
      }
    : {
        success: '#16a34a', successBg: '#f0fdf4', successStrong: '#166534', warning: '#d97706', warningBg: '#fffbeb', warningStrong: '#92400e',
        error: '#dc2626', errorBg: '#fef2f2', errorStrong: '#991b1b', info: '#2563eb', infoBg: '#eff6ff', infoStrong: '#1e40af',
        purple: '#7c3aed', purpleBg: '#f5f3ff', slate: '#64748b', slateBg: '#f1f5f9'
      }

  // 侧边栏（跟随主题模式：暗色主题用深色板，浅色主题用浅色板）
  const sidebarDark = dark
  const sidebar = sidebarDark
    ? { bg: '#111726', text: '#e6eaf2', text2: '#8b95ab', border: 'rgba(255,255,255,0.06)', hoverBg: 'rgba(255,255,255,0.05)', activeBg: rgba(p, 0.20), activeText: '#ffffff' }
    : { bg: '#ffffff', text: '#1f2937', text2: '#6b7280', border: '#e5e7eb', hoverBg: '#fafbfc', activeBg: primarySoft, activeText: primaryInk }

  // 顶部栏（跟随主题模式）
  const headerBg = dark ? 'rgba(17,23,38,0.86)' : 'rgba(255,255,255,0.88)'

  const [rc, rb, ri, rbd] = RADII[s.radius] || RADII.standard
  const [sh, shLg] = (SHADOWS[dark ? 'dark' : 'light'][s.shadow]) || SHADOWS.light.medium

  const cardBorder = CARD_STYLES[s.cardStyle] === 'none' ? 'none' : `1px solid ${neut.border}`
  let cardShadow = sh
  if (s.cardStyle === 'shadow-light') cardShadow = SHADOWS[dark ? 'dark' : 'light'].light[0]
  else if (s.cardStyle === 'shadow-strong') cardShadow = SHADOWS[dark ? 'dark' : 'light'].strong[0]
  else if (s.cardStyle === 'bordered' || s.cardStyle === 'borderless') cardShadow = 'none'

  const sw = Math.max(160, Math.min(360, Number(s.layout.sidebarWidth) || 240))
  const scw = Math.max(48, Math.min(96, Number(s.layout.sidebarCollapsedWidth) || 64))

  return {
    '--a-primary': primary,
    '--a-primary-strong': primaryStrong,
    '--a-primary-soft': primarySoft,
    '--a-primary-ink': primaryInk,
    '--a-focus-ring': focusRing,
    '--a-bg': neut.bg,
    '--a-card': neut.card,
    '--a-card-alt': neut.cardAlt,
    '--a-text': neut.text,
    '--a-text-2': neut.text2,
    '--a-text-3': neut.text3,
    '--a-border': neut.border,
    '--a-border-strong': neut.borderStrong,
    '--a-success': states.success,
    '--a-success-bg': states.successBg,
    '--a-success-strong': states.successStrong,
    '--a-warning': states.warning,
    '--a-warning-bg': states.warningBg,
    '--a-warning-strong': states.warningStrong,
    '--a-error': states.error,
    '--a-error-bg': states.errorBg,
    '--a-error-strong': states.errorStrong,
    '--a-info': states.info,
    '--a-info-bg': states.infoBg,
    '--a-info-strong': states.infoStrong,
    '--a-purple': states.purple,
    '--a-purple-bg': states.purpleBg,
    '--a-slate': states.slate,
    '--a-slate-bg': states.slateBg,
    '--a-r-card': `${rc}px`,
    '--a-r-btn': `${rb}px`,
    '--a-r-input': `${ri}px`,
    '--a-r-badge': `${rbd}px`,
    '--a-shadow': sh,
    '--a-shadow-lg': shLg,
    '--a-card-border': cardBorder,
    '--a-card-shadow': cardShadow,
    '--a-sidebar-width': `${sw}px`,
    '--a-sidebar-collapsed-width': `${scw}px`,
    '--a-sidebar-bg': sidebar.bg,
    '--a-sidebar-text': sidebar.text,
    '--a-sidebar-text-2': sidebar.text2,
    '--a-sidebar-border': sidebar.border,
    '--a-sidebar-hover-bg': sidebar.hoverBg,
    '--a-sidebar-active-bg': sidebar.activeBg,
    '--a-sidebar-active-text': sidebar.activeText,
    '--a-header-bg': headerBg,
    '--a-header-height': `${Math.max(48, Math.min(80, Number(s.header.height) || 64))}px`
  }
}

// ---------- 应用 / 保存 ----------
export function applySettings() {
  const mode = resolveMode()
  const vars = buildTokens(draft, mode)
  const root = document.documentElement
  for (const [k, v] of Object.entries(vars)) root.style.setProperty(k, v)
  root.setAttribute('data-a-theme', mode)

  Object.assign(tokens, {
    mode,
    primary: vars['--a-primary'],
    primaryStrong: vars['--a-primary-strong'],
    primarySoft: vars['--a-primary-soft'],
    primaryInk: vars['--a-primary-ink'],
    success: vars['--a-success'],
    warning: vars['--a-warning'],
    error: vars['--a-error'],
    info: vars['--a-info'],
    text: vars['--a-text'],
    text2: vars['--a-text-2'],
    text3: vars['--a-text-3'],
    border: vars['--a-border'],
    card: vars['--a-card'],
    bg: vars['--a-bg']
  })
}

/** 保存草稿（统一提交） */
export function saveSettings() {
  Object.assign(saved, clone(draft))
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(saved))
  } catch { /* 隐私模式 */ }
}

/** 取消：恢复为已保存值（保持实时预览体验） */
export function restoreSaved() {
  Object.assign(draft, clone(saved))
}

/** 恢复默认：草稿重置为默认并实时预览，保存后才持久化 */
export function resetToDefault() {
  Object.assign(draft, clone(DEFAULT_SETTINGS))
}

// 草稿变更 → 立即应用（实时预览）；模块加载即应用一次
watch(draft, applySettings, { deep: true })
applySettings()
