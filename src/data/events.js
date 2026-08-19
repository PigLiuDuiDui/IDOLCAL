// ============================================================
// 活动数据（独立于页面组件，组件只负责渲染）
// 字段结构：id / artist / date / endDate / time / timezone /
//           title / type / status / location / description /
//           image / sourceName / sourceUrl / isOfficial /
//           onlineUrl / mapUrl
// 多语言字段：title / location / description 为 { en, 'zh-CN', ko }
//            （sourceName 为品牌/账号名，保持原文不翻译）
// 更换艺人时只需替换本文件与 artists.js
// ============================================================

// ---- 活动类型定义（筛选器 / 日历标记 / 卡片标签共用）----
export const EVENT_TYPES = [
  { id: 'RELEASE', label: { en: 'RELEASE', 'zh-CN': '回归', ko: '컴백' }, marker: '●' },
  { id: 'EVENT', label: { en: 'EVENT', 'zh-CN': '活动', ko: '이벤트' }, marker: '◆' },
  { id: 'TV', label: { en: 'TV', 'zh-CN': '综艺', ko: '예능' }, marker: '▲' },
  { id: 'LIVE', label: { en: 'LIVE', 'zh-CN': '直播', ko: '라이브' }, marker: '▶' },
  { id: 'PHOTO', label: { en: 'PHOTO', 'zh-CN': '写真', ko: '화보' }, marker: '✳' },
  { id: 'MAGAZINE', label: { en: 'MAGAZINE', 'zh-CN': '杂志', ko: '매거진' }, marker: '■' },
  { id: 'OFFLINE', label: { en: 'OFFLINE', 'zh-CN': '线下', ko: '오프라인' }, marker: '＋' },
  { id: 'BRAND', label: { en: 'BRAND', 'zh-CN': '品牌', ko: '브랜드' }, marker: '◈' },
  { id: 'BIRTHDAY', label: { en: 'BIRTHDAY', 'zh-CN': '生日', ko: '생일' }, marker: '☆' }
]

export const TYPE_LABEL = Object.fromEntries(EVENT_TYPES.map((t) => [t.id, t.label]))
export const TYPE_MARKER = Object.fromEntries(EVENT_TYPES.map((t) => [t.id, t.marker]))

// ---- 事件状态（id 用于 data-status 样式，显示文本走 i18n）----
export const STATUS = {
  CONFIRMED: 'CONFIRMED',
  TBA: 'TBA',
  RUMORED: 'RUMORED'
}

// ---- 官方来源可信度 ----
export const SOURCE_LEVELS = {
  OFFICIAL: 'OFFICIAL',
  BRAND: 'BRAND',
  MEDIA: 'MEDIA',
  FAN: 'FAN PROJECT'
}

const W = 'https://weverse.io' // 示例来源（占位）
const X = 'https://x.com' // 示例来源（占位）
const YT = 'https://youtube.com' // 示例来源（占位）

