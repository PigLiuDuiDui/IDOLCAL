// ============================================================
// 导出前端数据快照 → api 后端初始化种子文件
// 用法：npm run export:seed
// 产物：api/src/main/resources/seed/{events,artists,comebacks,tutorials,meta}.json
// 后端首次启动（表为空）时由 DataSeeder 导入；之后以数据库为准，
// 本脚本仅用于“从旧数据文件迁移/重建”场景。
// ============================================================

import { writeFileSync, mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..')
const outDir = path.join(root, 'api', 'src', 'main', 'resources', 'seed')

const { events, EVENT_TYPES, STATUS, SOURCE_LEVELS } = await import('../src/data/events.js')
const { artists, currentArtist } = await import('../src/data/artists.js')
const { comebacks, COMEBACK_STAGES } = await import('../src/data/comebacks.js')
const { tutorialBoards } = await import('../src/data/tutorials.js')

mkdirSync(outDir, { recursive: true })

writeFileSync(path.join(outDir, 'events.json'), JSON.stringify(events, null, 2), 'utf8')
// current 字段为后端特有（标记当前展示艺人），导出时按前端 currentArtist 补齐
writeFileSync(
  path.join(outDir, 'artists.json'),
  JSON.stringify(artists.map((a) => ({ ...a, current: a.id === currentArtist.id })), null, 2),
  'utf8'
)
writeFileSync(path.join(outDir, 'comebacks.json'), JSON.stringify(comebacks, null, 2), 'utf8')
writeFileSync(path.join(outDir, 'tutorials.json'), JSON.stringify(tutorialBoards, null, 2), 'utf8')

const meta = {
  eventTypes: EVENT_TYPES,
  statuses: STATUS,
  sourceLevels: SOURCE_LEVELS,
  comebackStages: COMEBACK_STAGES
}
writeFileSync(path.join(outDir, 'meta.json'), JSON.stringify(meta, null, 2), 'utf8')

console.log(`[export-seed] 已生成 ${outDir}（events:${events.length} artists:${artists.length} comebacks:${comebacks.length} tutorials:${tutorialBoards.length} meta:✓）`)
