#!/usr/bin/env node
// ============================================================
// 微信推送脚本：读取 src/data/events.js，推送今天 / 明天的活动
//
// 通道（三选一）：
//   - 企业微信群机器人（推荐） 群设置 -> 群机器人 -> 添加，复制 webhook
//   - Server酱³              https://sct.ftqq.com   免费版每日 5 条，需关注公众号
//   - PushPlus              https://www.pushplus.plus
//
// 密钥来源（按优先级）：命令行参数 > 环境变量 > scripts/.env
//   node scripts/notify.js --wecom <WEBHOOK_URL>   # 使用企业微信群机器人
//   node scripts/notify.js --sct <SENDKEY>         # 使用 Server酱
//   node scripts/notify.js --pushplus <TOKEN>      # 使用 PushPlus
//   node scripts/notify.js --test                  # 发送测试消息验证通道
//
// 定时触发（Windows 任务计划程序，每天 08:30 执行一次）：
//   schtasks /Create /TN "IdolCalNotify" /SC DAILY /ST 08:30 /TR "node d:\home\project\IdolCal\scripts\notify.js"
// ============================================================

import { readFileSync, existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// ---------- 密钥解析：命令行参数 > 环境变量 > scripts/.env ----------
const args = process.argv.slice(2)
const argVal = (flag) => {
  const i = args.indexOf(flag)
  return i >= 0 ? args[i + 1] : undefined
}

const envPath = path.join(__dirname, '.env')
if (existsSync(envPath)) {
  for (const line of readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/)
    if (m && process.env[m[1]] === undefined) process.env[m[1]] = m[2]
  }
}

const wecomWebhook = argVal('--wecom') || process.env.WECOM_WEBHOOK
const sctKey = argVal('--sct') || process.env.SCT_KEY
const pushplusToken = argVal('--pushplus') || process.env.PUSHPLUS_TOKEN
const isTest = args.includes('--test')

// ---------- 数据 ----------
const { events, TYPE_LABEL, TYPE_MARKER } = await import('../src/data/events.js')

const MS_DAY = 24 * 60 * 60 * 1000

/** 指定时区下的 YYYY-MM-DD（offsetDays 可偏移天数） */
function dateKeyIn(timeZone, offsetDays = 0) {
  const d = new Date(Date.now() + offsetDays * MS_DAY)
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(d)
  const get = (t) => parts.find((p) => p.type === t).value
  return `${get('year')}-${get('month')}-${get('day')}`
}

// 活动数据为 KST（韩国时间）语义，按首尔时区判定"今天 / 明天"
const today = dateKeyIn('Asia/Seoul')
const tomorrow = dateKeyIn('Asia/Seoul', 1)

/** 多语言对象取中文回退英文；纯字符串原样返回（如 sourceName） */
function t(field, fallback = '') {
  if (typeof field === 'string') return field
  const v = field && (field['zh-CN'] || field.en)
  return v || fallback
}

/** '2026-08-18' -> '8月18日' */
function fmtDate(key) {
  const [, m, d] = key.split('-')
  return `${Number(m)}月${Number(d)}日`
}

/** 单条活动渲染（markdown 行） */
function renderEvent(e) {
  const marker = TYPE_MARKER[e.type] || '●'
  const label = t(TYPE_LABEL[e.type])
  const time = e.time ? `${e.time} KST` : '时间待定'
  const lines = [
    `${marker} **${t(e.title)}**（${label}）`,
    `${time} · ${t(e.location) || '—'}`,
    e.sourceUrl ? `[${t(e.sourceName) || '来源'}](${e.sourceUrl})` : t(e.sourceName) || ''
  ].filter(Boolean)
  return lines.join('\n')
}

function buildMessage() {
  const todays = events.filter((e) => e.date === today)
  const tomorrows = events.filter((e) => e.date === tomorrow)
  return { todays, tomorrows }
}

// ---------- 推送通道 ----------
async function send(title, markdown) {
  if (wecomWebhook) {
    const res = await fetch(wecomWebhook, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ msgtype: 'markdown', markdown: { content: `## ${title}\n${markdown}` } })
    })
    const json = await res.json()
    if (json.errcode !== 0) throw new Error(`企业微信推送失败：${json.errmsg || JSON.stringify(json)}`)
    console.log(`[IdolCal] 企业微信推送成功：${title}`)
  } else if (sctKey) {
    const res = await fetch(`https://sctapi.ftqq.com/${sctKey}.send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ title, desp: markdown })
    })
    const json = await res.json()
    if (json.code !== 0) throw new Error(`Server酱推送失败：${json.message || JSON.stringify(json)}`)
    console.log(`[IdolCal] Server酱推送成功：${title}`)
  } else if (pushplusToken) {
    const res = await fetch('https://www.pushplus.plus/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: pushplusToken, title, content: markdown, template: 'markdown' })
    })
    const json = await res.json()
    if (json.code !== 200) throw new Error(`PushPlus 推送失败：${json.msg || JSON.stringify(json)}`)
    console.log(`[IdolCal] PushPlus 推送成功：${title}`)
  } else {
    throw new Error(
      '未配置推送密钥：请传入 --wecom <WEBHOOK_URL> / --sct <SENDKEY> / --pushplus <TOKEN>，\n' +
      '或在 scripts/.env 中配置 WECOM_WEBHOOK / SCT_KEY / PUSHPLUS_TOKEN'
    )
  }
}

// ---------- 主流程 ----------
try {
  if (isTest) {
    await send('IdolCal 测试消息', '### 通道测试成功\n\n推送通道可用，日程提醒即将生效。')
    process.exit(0)
  }

  const { todays, tomorrows } = buildMessage()
  if (todays.length === 0 && tomorrows.length === 0) {
    console.log(`[IdolCal] ${today}：今天和明天均无活动，跳过推送`)
    process.exit(0)
  }

  const section = (title, list) =>
    list.length === 0 ? null : `### ${title}\n\n${list.map(renderEvent).join('\n\n')}`

  const parts = [
    section(`今日日程 · ${fmtDate(today)}（KST）`, todays),
    section(`明日日程 · ${fmtDate(tomorrow)}（KST）`, tomorrows)
  ].filter(Boolean)

  const desp = parts.join('\n\n---\n\n') + '\n\n---\n来源：IdolCal 官方日程档案'
  const title = `EVAN 日程提醒 · ${fmtDate(today)}`

  await send(title, desp)
} catch (err) {
  console.error(`[IdolCal] ${err.message}`)
  process.exit(1)
}
