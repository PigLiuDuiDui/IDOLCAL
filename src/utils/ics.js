// ============================================================
// 前端 ICS 生成器（RFC 5545）
// 订阅面板「下载 .ics」时动态生成：每个活动可携带独立的 VALARM 提醒
// （优先使用用户已单独设置的提醒，未设置的活动用面板统一默认）。
// 与 scripts/gen-ics.js（静态订阅文件）保持相同的事件序列化规则，
// 时间一律使用官方时区（Asia/Seoul）。
// ============================================================

/** 多语言字段取中文回退英文；纯字符串原样返回 */
const t = (f, fb = '') => (typeof f === 'string' ? f : (f && (f['zh-CN'] || f.en)) || fb)

/** RFC 5545 文本转义（反斜杠 / 分号 / 逗号 / 换行） */
const esc = (s) =>
  String(s ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/;/g, '\\;')
    .replace(/,/g, '\\,')
    .replace(/\n/g, '\\n')

/** 长行按 74 字节折叠（RFC 5545：折行以 CRLF+空格 续行） */
function fold(line) {
  const size = (s) => new TextEncoder().encode(s).length
  if (size(line) <= 75) return line
  const out = []
  let cur = ''
  for (const ch of line) {
    if (size(cur + ch) > 74) {
      out.push(cur)
      cur = ch
    } else {
      cur += ch
    }
  }
  out.push(cur)
  return out.join('\r\n ')
}

/** 'YYYY-MM-DD' + 'HH:MM' -> 'YYYYMMDDTHHMM00' */
function kstDateTime(dateKey, time) {
  return `${dateKey.replaceAll('-', '')}T${time.replace(':', '')}00`
}

/** dateKey 偏移 n 天 */
function addDays(dateKey, n) {
  const [y, m, d] = dateKey.split('-').map(Number)
  const dt = new Date(Date.UTC(y, m - 1, d) + n * 86400000)
  const p = (x) => String(x).padStart(2, '0')
  return `${dt.getUTCFullYear()}-${p(dt.getUTCMonth() + 1)}-${p(dt.getUTCDate())}`
}

/**
 * 生成完整 ICS 文本
 * @param {object} opts
 * @param {Array} opts.events 活动数据
 * @param {object} opts.artist 当前艺人 { name, accent }
 * @param {object} opts.typeLabel 类型标签映射 { [typeId]: string | {zh-CN,en} }
 * @param {Function} [opts.alarmMinutesOf] (event) => number | null | undefined
 *   活动开始前分钟数；null/undefined/0 表示该活动不加提醒
 */
export function generateIcs({ events, artist, typeLabel, alarmMinutesOf = () => null }) {
  const now = new Date()
  const p = (x) => String(x).padStart(2, '0')
  const stamp = `${now.getUTCFullYear()}${p(now.getUTCMonth() + 1)}${p(now.getUTCDate())}T${p(
    now.getUTCHours()
  )}${p(now.getUTCMinutes())}${p(now.getUTCSeconds())}Z`

  const eventLines = (e) => {
    const allDay = e.time === '00:00' // 00:00 视为全天事件（如生日）

    const start = allDay
      ? `DTSTART;VALUE=DATE:${e.date.replaceAll('-', '')}`
      : `DTSTART;TZID=Asia/Seoul:${kstDateTime(e.date, e.time)}`

    let end
    if (allDay) {
      end = `DTEND;VALUE=DATE:${(e.endDate || addDays(e.date, 1)).replaceAll('-', '')}`
    } else if (e.endDate) {
      end = `DTEND;TZID=Asia/Seoul:${kstDateTime(e.endDate, e.time)}`
    } else {
      // 无 endDate：按开始时间 +1 小时估算
      const [y, m, d] = e.date.split('-').map(Number)
      const [hh, mm] = e.time.split(':').map(Number)
      const dt = new Date(Date.UTC(y, m - 1, d, hh, mm) + 3600000)
      end = `DTEND;TZID=Asia/Seoul:${dt.getUTCFullYear()}${p(dt.getUTCMonth() + 1)}${p(dt.getUTCDate())}T${p(
        dt.getUTCHours()
      )}${p(dt.getUTCMinutes())}00`
    }

    const status = e.status === 'CONFIRMED' ? 'CONFIRMED' : 'TENTATIVE'
    const summary = `${t(typeLabel[e.type])} · ${t(e.title)}`
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
    lines.push(`STATUS:${status}`)

    // 活动级提醒：每个活动可携带独立的 VALARM
    const alarmMinutes = Number(alarmMinutesOf(e))
    if (Number.isFinite(alarmMinutes) && alarmMinutes > 0) {
      lines.push(
        'BEGIN:VALARM',
        'ACTION:DISPLAY',
        `DESCRIPTION:${esc(summary)}`,
        `TRIGGER:-PT${alarmMinutes}M`,
        'END:VALARM'
      )
    }

    lines.push('END:VEVENT')
    return lines
  }

  const body = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//IdolCal//EVAN Official Schedule//ZH-CN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    `X-WR-CALNAME:${esc(`${artist?.name || ''} · ${t({ en: 'Official Schedule', 'zh-CN': '官方日程', ko: '공식 일정' })}`)}`,
    'X-WR-TIMEZONE:Asia/Seoul',
    `X-APPLE-CALENDAR-COLOR:${artist?.accent || '#6C5CE7'}`,
    ...(events || []).flatMap(eventLines),
    'END:VCALENDAR'
  ]
    .map(fold)
    .join('\r\n')

  return body + '\r\n'
}
