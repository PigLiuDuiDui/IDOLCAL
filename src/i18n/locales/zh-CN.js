                                          
export default {
  meta: {
    title: 'EVAN 官方日程',
    description:
      'EVAN 官方日程 — 官方活动档案站：回归、音乐节目、直播、线下活动、品牌活动、生日等日程索引'
  },
  nav: {
    label: '主导航',
    schedule: '日程',
    comeback: '回归',
    reminders: '提醒',
    timeline: '时间线',
    archive: '档案',
    about: '关于'
  },
  bottomNav: {
    label: '底部导航'
  },
  common: {
    all: '全部',
    language: '语言'
  },
  hero: {
    officialArchive: '官方档案 — {year}',
    currentEra: '当前时期',
    nextEvent: '下一活动',
    clickForDetails: '点击查看详情'
  },
  subscribe: {
    button: '订阅日历',
    eyebrow: '日历订阅',
    title: '订阅到手机日历',
    desc: '把 {artist} 的日程同步到您的手机日历，活动更新自动刷新，到点由手机系统提醒。',
    copy: '复制链接',
    copied: '已复制',
    copyFail: '复制失败，长按链接复制',
    download: '下载 .ics 文件',
    stepIos: 'iPhone：复制链接 → 设置 → 日历 → 添加订阅日历',
    stepAndroid: '安卓：复制链接 → 打开日历 App → 添加订阅（各机型入口略有不同）',
    localHint: '当前为本地预览模式，链接仅本机可用；部署上线后自动变为正式订阅链接。',
    deployHint: '把上面的订阅链接发给粉丝，添加一次即可永久自动同步。',
    close: '关闭'
  },
  calendar: {
    prevMonth: '上一月',
    nextMonth: '下一月',
    switchView: '切换视图',
    today: '今天',
    month: '月',
    list: '列表',
    more: '＋{n} 更多',
    empty: '所选类型暂无活动。'
  },
  home: {
    switchView: '切换视图',
    today: '今日',
    thisWeek: '本周',
    calendar: '日历',
    todayLabel: '今日',
    thisWeekLabel: '本周',
    todayTag: '今天',
    todayEmpty: '今天没有活动，享受平静 —— 或看看本周有什么。',
    weekEmpty: '未来 7 天暂无活动。',
    allDay: '全天'
  },
  reminder: {
    setBtn: '提醒我',
    setBtnDone: '已设置提醒',
    title: '设置提醒',
    eyebrow: '活动提醒',
    at: '于',
    confirm: '设置提醒',
    update: '更新提醒',
    cancel: '取消提醒',
    setAt: '已设置提醒：{at}',
    noTime: '该活动尚未确认开始时间。',
    options: {
      '1d': '1 天前',
      '3h': '3 小时前',
      '1h': '1 小时前',
      '30m': '30 分钟前',
      start: '活动开始时'
    }
  },
  reminders: {
    eyebrow: '我的提醒',
    sub: '集中查看所有已设置的活动提醒。提醒保存在浏览器本地，系统级通知将在 PWA 更新中上线。',
    alertAt: '提醒时间',
    cancel: '取消',
    emptyTitle: '还没有提醒',
    emptyDesc: '打开任意活动，点击「提醒我」即可在活动开始前收到提醒。',
    browse: '浏览日程',
    note: '提醒基于活动官方时区计算，并转换为你所在时区显示。'
  },
  timezone: {
    select: '选择时区',
    eyebrow: '时区',
    auto: '自动',
    autoHint: '跟随设备时区',
    official: '官方时间',
    yourTime: '你的时间',
    close: '关闭'
  },
  comeback: {
    eyebrow: '回归',
    releaseDate: '发行日期',
    timeline: '回归时间线',
    timelineNote: '完整日程',
    statuses: {
      COMPLETED: '已完成',
      UPCOMING: '即将到来',
      TODAY: '今天'
    },
    stages: {
      'concept-photo': '概念照',
      tracklist: '曲目列表',
      'highlight-medley': '试听片段',
      'mv-teaser': 'MV 预告',
      'album-release': '专辑发行',
      showcase: 'Showcase',
      'music-shows': '音乐节目'
    }
  },
  upcoming: {
    title: '近期日程',
    note: '未来 7 条日程',
    next: 'NEXT',
    empty: '所选类型暂无近期活动。'
  },
  timeline: {
    eyebrow: '日程档案',
    sub: '整个活动时期的纵向档案。按月份归档 {artist} 的官方日程 — {era} era.',
    eventCount: '共 {n} 个活动',
    empty: '所选类型暂无活动。',
    browseArchive: '浏览完整历史 Archive →'
  },
  archive: {
    eyebrow: '官方档案 — {year}',
    sub: '已发生的官方活动档案。所有条目保留原始官方来源记录，永不伪造、永不补写。',
    empty: '所选类型暂无历史活动。'
  },
  about: {
    eyebrow: '关于本档案站',
    sourcePolicy: '来源政策',
    note: '本档案站不制造信息，只归档信息。每条活动均标注官方来源与可信度等级，非官方内容不会被伪装成官方信息。',
    levels: {
      official: {
        name: '官方渠道',
        desc: '来自 Weverse / 官方 SNS / 官方 YouTube 等艺人官方渠道，可信度最高，以官宣为准。'
      },
      brand: {
        name: '品牌渠道',
        desc: '来自合作品牌的官方渠道，活动本身为官方合作，但信息以品牌方公布为准。'
      },
      media: {
        name: '媒体渠道',
        desc: '来自电视节目、杂志、媒体平台，内容经官方参与，但发布渠道为第三方。'
      },
      fan: {
        name: '粉丝项目',
        desc: '未经官方证实的消息一律标注为 RUMORED，仅作参考，不视为官方日程。'
      }
    },
    footer:
      '本页面为粉丝自建的官方活动档案站，与 {artist} 及其所属公司无隶属关系。所有日程请以官方渠道公布为准。',
    footNote: 'EVAN OFFICIAL SCHEDULE — ARCHIVE {year}'
  },
  drawer: {
    label: '活动详情',
    close: '关闭',
    date: '日期',
    time: '时间',
    location: '地点',
    countdown: '倒计时',
    officialSource: '官方来源',
    published: '发布于 {date}',
    viewOriginal: '查看原文 →',
    viewOfficialSource: '查看官方来源',
    viewSource: '查看来源',
    watchLive: '观看直播',
    joinOnline: '在线参与',
    viewLocation: '查看地点',
    navigate: '导航',
    addToCalendar: '加入我的日历'
  },
  types: {
    RELEASE: '回归',
    EVENT: '活动',
    TV: '综艺',
    LIVE: '直播',
    PHOTO: '写真',
    MAGAZINE: '杂志',
    OFFLINE: '线下',
    BRAND: '品牌',
    BIRTHDAY: '生日'
  },
  status: {
    CONFIRMED: '已确认',
    TBA: '待定',
    RUMORED: '传闻'
  },
  sourceLevels: {
    OFFICIAL: '已验证来源',
    BRAND: '品牌来源',
    MEDIA: '媒体来源',
    FAN: '粉丝项目'
  }
}
