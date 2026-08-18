// i18n 实例：语言检测 / 持久化 / html 文档同步 / 多语言数据字段取值
import { createI18n, useI18n } from 'vue-i18n'
import en from './locales/en'
import zhCN from './locales/zh-CN'
import ko from './locales/ko'

export const LOCALES = [
  { code: 'en', short: 'EN', label: 'English' },
  { code: 'zh-CN', short: '中文', label: '简体中文' },
  { code: 'ko', short: '한국어', label: '한국어' }
]

export const STORAGE_KEY = 'idolcal-locale'

// FullCalendar 的 locale code 与 i18n locale 的映射
export const FC_LOCALES = {
  en: 'en',
  'zh-CN': 'zh-cn',
  ko: 'ko'
}

/** 语言检测：优先 localStorage，其次浏览器语言，默认英文 */
function detectLocale() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved && LOCALES.some((l) => l.code === saved)) return saved
  const nav = (navigator.language || '').toLowerCase()
  if (nav.startsWith('zh')) return 'zh-CN'
  if (nav.startsWith('ko')) return 'ko'
  return 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: detectLocale(),
  fallbackLocale: 'en',
  messages: { en, 'zh-CN': zhCN, ko }
})

const HTML_LANG = { en: 'en', 'zh-CN': 'zh-CN', ko: 'ko' }

/** 同步 <html lang> 与文档标题 / 描述 */
export function applyDocumentLocale() {
  const locale = i18n.global.locale.value
  document.documentElement.lang = HTML_LANG[locale] || 'en'
  document.title = i18n.global.t('meta.title')
  const desc = document.querySelector('meta[name="description"]')
  if (desc) desc.setAttribute('content', i18n.global.t('meta.description'))
}

/** 切换语言并持久化 */
export function setLocale(code) {
  i18n.global.locale.value = code
  localStorage.setItem(STORAGE_KEY, code)
  applyDocumentLocale()
}

/**
 * 多语言数据字段取值：{ en: '...', 'zh-CN': '...', ko: '...' } -> 当前语言文本。
 * 纯字符串字段原样返回，兼容未翻译的数据。
 */
export function useText() {
  const { locale } = useI18n()
  return (value) => {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return value[locale.value] ?? value.en ?? value['zh-CN'] ?? Object.values(value)[0] ?? ''
    }
    return value ?? ''
  }
}

export default i18n
