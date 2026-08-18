---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 2c5f038cd7f38b6afa3b398cc7bedaea_4461c6a29acc11f19bec525400826444
    ReservedCode1: fVSQJtSTcIcfwcHMYWW5EUu3v9QDdCcpHPiLS5ceu262HdRNOOdfv4LacSMmfT0s0eUmeX2pE9TcMfQzf5k4D+3LrFx0Jyj7ZEQNsCgtKIp99D25d6QLK0M8hwWZPS/OO5WK9/6UWRZmkgXwW29TffIIFio4sfpoU85u3Xh5/O0ScWogaoFEKohZbOM=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 2c5f038cd7f38b6afa3b398cc7bedaea_4461c6a29acc11f19bec525400826444
    ReservedCode2: fVSQJtSTcIcfwcHMYWW5EUu3v9QDdCcpHPiLS5ceu262HdRNOOdfv4LacSMmfT0s0eUmeX2pE9TcMfQzf5k4D+3LrFx0Jyj7ZEQNsCgtKIp99D25d6QLK0M8hwWZPS/OO5WK9/6UWRZmkgXwW29TffIIFio4sfpoU85u3Xh5/O0ScWogaoFEKohZbOM=
---

# EVAN Official Schedule 💎

一个面向粉丝的「官方活动档案站」——记录 EVAN 的回归发歌、音乐节目、直播、线下活动、品牌活动、生日等官方日程，视觉走高级杂志 / 现代 Web Archive 路线（非饭圈应援风）。

## 技术栈

- Vue 3（Composition API）+ Vite 5
- Pinia（全局 UI 状态）
- Vue Router 4（hash 模式）
- FullCalendar 7（@fullcalendar/vue3，仅 Standard 功能，视觉完全重设计）

纯 CSS 实现，无额外 UI 库。色彩体系：80% 中性色（Off-white / Deep Black / Soft Gray）+ 15% 灰阶 + 5% 克制强调色。

## 功能

- **Hero 档案头部**：艺人身份 / CURRENT ERA / 下一活动倒计时（D-XX），深色杂志式排版
- **活动日历**：FullCalendar 月历 + 列表视图，Editorial 风格重设计；桌面端月历、移动端自动切换列表；日期格子内最多 2~3 条事件（＋N more），今日克制高亮
- **Upcoming 区域**：未来 7 个活动的纵向时间线，最近一个活动带 NEXT / D-XX 视觉强调
- **多类型筛选**：ALL / RELEASE / EVENT / TV / LIVE / PHOTO / MAGAZINE / OFFLINE / BRAND / BIRTHDAY，Pill 多选，日历与列表联动过滤
- **活动详情面板**：桌面右侧滑出 / 移动端 Bottom Sheet；含日期、时间、时区、地点、状态、描述、官方来源（区分 Official / Brand / Media / Fan Project 可信度）；底部操作按钮按类型动态出现（查看官方来源 / 观看直播 / 查看地点 / 导航 / 加入我的日历 [导出 .ics]）
- **Timeline**：月度分组的 Archive 感纵向时间线，快速浏览整个时期
- **Archive**：已发生活动的档案索引（按年份 / 月份归档，保留来源记录）
- **About**：来源可信度政策说明与免责声明
- **数据独立**：活动数据集中在 `src/data/`，更换艺人只需替换数据文件；预留 `artist` 字段支持多艺人

## 快速开始

```bash
npm install
npm run dev
```

打开浏览器访问 http://localhost:5173

## 构建

```bash
npm run build
npm run preview
```

## 项目结构

```
IdolCal/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.js                  # 应用入口（Pinia + Router）
    ├── App.vue                  # 根组件（顶部导航 + 底部导航 + 全局详情面板）
    ├── style.css                # 全局视觉体系（中性色 token / 通用组件样式）
    ├── router/index.js          # 路由：Schedule / Timeline / Archive / About
    ├── stores/ui.js             # 全局 UI 状态（详情面板 + 类型筛选）
    ├── data/
    │   ├── artists.js           # 艺人档案配置（EVAN，预留多艺人）
    │   └── events.js            # 活动数据（独立数据结构 + 类型 / 状态 / 来源定义）
    ├── utils/date.js            # 日期工具（D-Day 倒计时 / 杂志式格式化 / 月历网格）
    ├── views/
    │   ├── ScheduleView.vue     # 主页：Hero → 筛选 → 日历 / Upcoming（PC 双栏）
    │   ├── TimelineView.vue     # 月度分组 Archive 时间线
    │   ├── ArchiveView.vue      # 历史活动归档
    │   └── AboutView.vue        # 关于 / 来源政策
    └── components/
        ├── SiteHeader.vue           # 顶部导航（桌面）
        ├── BottomNav.vue            # 底部导航（移动端：Schedule / Timeline / Archive / About）
        ├── HeroSection.vue          # Hero：艺人身份 + ERA + NEXT 倒计时
        ├── FilterBar.vue            # 多选 Pill 类型筛选
        ├── CalendarPanel.vue        # FullCalendar 封装（Editorial 风格 + Month/List）
        ├── UpcomingList.vue         # 未来活动纵向时间线
        ├── EventCard.vue            # 小型 Editorial 事件卡片
        └── EventDetailDrawer.vue    # 活动详情（右侧 Drawer / Bottom Sheet）
```

## 数据说明

活动数据结构（`src/data/events.js`）：

```
{ id, artist, date, endDate, time, timezone, title, type,
  status, location, description, image, sourceName, sourceUrl,
  isOfficial, onlineUrl, mapUrl }
```

页面组件只负责渲染；未来更换 / 新增艺人（EVAN / HEESEUNG / ENHYPEN …）只需替换数据文件。

*（内容由AI生成，仅供参考）*
