// ============================================================
// 艺人档案配置
// 数据独立于页面组件：更换 / 新增艺人时只需替换此文件
// 与 events.js 中的 artist 字段对应，预留多艺人支持
// 多语言字段：intro 为 { en, 'zh-CN', ko }
// ============================================================

export const artists = [
  {
    id: 'evan',
    name: 'EVAN',
    subName: 'OFFICIAL SCHEDULE',
    year: '2026',
    era: 'DEATH OF ME',
    eraPeriod: 'AUG — NOV 2026',
    // 克制使用的强调色（仅用于重要活动 / 当前状态 / Countdown / Active Filter）
    accent: '#a62f2f',
    accentSoft: '#efe7e4',
    heroImage: null, // 预留官方视觉图片，空则使用排版式 Hero
    sourceTag: 'Official',
    intro: {
      en: 'EVAN\u2019s official schedule archive. All schedules come from public information on official channels, labeled with source and credibility — unverified unofficial content is never spread.',
      'zh-CN':
        'EVAN 的官方活动档案。所有日程均来自官方渠道公开信息，标注来源与可信度，不传播未经证实的非官方内容。',
      ko: 'EVAN의 공식 일정 아카이브. 모든 일정은 공식 채널의 공개 정보에서 가져왔으며, 출처와 신뢰도를 표시하고 확인되지 않은 비공식 내용은 유포하지 않습니다.'
    }
  }
]

/** 当前展示的艺人（未来可切换） */
export const currentArtist = artists[0]

export function getArtist(id) {
  return artists.find((a) => a.id === id) || currentArtist
}
