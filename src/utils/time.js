// ============================================================
// 时区转换核心
// 数据层永远保存官方原始时区（events.js 的 timezone 字段），
// 前端基于 Temporal 在用户时区 / 手动选择时区之间转换，
// 转换结果绝不写回数据。
// ============================================================
import { Temporal } from 'temporal-polyfill'

// ---- 官方时区缩写 → IANA 时区 ----
export const TZ_ALIASES = {
  KST: 'Asia/Seoul',
  JST: 'Asia/Tokyo',
  CST: 'Asia/Shanghai',
  ICT: 'Asia/Bangkok',
  PHT: 'Asia/Manila',
  SGT: 'Asia/Singapore',
  WIB: 'Asia/Jakarta',
  HKT: 'Asia/Hong_Kong',
  IST: 'Asia/Kolkata'
}

/** 事件 timezone 字段（缩写）→ IANA 时区 */
export function ianaZone(tz) {
  return TZ_ALIASES[tz] || tz || 'Asia/Seoul'
}

// ---- 常用粉丝时区（时区选择器用）----
export const TIMEZONE_OPTIONS = [
  'Asia/Seoul',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Asia/Taipei',
  'Asia/Hong_Kong',
  'Asia/Singapore',
  'Asia/Bangkok',
  'Asia/Manila',
  'Asia/Jakarta',
  'Asia/Kolkata',
  'Australia/Sydney',
  'Europe/London',
  'Europe/Paris',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'UTC'
]

/** 浏览器默认时区（IANA，自动检测） */
export function browserZone() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  } catch {
    return 'UTC'
  }
}

/** 固定缩写映射（粉丝熟悉的 KST / JST / CST 等） */
const FIXED_ABBR = {
  'Asia/Seoul': 'KST',
  'Asia/Tokyo': 'JST',
  'Asia/Shanghai': 'CST',
  'Asia/Taipei': 'CST',
  'Asia/Hong_Kong': 'HKT',
  'Asia/Singapore': 'SGT',
  'Asia/Bangkok': 'ICT',
  'Asia/Manila': 'PHT',
  'Asia/Jakarta': 'WIB',
  'Asia/Kolkata': 'IST'
}

/**
 * 时区显示缩写：优先固定映射（KST/JST…），
 * 其余用 Intl 短名（EDT/BST/CEST），再兜底为 UTC±H。
 * @param {string} zone IANA 时区
 * @param {string} [dateKey] 计算当天的偏移（夏令时正确）
 */
export function tzAbbr(zone, dateKey) {
  if (FIXED_ABBR[zone]) return FIXED_ABBR[zone]
  try {
    const d = dateKey ? new Date(`${dateKey}T12:00:00`) : new Date()
    const parts = new Intl.DateTimeFormat('en-US', { timeZone: zone, timeZoneName: 'short' }).formatToParts(d)
    const name = parts.find((p) => p.type === 'timeZoneName')?.value || ''
    if (/^[A-Z]{3,4}$/.test(name)) return name // EDT / BST / CEST
    const m = name.match(/GMT([+-])(\d{1,2})(?::(\d{2}))?/)
    if (m) return `UTC${m[1]}${Number(m[2])}`
  } catch {
    /* 忽略：兜底 */
  }
  return zone.split('/').pop().replace(/_/g, ' ')
}

/**
 * 事件 → 完整时间数据契约（README 数据层要求）
 * startDateTime / endDateTime / timezone / originalTimezone
 * timezone 字段即官方原始时区，历史数据不重复保存转换结果。
 */
export function getEventDateTime(event) {
  const start = `${event.date} ${event.time || '00:00'}`
  const end = event.endDate ? `${event.endDate} ${event.time || '00:00'}` : start
  return {
    startDateTime: start,
    endDateTime: end,
    timezone: event.timezone || 'KST',
    originalTimezone: event.originalTimezone || event.timezone || 'KST'
  }
}