// ---- 活动数据 ----
export const events = [
  // ================= 2026 年 5 月（历史 / Archive） =================
  {
    id: 'e001',
    artist: 'evan',
    date: '2026-05-28',
    endDate: null,
    time: '12:00',
    timezone: 'KST',
    title: { en: 'DEBUT ANNOUNCEMENT', 'zh-CN': '出道公告', ko: '데뷔 공지' },
    type: 'EVENT',
    status: STATUS.CONFIRMED,
    location: { en: 'Online', 'zh-CN': '线上', ko: '온라인' },
    description: {
      en: 'Official announcement of EVAN\u2019s solo debut. The beginning of the upcoming era.',
      'zh-CN': 'EVAN 个人出道的官方公告。即将开启新时期的序幕。',
      ko: 'EVAN 솔로 데뷔 공식 발표. 다가오는 시대의 시작.'
    },
    image: null,
    sourceName: 'EVAN Official X',
    sourceUrl: X,
    isOfficial: true,
    onlineUrl: X,
    mapUrl: null
  },

  // ================= 2026 年 6 月（历史 / Archive） =================
  {
    id: 'e002',
    artist: 'evan',
    date: '2026-06-11',
    endDate: null,
    time: '18:00',
    timezone: 'KST',
    title: { en: 'DEBUT TRAILER \u2014 WHO IS EVAN', 'zh-CN': '出道预告 — WHO IS EVAN', ko: '데뷔 트레일러 — WHO IS EVAN' },
    type: 'RELEASE',
    status: STATUS.CONFIRMED,
    location: { en: 'YouTube', 'zh-CN': 'YouTube', ko: 'YouTube' },
    description: {
      en: 'A cinematic debut trailer introducing EVAN to the world. Directed with a film-like aesthetic.',
      'zh-CN': '向世界介绍 EVAN 的电影感出道预告片，以影像美学为执导方向。',
      ko: 'EVAN을 세상에 소개하는 시네마틱 데뷔 트레일러. 영화 같은 미학으로 연출되었습니다.'
    },
    image: null,
    sourceName: 'EVAN Official YouTube',
    sourceUrl: YT,
    isOfficial: true,
    onlineUrl: YT,
    mapUrl: null
  },
  {
    id: 'e003',
    artist: 'evan',
    date: '2026-06-24',
    endDate: null,
    time: '12:00',
    timezone: 'KST',
    title: { en: 'PROFILE PHOTO RELEASE', 'zh-CN': '概念照公开', ko: '프로필 사진 공개' },
    type: 'PHOTO',
    status: STATUS.CONFIRMED,
    location: { en: 'Weverse', 'zh-CN': 'Weverse', ko: 'Weverse' },
    description: {
      en: 'Official profile photos revealing the visual direction of the debut era.',
      'zh-CN': '公开出道时期视觉方向的官方概念照。',
      ko: '데뷔 시대의 비주얼 방향을 보여주는 공식 프로필 사진 공개.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: W,
    mapUrl: null
  },

  // ================= 2026 年 7 月（历史 / Archive） =================
  {
    id: 'e004',
    artist: 'evan',
    date: '2026-07-05',
    endDate: null,
    time: '20:00',
    timezone: 'KST',
    title: { en: 'SOLO LIVE — FIRST GREETING', 'zh-CN': '个人直播 — FIRST GREETING', ko: '솔로 라이브 — FIRST GREETING' },
    type: 'LIVE',
    status: STATUS.CONFIRMED,
    location: { en: 'Weverse Live', 'zh-CN': 'Weverse 直播', ko: 'Weverse 라이브' },
    description: {
      en: 'First live broadcast with fans before the official debut. Introduction of the upcoming album.',
      'zh-CN': '正式出道前的首次粉丝直播，介绍即将发行的专辑。',
      ko: '공식 데뷔 전 팬들과의 첫 라이브 방송. 곧 발매될 앨범 소개.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: W,
    mapUrl: null
  },
  {
    id: 'e005',
    artist: 'evan',
    date: '2026-07-18',
    endDate: null,
    time: '09:00',
    timezone: 'KST',
    title: { en: 'MAGAZINE COVER — W KOREA', 'zh-CN': '杂志封面 — W KOREA', ko: '매거진 커버 — W KOREA' },
    type: 'MAGAZINE',
    status: STATUS.CONFIRMED,
    location: { en: 'W Korea', 'zh-CN': 'W Korea', ko: 'W Korea' },
    description: {
      en: 'First solo magazine cover. Interview and editorial photo spread.',
      'zh-CN': '首次个人杂志封面，包含专访与画报。',
      ko: '첫 단독 매거진 커버. 인터뷰와 화보 수록.'
    },
    image: null,
    sourceName: 'W Korea',
    sourceUrl: X,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },
  {
    id: 'e006',
    artist: 'evan',
    date: '2026-07-26',
    endDate: null,
    time: '14:00',
    timezone: 'KST',
    title: { en: 'DEBUT SHOWCASE PRESS CONFERENCE', 'zh-CN': '出道 Showcase 记者发布会', ko: '데뷔 쇼케이스 기자간담회' },
    type: 'EVENT',
    status: STATUS.CONFIRMED,
    location: { en: 'Seoul, KR', 'zh-CN': '韩国 首尔', ko: '서울, 대한민국' },
    description: {
      en: 'Press conference for media. Details of the debut showcase were partially disclosed.',
      'zh-CN': '面向媒体的记者发布会，部分公开出道 Showcase 的细节。',
      ko: '미디어 대상 기자간담회. 데뷔 쇼케이스 세부 내용 일부 공개.'
    },
    image: null,
    sourceName: 'EVAN Official X',
    sourceUrl: X,
    isOfficial: true,
    onlineUrl: null,
    mapUrl: null
  },

  // ================= 2026 年 8 月（DEATH OF ME 回归预热） =================
  {
    id: 'e007',
    artist: 'evan',
    date: '2026-08-01',
    endDate: null,
    time: '11:00',
    timezone: 'KST',
    title: { en: 'BRAND CAMPAIGN — LUMEN EYEWEAR', 'zh-CN': '品牌活动 — LUMEN EYEWEAR', ko: '브랜드 캠페인 — LUMEN EYEWEAR' },
    type: 'BRAND',
    status: STATUS.CONFIRMED,
    location: { en: 'Seoul, KR', 'zh-CN': '韩国 首尔', ko: '서울, 대한민국' },
    description: {
      en: 'Official brand campaign for LUMEN Eyewear. Photo and film content.',
      'zh-CN': 'LUMEN Eyewear 官方品牌活动，包含画报与影像内容。',
      ko: 'LUMEN Eyewear 공식 브랜드 캠페인. 화보 및 필름 콘텐츠.'
    },
    image: null,
    sourceName: 'LUMEN Eyewear',
    sourceUrl: X,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },
  {
    id: 'e008',
    artist: 'evan',
    date: '2026-08-14',
    endDate: null,
    time: '12:00',
    timezone: 'KST',
    title: { en: 'OFFICIAL ANNOUNCEMENT — NEW ERA', 'zh-CN': '官方公告 — 新篇章', ko: '공식 공지 — NEW ERA' },
    type: 'EVENT',
    status: STATUS.CONFIRMED,
    location: { en: 'Online', 'zh-CN': '线上', ko: '온라인' },
    description: {
      en: 'Official announcement of the new era. Title of the upcoming album revealed: DEATH OF ME.',
      'zh-CN': '新时期官方公告，公开新专辑名称：DEATH OF ME。',
      ko: '새 시대의 공식 공지. 새 앨범명 공개: DEATH OF ME.'
    },
    image: null,
    sourceName: 'EVAN Official X',
    sourceUrl: X,
    isOfficial: true,
    onlineUrl: X,
    mapUrl: null
  },
  {
    id: 'e009',
    artist: 'evan',
    date: '2026-08-20',
    endDate: null,
    time: '12:00',
    timezone: 'KST',
    title: { en: 'CONCEPT PHOTO — DEATH OF ME', 'zh-CN': '概念照 — DEATH OF ME', ko: '콘셉트 포토 — DEATH OF ME' },
    type: 'PHOTO',
    status: STATUS.CONFIRMED,
    location: { en: 'Weverse', 'zh-CN': 'Weverse', ko: 'Weverse' },
    description: {
      en: 'First concept photo set of the DEATH OF ME era. Dark, cinematic visual direction.',
      'zh-CN': 'DEATH OF ME 时期首组概念照，暗黑电影感视觉方向。',
      ko: 'DEATH OF ME 시대 첫 콘셉트 포토 세트. 다크하고 시네마틱한 비주얼 방향.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: W,
    mapUrl: null
  },
  {
    id: 'e010',
    artist: 'evan',
    date: '2026-08-25',
    endDate: null,
    time: '18:00',
    timezone: 'KST',
    title: { en: 'TRACKLIST REVEAL', 'zh-CN': '曲目列表公开', ko: '트랙리스트 공개' },
    type: 'RELEASE',
    status: STATUS.CONFIRMED,
    location: { en: 'Weverse', 'zh-CN': 'Weverse', ko: 'Weverse' },
    description: {
      en: 'Tracklist of the debut album revealed, including the title track.',
      'zh-CN': '公开出道专辑曲目列表（含主打歌）。',
      ko: '타이틀곡을 포함한 데뷔 앨범 트랙리스트 공개.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: W,
    mapUrl: null
  },

  // ================= 2026 年 9 月（回归周） =================
  {
    id: 'e011',
    artist: 'evan',
    date: '2026-09-01',
    endDate: null,
    time: '18:00',
    timezone: 'KST',
    title: { en: 'MV TEASER', 'zh-CN': 'MV 预告', ko: 'MV 티저' },
    type: 'RELEASE',
    status: STATUS.CONFIRMED,
    location: { en: 'YouTube', 'zh-CN': 'YouTube', ko: 'YouTube' },
    description: {
      en: 'Music video teaser of the title track.',
      'zh-CN': '主打歌 MV 预告片。',
      ko: '타이틀곡 뮤직비디오 티저.'
    },
    image: null,
    sourceName: 'EVAN Official YouTube',
    sourceUrl: YT,
    isOfficial: true,
    onlineUrl: YT,
    mapUrl: null
  },
  {
    id: 'e024',
    artist: 'evan',
    date: '2026-09-02',
    endDate: null,
    time: '18:00',
    timezone: 'KST',
    title: { en: 'HIGHLIGHT MEDLEY', 'zh-CN': '专辑试听片段', ko: '하이라이트 메들리' },
    type: 'RELEASE',
    status: STATUS.CONFIRMED,
    location: { en: 'YouTube', 'zh-CN': 'YouTube', ko: 'YouTube' },
    description: {
      en: 'Highlight medley preview of the debut album DEATH OF ME.',
      'zh-CN': '出道专辑 DEATH OF ME 的试听片段预览。',
      ko: '데뷔 앨범 DEATH OF ME 하이라이트 메들리 미리보기.'
    },
    image: null,
    sourceName: 'EVAN Official YouTube',
    sourceUrl: YT,
    isOfficial: true,
    onlineUrl: YT,
    mapUrl: null
  },
  {
    id: 'e012',
    artist: 'evan',
    date: '2026-09-07',
    endDate: null,
    time: '18:00',
    timezone: 'KST',
    title: { en: 'ALBUM RELEASE — DEATH OF ME', 'zh-CN': '专辑发行 — DEATH OF ME', ko: '앨범 발매 — DEATH OF ME' },
    type: 'RELEASE',
    status: STATUS.CONFIRMED,
    location: { en: 'All Platforms', 'zh-CN': '全平台', ko: '전 플랫폼' },
    description: {
      en: 'Debut album DEATH OF ME releases worldwide. Title track MV premieres simultaneously.',
      'zh-CN': '出道专辑 DEATH OF ME 全球发行，主打歌 MV 同步首播。',
      ko: '데뷔 앨범 DEATH OF ME 전 세계 발매. 타이틀곡 MV 동시 공개.'
    },
    image: null,
    sourceName: 'EVAN Official X',
    sourceUrl: X,
    isOfficial: true,
    onlineUrl: YT,
    mapUrl: null
  },
  {
    id: 'e013',
    artist: 'evan',
    date: '2026-09-07',
    endDate: null,
    time: '19:00',
    timezone: 'KST',
    title: { en: 'SOLO DEBUT SHOWCASE', 'zh-CN': '个人出道 Showcase', ko: '솔로 데뷔 쇼케이스' },
    type: 'EVENT',
    status: STATUS.CONFIRMED,
    location: { en: 'YES24 Live Hall, Seoul', 'zh-CN': '首尔 YES24 Live Hall', ko: '서울 YES24 라이브홀' },
    description: {
      en: 'Solo debut showcase with full live stage. First official performance of the new era.',
      'zh-CN': '完整舞台的个人出道 Showcase，新时期的首场官方演出。',
      ko: '풀 라이브 무대의 솔로 데뷔 쇼케이스. 새 시대의 첫 공식 공연.'
    },
    image: null,
    sourceName: 'EVAN Official X',
    sourceUrl: X,
    isOfficial: true,
    onlineUrl: null,
    mapUrl: 'https://maps.google.com'
  },
  {
    id: 'e014',
    artist: 'evan',
    date: '2026-09-08',
    endDate: null,
    time: '18:00',
    timezone: 'KST',
    title: { en: 'M COUNTDOWN', 'zh-CN': 'M COUNTDOWN', ko: 'M COUNTDOWN' },
    type: 'TV',
    status: STATUS.CONFIRMED,
    location: { en: 'CJ ENM, Seoul', 'zh-CN': '首尔 CJ ENM', ko: '서울 CJ ENM' },
    description: {
      en: 'First music show stage of the debut era.',
      'zh-CN': '出道时期首次音乐节目舞台。',
      ko: '데뷔 시대 첫 음악방송 무대.'
    },
    image: null,
    sourceName: 'Mnet',
    sourceUrl: YT,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },
  {
    id: 'e015',
    artist: 'evan',
    date: '2026-09-09',
    endDate: null,
    time: '17:05',
    timezone: 'KST',
    title: { en: 'MUSIC BANK', 'zh-CN': 'MUSIC BANK', ko: 'MUSIC BANK' },
    type: 'TV',
    status: STATUS.CONFIRMED,
    location: { en: 'KBS, Seoul', 'zh-CN': '首尔 KBS', ko: '서울 KBS' },
    description: {
      en: 'KBS Music Bank performance.',
      'zh-CN': 'KBS 音乐银行舞台。',
      ko: 'KBS 뮤직뱅크 무대.'
    },
    image: null,
    sourceName: 'KBS',
    sourceUrl: YT,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },
  {
    id: 'e016',
    artist: 'evan',
    date: '2026-09-12',
    endDate: null,
    time: '14:00',
    timezone: 'KST',
    title: { en: 'FANSIGN EVENT — SEOUL', 'zh-CN': '粉丝签售会 — 首尔', ko: '팬사인회 — 서울' },
    type: 'OFFLINE',
    status: STATUS.CONFIRMED,
    location: { en: 'YES24 Live Hall, Seoul', 'zh-CN': '首尔 YES24 Live Hall', ko: '서울 YES24 라이브홀' },
    description: {
      en: 'Offline fansign event for the debut album. Limited capacity.',
      'zh-CN': '出道专辑线下签售会，名额有限。',
      ko: '데뷔 앨범 오프라인 팬사인회. 인원 제한.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: null,
    mapUrl: 'https://maps.google.com'
  },
  {
    id: 'e017',
    artist: 'evan',
    date: '2026-09-19',
    endDate: null,
    time: '20:00',
    timezone: 'KST',
    title: { en: 'DEATH OF ME — SPECIAL LIVE', 'zh-CN': 'DEATH OF ME — 特别直播', ko: 'DEATH OF ME — 스페셜 라이브' },
    type: 'LIVE',
    status: STATUS.CONFIRMED,
    location: { en: 'Weverse Live', 'zh-CN': 'Weverse 直播', ko: 'Weverse 라이브' },
    description: {
      en: 'Special live broadcast celebrating the debut week. Unreleased behind content.',
      'zh-CN': '庆祝出道周的特别直播，包含未公开幕后内容。',
      ko: '데뷔 주간을 기념하는 스페셜 라이브 방송. 미공개 비하인드 콘텐츠.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: W,
    mapUrl: null
  },

  // ================= 2026 年 10 月 =================
  {
    id: 'e018',
    artist: 'evan',
    date: '2026-10-03',
    endDate: null,
    time: '09:00',
    timezone: 'KST',
    title: { en: 'MAGAZINE INTERVIEW — ARENA HOMME+', 'zh-CN': '杂志专访 — ARENA HOMME+', ko: '매거진 인터뷰 — ARENA HOMME+' },
    type: 'MAGAZINE',
    status: STATUS.CONFIRMED,
    location: { en: 'ARENA HOMME+', 'zh-CN': 'ARENA HOMME+', ko: 'ARENA HOMME+' },
    description: {
      en: 'In-depth interview about the debut album and creative process.',
      'zh-CN': '关于出道专辑与创作过程的深度专访。',
      ko: '데뷔 앨범과 창작 과정에 대한 심층 인터뷰.'
    },
    image: null,
    sourceName: 'ARENA HOMME+',
    sourceUrl: X,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },
  {
    id: 'e019',
    artist: 'evan',
    date: '2026-10-17',
    endDate: null,
    time: '16:00',
    timezone: 'KST',
    title: { en: 'BRAND EVENT — LUMEN POP-UP', 'zh-CN': '品牌活动 — LUMEN 快闪店', ko: '브랜드 이벤트 — LUMEN 팝업' },
    type: 'BRAND',
    status: STATUS.TBA,
    location: { en: 'Seongsu-dong, Seoul', 'zh-CN': '首尔圣水洞', ko: '서울 성수동' },
    description: {
      en: 'Pop-up store event with LUMEN Eyewear. Details to be announced.',
      'zh-CN': '与 LUMEN Eyewear 合作的快闪店活动，详情待公布。',
      ko: 'LUMEN Eyewear와 함께하는 팝업 스토어 이벤트. 세부 내용 추후 공개.'
    },
    image: null,
    sourceName: 'LUMEN Eyewear',
    sourceUrl: X,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },
  {
    id: 'e020',
    artist: 'evan',
    date: '2026-10-31',
    endDate: null,
    time: '21:00',
    timezone: 'KST',
    title: { en: 'HALLOWEEN SPECIAL LIVE', 'zh-CN': '万圣节特别直播', ko: '할로윈 스페셜 라이브' },
    type: 'LIVE',
    status: STATUS.RUMORED,
    location: { en: 'Weverse Live', 'zh-CN': 'Weverse 直播', ko: 'Weverse 라이브' },
    description: {
      en: 'Rumored Halloween special live. Not yet officially confirmed — please check official sources.',
      'zh-CN': '网传万圣节特别直播，尚未官方确认 — 请以官方渠道为准。',
      ko: '루머 중인 할로윈 스페셜 라이브. 아직 공식 확인 전 — 공식 채널을 확인해 주세요.'
    },
    image: null,
    sourceName: 'Fan Community',
    sourceUrl: null,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  },

  // ================= 2026 年 11 月 =================
  {
    id: 'e021',
    artist: 'evan',
    date: '2026-11-14',
    endDate: null,
    time: '00:00',
    timezone: 'KST',
    title: { en: 'EVAN BIRTHDAY', 'zh-CN': 'EVAN 生日', ko: 'EVAN 생일' },
    type: 'BIRTHDAY',
    status: STATUS.CONFIRMED,
    location: { en: '—', 'zh-CN': '—', ko: '—' },
    description: {
      en: 'EVAN\u2019s birthday. Official birthday greeting content expected.',
      'zh-CN': 'EVAN 的生日，预计会有官方生日问候内容。',
      ko: 'EVAN의 생일. 공식 생일 인사 콘텐츠 예정.'
    },
    image: null,
    sourceName: 'EVAN Weverse',
    sourceUrl: W,
    isOfficial: true,
    onlineUrl: W,
    mapUrl: null
  },
  {
    id: 'e022',
    artist: 'evan',
    date: '2026-11-28',
    endDate: '2026-11-29',
    time: '18:00',
    timezone: 'KST',
    title: { en: 'FAN CONCERT — FIRST STAGE', 'zh-CN': '粉丝演唱会 — FIRST STAGE', ko: '팬 콘서트 — FIRST STAGE' },
    type: 'OFFLINE',
    status: STATUS.TBA,
    location: { en: 'Olympic Hall, Seoul', 'zh-CN': '首尔奥林匹克大厅', ko: '서울 올림픽홀' },
    description: {
      en: 'First solo fan concert. Two-day run. Ticket details to be announced via official channels.',
      'zh-CN': '首场个人粉丝演唱会，为期两天。售票详情将通过官方渠道公布。',
      ko: '첫 솔로 팬 콘서트. 이틀간 진행. 티켓 상세는 공식 채널을 통해 공지 예정.'
    },
    image: null,
    sourceName: 'EVAN Official X',
    sourceUrl: X,
    isOfficial: true,
    onlineUrl: null,
    mapUrl: 'https://maps.google.com'
  },

  // ================= 2026 年 12 月 =================
  {
    id: 'e023',
    artist: 'evan',
    date: '2026-12-05',
    endDate: null,
    time: '19:30',
    timezone: 'KST',
    title: { en: 'YEAR-END STAGE — MAMA AWARDS', 'zh-CN': '年末舞台 — MAMA 颁奖礼', ko: '연말 무대 — MAMA AWARDS' },
    type: 'TV',
    status: STATUS.TBA,
    location: { en: 'Osaka, JP', 'zh-CN': '日本 大阪', ko: '오사카, 일본' },
    description: {
      en: 'Year-end award show performance. Lineup to be confirmed.',
      'zh-CN': '年末颁奖典礼舞台，出演阵容待确认。',
      ko: '연말 시상식 무대. 라인업 확정 예정.'
    },
    image: null,
    sourceName: 'MAMA',
    sourceUrl: YT,
    isOfficial: false,
    onlineUrl: null,
    mapUrl: null
  }
]

// ---- 导出排序后的活动（按日期）----
export const eventsSorted = [...events].sort((a, b) => (a.date < b.date ? -1 : 1))

// ---- 查询工具 ----
export function getEventById(id) {
  return events.find((e) => e.id === id)
}
