// ============================================================
// Comeback Hub / 回归专题数据
// 只定义专题结构与节点顺序，节点活动一律复用 events.js 中的
// 现有事件（eventIds 引用），不重复创建活动数据。
// ============================================================

// ---- 回归时间线标准节点顺序（i18n label 见 comeback.stages.*）----
export const COMEBACK_STAGES = [
  { id: 'concept-photo', key: 'conceptPhoto' },
  { id: 'tracklist', key: 'tracklist' },
  { id: 'highlight-medley', key: 'highlightMedley' },
  { id: 'mv-teaser', key: 'mvTeaser' },
  { id: 'album-release', key: 'albumRelease' },
  { id: 'showcase', key: 'showcase' },
  { id: 'music-shows', key: 'musicShows' }
]

// ---- 回归专题列表（支持多个；当前为 DEATH OF ME 出道时期）----
export const comebacks = [
  {
    id: 'death-of-me',
    artistId: 'evan',
    title: 'DEATH OF ME',
    tagline: { en: 'THE FIRST ERA', 'zh-CN': '第一个时期', ko: '첫 번째 시대' },
    releaseDate: '2026-09-07',
    releaseTime: '18:00',
    releaseTimezone: 'KST',
    // 每个节点引用现有事件；多事件节点（music-shows）按时间展示
    stages: [
      { stage: 'concept-photo', eventIds: ['e009'] },
      { stage: 'tracklist', eventIds: ['e010'] },
      { stage: 'highlight-medley', eventIds: ['e024'] },
      { stage: 'mv-teaser', eventIds: ['e011'] },
      { stage: 'album-release', eventIds: ['e012'] },
      { stage: 'showcase', eventIds: ['e013'] },
      { stage: 'music-shows', eventIds: ['e014', 'e015'] }
    ]
  }
]

/** 当前回归专题（未来可切换） */
export const currentComeback = comebacks[0]

export function getComeback(id) {
  return comebacks.find((c) => c.id === id) || currentComeback
}