/** 事件开始时刻（官方时区 ZonedDateTime）；无 time 的全天事件返回 null */
export function getEventStart(event) {
  if (!event.time) return null
  const [y, m, d] = event.date.split('-').map(Number)
  const [h, min] = event.time.split(':').map(Number)
  try {
    return Temporal.ZonedDateTime.from({
      timeZone: ianaZone(event.timezone),
      year: y,
      month: m,
      day: d,
      hour: h,
      minute: min
    })
  } catch {
    return null
  }
}

/** 事件结束时刻（官方时区）；无明确结束时间的全天事件返回 null */
export function getEventEnd(event) {
  if (!event.time || !event.endDate) return null
  const [y, m, d] = event.endDate.split('-').map(Number)
  const [h, min] = event.time.split(':').map(Number)
  try {
    return Temporal.ZonedDateTime.from({
      timeZone: ianaZone(event.timezone),
      year: y,
      month: m,
      day: d,
      hour: h,
      minute: min
    })
  } catch {
    return null
  }
}

/** YYYY-MM-DD -> 'HH:MM'（本地时区显示用，忽略无效） */
function zdtToParts(zdt) {
  return {
    date: `${zdt.year}-${String(zdt.month).padStart(2, '0')}-${String(zdt.day).padStart(2, '0')}`,
    time: `${String(zdt.hour).padStart(2, '0')}:${String(zdt.minute).padStart(2, '0')}`
  }
}

/**
 * 事件 → 官方时间 + 用户时区时间（统一转换入口，所有页面共用）
 * @param {object} event 事件数据
 * @param {string} displayZone IANA 时区（用户显示时区）
 * @returns {{ official: {date,time,tz}|null, local: {date,time,tz}|null, dayShift: number, startInstant: Temporal.Instant|null, isAllDay: boolean }}
 */
export function localizeEvent(event, displayZone) {
  const start = getEventStart(event)
  if (!start) {
    // 全天事件：只保留日期，不转换
    return {
      official: null,
      local: null,
      dayShift: 0,
      startInstant: null,
      isAllDay: true
    }
  }
  const official = { ...zdtToParts(start), tz: tzAbbr(ianaZone(event.timezone), event.date) }
  const localZdt = start.withTimeZone(displayZone)
  const localParts = zdtToParts(localZdt)
  const local = { ...localParts, tz: tzAbbr(displayZone, localParts.date) }
  const dayShift = diffDaysKey(official.date, local.date)
  return {
    official,
    local,
    dayShift,
    startInstant: start.toInstant(),
    isAllDay: false
  }
}

/** 两 YYYY-MM-DD 相差天数（local - official，跨天方向） */
export function diffDaysKey(dateKey1, dateKey2) {
  const MS_DAY = 24 * 60 * 60 * 1000
  return Math.round(
    (Date.UTC(...dateKey2.split('-').map(Number)) - Date.UTC(...dateKey1.split('-').map(Number))) / MS_DAY
  )
}

// ---- 提醒偏移选项（1天前 / 3小时前 / 1小时前 / 30分钟前 / 开始时 / 自定义）----
// 注意：Instant.subtract 不支持日历单位（days），1 天用 24 小时表示（绝对时刻，无 DST 歧义）
// custom 为动态偏移：分钟数由用户输入（见 CUSTOM_OFFSET_MIN / MAX）
export const REMINDER_OFFSETS = [
  { id: '1d', delta: { hours: 24 } },
  { id: '3h', delta: { hours: 3 } },
  { id: '1h', delta: { hours: 1 } },
  { id: '30m', delta: { minutes: 30 } },
  { id: 'start', delta: null },
  { id: 'custom', delta: null, custom: true }
]

/** 自定义提醒偏移范围（分钟）：5 分钟 ~ 30 天 */
export const CUSTOM_OFFSET_MIN = 5
export const CUSTOM_OFFSET_MAX = 30 * 24 * 60

