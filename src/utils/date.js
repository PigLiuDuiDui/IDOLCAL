// 日期工具函数
// 多语言日期格式：通过 i18n 实例读取当前语言（响应式）
import i18n from '../i18n'

/** 当前 i18n locale（'en' / 'zh-CN' / 'ko'） */
function locale() {
  return i18n.global.locale.value
}

export function pad2(n) {
  return String(n).padStart(2, '0')
}

/**
 * 将 年/月/日 格式化为 YYYY-MM-DD
 * @param {number} year
 * @param {number} month 1-based
 * @param {number} day
 */
export function formatDate(year, month, day) {
  return `${year}-${pad2(month)}-${pad2(day)}`
}

/** Date 对象 -> YYYY-MM-DD */
export function toDateKey(date) {
  return formatDate(date.getFullYear(), date.getMonth() + 1, date.getDate())
}

/** YYYY-MM-DD -> Date 对象（本地时区当日零点） */
export function parseDateKey(key) {
  const [y, m, d] = key.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** 今天的 YYYY-MM-DD */
export function todayKey() {
  return toDateKey(new Date())
}

/** 两日期相差天数（dateKey2 - dateKey1） */
export function diffDays(dateKey1, dateKey2) {
  const MS_DAY = 24 * 60 * 60 * 1000
  return Math.round((parseDateKey(dateKey2) - parseDateKey(dateKey1)) / MS_DAY)
}

/** 距某天的倒计时：今天为 D-DAY，明天为 D-1，未来为 D+n */
export function dDayLabel(dateKey) {
  const diff = diffDays(todayKey(), dateKey)
  if (diff === 0) return 'D-DAY'
  return `D-${diff}`
}

/** 倒计时文本：正数返回 'D-19'，负数返回 'D+3'（已过去），0 返回 'D-DAY' */
export function countdownLabel(dateKey) {
  const diff = diffDays(todayKey(), dateKey)
  if (diff === 0) return 'D-DAY'
  return diff > 0 ? `D-${diff}` : `D+${-diff}`
}

/** YYYY-MM-DD -> '08.18'（数字短日期，三语通用） */
export function shortDate(dateKey) {
  const [, m, d] = dateKey.split('-')
  return `${m}.${d}`
}

/** YYYY-MM-DD -> 按语言格式化完整日期（'Aug 18, 2026' / '2026年8月18日' / '2026년 8월 18일'） */
export function fullDate(dateKey) {
  const [y, m, d] = dateKey.split('-').map(Number)
  const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  const MONTHS_ZH = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']
  const MONTHS_KO = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12']
  const l = locale()
  if (l === 'zh-CN') return `${y}年${MONTHS_ZH[m - 1]}月${d}日`
  if (l === 'ko') return `${y}년 ${MONTHS_KO[m - 1]}월 ${d}일`
  return `${MONTHS[m - 1]} ${d}, ${y}`
}

/** YYYY-MM-DD -> 杂志式日期（'18 AUG 2026' / '2026.08.18'） */
export function editorialDate(dateKey) {
  const [y, m, d] = dateKey.split('-').map(Number)
  const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
  const l = locale()
  if (l === 'en') return `${pad2(d)} ${MONTHS[m - 1]} ${y}`
  return `${y}.${pad2(m)}.${pad2(d)}`
}

/** YYYY-MM-DD -> 月份（YYYY 年 M 月，0-based 兼容） */
export function monthKeyOf(dateKey) {
  return dateKey.slice(0, 7) // 'YYYY-MM'
}

/** 'YYYY-MM' -> 月份标题（'AUGUST 2026' / '2026年8月' / '2026년 8월'） */
export function monthLabel(monthKey) {
  const [y, m] = monthKey.split('-').map(Number)
  const MONTHS = [
    'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
    'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'
  ]
  const l = locale()
  if (l === 'zh-CN') return `${y}年${m}月`
  if (l === 'ko') return `${y}년 ${m}월`
  return `${MONTHS[m - 1]} ${y}`
}

/**
 * 生成某月的月历网格（含前后月补位格）
 * @param {number} year
 * @param {number} month 0-based
 * @returns {{key: string, day: number, inMonth: boolean}[]}
 */
export function getMonthGrid(year, month) {
  const first = new Date(year, month, 1)
  const startWeekday = first.getDay() // 0=周日
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells = []

  // 上个月补位
  const prevYear = month === 0 ? year - 1 : year
  const prevMonth = month === 0 ? 12 : month // 1-based
  const prevDays = new Date(year, month, 0).getDate()
  for (let i = startWeekday - 1; i >= 0; i--) {
    cells.push({
      key: formatDate(prevYear, prevMonth, prevDays - i),
      day: prevDays - i,
      inMonth: false
    })
  }

  // 当月
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push({
      key: formatDate(year, month + 1, d),
      day: d,
      inMonth: true
    })
  }

  // 下个月补位，凑满整周
  const total = cells.length
  const remaining = total % 7 === 0 ? 0 : 7 - (total % 7)
  const nextYear = month === 11 ? year + 1 : year
  const nextMonth = month === 11 ? 1 : month + 2 // 1-based
  for (let i = 1; i <= remaining; i++) {
    cells.push({
      key: formatDate(nextYear, nextMonth, i),
      day: i,
      inMonth: false
    })
  }

  return cells
}

/** 星期表头（按语言：['日','一',...] / ['SUN',...] / ['일','월',...]） */
export function weekdayLabels() {
  const l = locale()
  if (l === 'zh-CN') return ['日', '一', '二', '三', '四', '五', '六']
  if (l === 'ko') return ['일', '월', '화', '수', '목', '금', '토']
  return ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
}

/** 英文星期表头（日历用） */
export function weekdayLabelsEn() {
  return ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
}
