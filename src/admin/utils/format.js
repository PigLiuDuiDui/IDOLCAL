// ============================================================
// 管理后台时间工具：后端一律返回 UTC epoch millis，前端本地时区展示
// ============================================================
import { t } from '../i18n'

/** 完整时间：2026-08-21 09:30 */
export function fmtDateTime(ms, withSeconds = false) {
  if (ms === null || ms === undefined) return '-'
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  const sec = withSeconds ? `:${p(d.getSeconds())}` : ''
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}${sec}`
}

/** 仅时间：09:30 */
export function fmtTime(ms) {
  if (ms === null || ms === undefined) return '-'
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 仅日期：2026-08-21 */
export function fmtDate(ms) {
  if (ms === null || ms === undefined) return '-'
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 相对时间：3 分钟前 / 2 小时前 / 昨天 / 3 天前 / 具体日期 */
export function timeAgo(ms) {
  if (ms === null || ms === undefined) return '-'
  const diff = Date.now() - ms
  if (diff < 0) return fmtDateTime(ms)
  const min = 60_000
  const hour = 3_600_000
  const day = 86_400_000
  if (diff < min) return t('time.justNow')
  if (diff < hour) return t('time.minAgo', { n: Math.floor(diff / min) })
  if (diff < day) return t('time.hourAgo', { n: Math.floor(diff / hour) })
  if (diff < 2 * day) return t('time.yesterday')
  if (diff < 7 * day) return t('time.dayAgo', { n: Math.floor(diff / day) })
  return fmtDate(ms)
}

/** 持续时间：58.2s / 1m 24s */
export function fmtDuration(ms) {
  if (ms === null || ms === undefined || ms < 0) return '-'
  const totalSec = ms / 1000
  if (totalSec < 60) return `${totalSec.toFixed(1)}s`
  const m = Math.floor(totalSec / 60)
  const s = Math.round(totalSec % 60)
  return `${m}m ${s}s`
}

/** 运行时长：3d 4h / 2h 15m / 45s */
export function fmtUptime(ms) {
  if (ms === null || ms === undefined) return '-'
  const day = 86_400_000
  const hour = 3_600_000
  const min = 60_000
  if (ms >= day) return `${Math.floor(ms / day)}d ${Math.floor((ms % day) / hour)}h`
  if (ms >= hour) return `${Math.floor(ms / hour)}h ${Math.floor((ms % hour) / min)}m`
  if (ms >= min) return `${Math.floor(ms / min)}m ${Math.floor((ms % min) / 1000)}s`
  return `${Math.round(ms / 1000)}s`
}

/** 大数字格式化：12842 → 12,842 */
export function fmtNum(n) {
  if (n === null || n === undefined) return '-'
  return Number(n).toLocaleString('en-US')
}

/** 存储格式化：282 MB / 1.2 GB */
export function fmtBytes(bytes) {
  if (bytes === null || bytes === undefined) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let v = bytes
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${i === 0 ? v : v.toFixed(1)} ${units[i]}`
}

/** epoch day → MM-DD（趋势图横轴） */
export function dayLabel(epochDay) {
  const d = new Date(epochDay * 86_400_000)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