/**
 * 提醒触发时刻（Instant，基于官方时区计算后减去偏移）
 * @param {string} [offsetMinutes] custom 偏移的分钟数（必填）
 */
export function getReminderInstant(event, offsetId, offsetMinutes) {
  const start = getEventStart(event)
  if (!start) return null
  if (offsetId === 'custom') {
    const m = Number(offsetMinutes)
    if (!Number.isFinite(m) || m <= 0) return null
    return start.toInstant().subtract({ minutes: m })
  }
  const offset = REMINDER_OFFSETS.find((o) => o.id === offsetId)
  if (!offset || !offset.delta) return start.toInstant()
  return start.toInstant().subtract(offset.delta)
}

/**
 * 提醒配置（store 条目）→ 活动开始前分钟数；
 * 'start' → 0；无效配置 → null。订阅日历 VALARM 与批量显示共用。
 */
export function reminderOffsetMinutes(reminder) {
  if (!reminder) return null
  if (reminder.offset === 'custom') {
    const m = Number(reminder.offsetMinutes)
    return Number.isFinite(m) && m > 0 ? m : null
  }
  if (reminder.offset === 'start') return 0
  const offset = REMINDER_OFFSETS.find((o) => o.id === reminder.offset)
  if (!offset?.delta) return null
  const { hours = 0, minutes = 0 } = offset.delta
  return hours * 60 + minutes
}

/** 提前分钟数 → 显示单位（整日/整小时优先，否则分钟）；无效返回 null */
export function minutesToParts(minutes) {
  const m = Number(minutes)
  if (!Number.isFinite(m) || m <= 0) return null
  if (m % 1440 === 0) return { value: m / 1440, unit: 'day' }
  if (m % 60 === 0) return { value: m / 60, unit: 'hour' }
  return { value: m, unit: 'minute' }
}

/** 提醒触发时刻 → 显示时区格式化（'09.07 · 18:00 JST'）；custom 偏移需传 offsetMinutes */
export function formatReminderAt(event, offsetId, displayZone, offsetMinutes) {
  const instant = getReminderInstant(event, offsetId, offsetMinutes)
  if (!instant) return null
  const zdt = instant.toZonedDateTimeISO(displayZone)
  const date = `${zdt.year}-${String(zdt.month).padStart(2, '0')}-${String(zdt.day).padStart(2, '0')}`
  const time = `${String(zdt.hour).padStart(2, '0')}:${String(zdt.minute).padStart(2, '0')}`
  return { date, time, tz: tzAbbr(displayZone, date) }
}

/** 当前时刻（本地 Instant，用于比较） */
export function nowInstant() {
  return Temporal.Now.instant()
}

/** 某 IANA 时区下的今天（YYYY-MM-DD），用于 D-Day 等按官方时区计数的场景 */
export function todayKeyInZone(zone) {
  const zdt = Temporal.Now.zonedDateTimeISO(zone || browserZone())
  return `${zdt.year}-${String(zdt.month).padStart(2, '0')}-${String(zdt.day).padStart(2, '0')}`
}

/** 星期短标签（THU / 四 / 목），用于 This Week 分组 */
export function weekdayShort(dateKey, locale) {
  const d = new Date(`${dateKey}T12:00:00`)
  const W_EN = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
  if (locale === 'zh-CN') return '周' + ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  if (locale === 'ko') return ['일', '월', '화', '수', '목', '금', '토'][d.getDay()]
  return W_EN[d.getDay()]
}

/** This Week 分组标题：'THU · AUG 20' / '周四 · 8月20日' / '목 · 8월 20일' */
export function weekGroupLabel(dateKey, locale) {
  const [y, m, d] = dateKey.split('-').map(Number)
  const w = weekdayShort(dateKey, locale)
  if (locale === 'zh-CN') return `${w} · ${m}月${d}日`
  if (locale === 'ko') return `${w} · ${m}월 ${d}일`
  const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
  return `${w} · ${MONTHS[m - 1]} ${d}`
}
