#!/usr/bin/env node
// ============================================================
// 生成日历订阅文件 public/calendar.ics（RFC 5545）
// 构建时自动运行（见 package.json 的 build 脚本），也可单独运行：
//   node scripts/gen-ics.js
// 数据源：优先后端 API（IDOLCAL_API 可覆盖，默认 http://localhost:8080），
//         后端不可用时回退本地 src/data/*.js（纯静态部署场景）。
// 产物 public/calendar.ics 会被 Vite 复制到 dist/ 随站点部署，
// 粉丝通过 webcal:// 链接订阅后，手机日历自动同步所有活动。
// ============================================================

import { mkdirSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const API_BASE = process.env.IDOLCAL_API || 'http://localhost:8080'

async function getJson(apiPath) {
  const res = await fetch(`${API_BASE}${apiPath}`)
  if (!res.ok) throw new Error(`API ${apiPath} → ${res.status}`)
  return res.json()
}

// ---- 数据源加载（API 优先，失败回退本地） ----
let events, TYPE_LABEL, currentArtist

try {
  const [ev, meta] = await Promise.all([getJson('/api/events'), getJson('/api/meta')])
  const artists = await getJson('/api/artists')
  events = ev
  TYPE_LABEL = Object.fromEntries(meta.eventTypes.map((t) => [t.id, t.label]))
  currentArtist = artists.find((a) => a.current) || artists[0]
  console.log('[gen-ics] 数据源：后端 API')
} catch (err) {
  console.warn(`[gen-ics] 后端 API 不可用（${err.message}），回退本地数据`)
  ;({ events, TYPE_LABEL } = await import('../src/data/events.js'))
  ;({ currentArtist } = await import('../src/data/artists.js'))
}

/** 多语言对象取中文回退英文；纯字符串原样返回（如 sourceName） */
const t = (f, fb = '') => {
  if (typeof f === 'string') return f
  return (f && (f['zh-CN'] || f.en)) || fb
}

/** RFC 5545 文本转义（反斜杠 / 分号 / 逗号 / 换行） */
const esc = (s) => s.replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\n/g, '\\n')

/** 长行按 74 字节折叠（RFC 5545：折行以 CRLF+空格 续行，避免截断多字节字符） */
function fold(line) {
  if (Buffer.byteLength(line, 'utf8') <= 75) return line
  const out = []
  let cur = ''
  for (const ch of line) {
    if (Buffer.byteLength(cur + ch, 'utf8') > 74) {
      out.push(cur)
      cur = ch
    } else {
      cur += ch
    }
  }
  out.push(cur)
  return out.join('\r\n ')
}

/** 'YYYY-MM-DD' + 'HH:MM' -> KST 时间字符串 'YYYYMMDDTHHMM00' */
function kstDateTime(dateKey, time) {
  return `${dateKey.replaceAll('-', '')}T${time.replace(':', '')}00`
}

/** dateKey 偏移 n 天（'2026-11-29'） */
function addDays(dateKey, n) {
  const [y, m, d] = dateKey.split('-').map(Number)
  const dt = new Date(Date.UTC(y, m - 1, d) + n * 86400000)
  const p = (x) => String(x).padStart(2, '0')
  return `${dt.getUTCFullYear()}-${p(dt.getUTCMonth() + 1)}-${p(dt.getUTCDate())}`
}

/** 单条活动 -> VEVENT 行数组 */
function eventLines(e) {
  const allDay = e.time === '00:00' // 00:00 视为全天事件（如生日）

  const start = allDay
    ? `DTSTART;VALUE=DATE:${e.date.replaceAll('-', '')}`
    : `DTSTART;TZID=Asia/Seoul:${kstDateTime(e.date, e.time)}`

  let end
  if (allDay) {
    // 全天事件：DTEND 为结束日（次日，无 endDate 时）
    end = `DTEND;VALUE=DATE:${(e.endDate || addDays(e.date, 1)).replaceAll('-', '')}`
  } else if (e.endDate) {
    // 跨日活动：结束日同时刻（如 11-28 18:00 至 11-29 18:00）
    end = `DTEND;TZID=Asia/Seoul:${kstDateTime(e.endDate, e.time)}`
  } else {
    // 无 endDate：按开始时间 +1 小时估算
    const [y, m, d] = e.date.split('-').map(Number)
    const [hh, mm] = e.time.split(':').map(Number)
    const dt = new Date(Date.UTC(y, m - 1, d, hh, mm) + 3600000)
    const p = (x) => String(x).padStart(2, '0')
    end = `DTEND;TZID=Asia/Seoul:${dt.getUTCFullYear()}${p(dt.getUTCMonth() + 1)}${p(dt.getUTCDate())}T${p(dt.getUTCHours())}${p(dt.getUTCMinutes())}00`
  }

  const status = e.status === 'CONFIRMED' ? 'CONFIRMED' : 'TENTATIVE'
  const summary = `${t(TYPE_LABEL[e.type])} · ${t(e.title)}`
  const descParts = [t(e.description)]
  if (e.status !== 'CONFIRMED') descParts.push(`状态：${e.status}（以官方渠道为准）`)
  descParts.push(`来源：${t(e.sourceName) || '—'}`)

  const lines = [
    'BEGIN:VEVENT',
    `UID:${e.id}@idolcal`,
    `DTSTAMP:${stamp}`,
    start,
    end,
    `SUMMARY:${esc(summary)}`,
    `DESCRIPTION:${esc(descParts.filter(Boolean).join('\n'))}`
  ]
  if (t(e.location)) lines.push(`LOCATION:${esc(t(e.location))}`)
  if (e.sourceUrl) lines.push(`URL:${esc(e.sourceUrl)}`)
  lines.push(`STATUS:${status}`, 'END:VEVENT')
  return lines
}

// ---- 组装 ----
const now = new Date()
const p = (x) => String(x).padStart(2, '0')
const stamp = `${now.getUTCFullYear()}${p(now.getUTCMonth() + 1)}${p(now.getUTCDate())}T${p(now.getUTCHours())}${p(now.getUTCMinutes())}${p(now.getUTCSeconds())}Z`

const body = [
  'BEGIN:VCALENDAR',
  'VERSION:2.0',
  'PRODID:-//IdolCal//EVAN Official Schedule//ZH-CN',
  'CALSCALE:GREGORIAN',
  'METHOD:PUBLISH',
  `X-WR-CALNAME:${esc(`${currentArtist.name} · ${t({ en: 'Official Schedule', 'zh-CN': '官方日程', ko: '공식 일정' })}`)}`,
  'X-WR-TIMEZONE:Asia/Seoul',
  `X-APPLE-CALENDAR-COLOR:${currentArtist.accent}`,
  ...events.flatMap(eventLines),
  'END:VCALENDAR'
]
  .map(fold)
  .join('\r\n')

const outPath = path.join(__dirname, '..', 'public', 'calendar.ics')
mkdirSync(path.dirname(outPath), { recursive: true })
writeFileSync(outPath, body + '\r\n', 'utf8')
console.log(`[gen-ics] 已生成 ${outPath}（${events.length} 个活动）`)
