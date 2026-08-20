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

一个面向粉丝的「官方活动档案站」——记录 EVAN 的回归发歌、音乐节目、直播、线下活动、品牌活动、生日等官方日程，视觉走高级杂志 / 现代 Web Archive 路线（非饭圈应援风）。支持按用户时区自动换算时间、为活动设置提醒、回归专题倒计时等粉丝高频诉求。

## 技术栈

- Vue 3（Composition API）+ Vite 5
- Pinia（全局状态：UI / 数据 / 提醒 / 时区）
- Vue Router 4（hash 模式）
- FullCalendar 7（@fullcalendar/vue3，仅 Standard 功能，视觉完全重设计）
- Temporal API（temporal-polyfill）：时区转换核心，正确处理跨天与夏令时
- vue-i18n：多语言界面（简体中文 / English / 한국어）
- Spring Boot 后端（`api/`，可选）：活动 / 回归 / 教程数据 API + Web Push 订阅 / 调度管理（Quartz 定时任务 + CAS 状态机防重复发送）；前端离线时自动回退本地数据

纯 CSS 实现，无额外 UI 库。色彩体系：80% 中性色（Off-white / Deep Black / Soft Gray）+ 15% 灰阶 + 5% 克制强调色。

## 功能

- **Today / This Week 快捷视图**：首页顶部 Today / Week / Calendar 三种视图切换，打开即见当天与未来 7 天的重要行程。Today 按时间排序，This Week 按日期分组（含 TODAY 标记），支持艺人 / 类型筛选与快速设置提醒，无活动时显示 Empty State
- **活动日历**：FullCalendar 月历 + 列表视图，Editorial 风格重设计；桌面端月历、移动端自动切换列表；日期格子内最多 2~3 条事件（＋N more），今日克制高亮
- **Upcoming 区域**：未来 7 个活动的纵向时间线，最近一个活动带 NEXT / D-XX 视觉强调
- **Comeback Hub / 回归专题**：仪式感回归页——艺人 + 回归名称 + D-DAY 倒计时 + Release Date（官方时间与本地时间对照）；COMEBACK TIMELINE 纵向时间线（Concept Photo → Tracklist → Highlight Medley → MV Teaser → Album Release → Showcase → Music Shows），节点状态分 COMPLETED / TODAY / UPCOMING，已完成的弱化保留、当前节点突出；节点直接关联日历 Event，点击跳转活动详情；回归结束后 Timeline 完整保留作为 Archive；支持多回归切换（预留）
- **活动提醒**：活动详情页「提醒我」按钮 → 弹出提醒设置（1 天前 / 3 小时前 / 1 小时前 / 30 分钟前 / 活动开始时 / 自定义 5 分钟~30 天）；设置后按钮变为「已设置提醒」，可随时修改或取消；仅对有明确开始时间的未开始活动开放提醒；活动卡片显示铃铛图标标记；首页 NEXT EVENT 卡片一键直达提醒面板；Reminders 页集中管理全部提醒（按 Today / Tomorrow / Upcoming 分组，批量设置 / 清理过期），并可开启 **Web Push 推送通知**（PWA Service Worker，iOS 引导添加到主屏幕后使用）；提醒触发时间基于活动实际时区计算；数据结构为未来扩展预留 channel / notified / subscriptionId 字段
- **自动时区转换**：数据层永远保存官方原始时区与时间，前端基于 Temporal 自动检测浏览器时区，也可在 TimezoneSwitcher 手动选择常用粉丝时区（自动 / KST / JST / CST …）；显示默认突出用户当地时间、保留官方时间作参考（如 `20:00 JST` / `19:00 KST · Official Time`），跨天活动显示 ±1 日标记；日历、Upcoming、Timeline、Today / This Week、活动详情、提醒计算全部走同一套转换逻辑，保证时间一致
- **多类型筛选**：ALL / RELEASE / EVENT / TV / LIVE / PHOTO / MAGAZINE / OFFLINE / BRAND / BIRTHDAY，Pill 多选，日历与列表联动过滤
- **活动详情面板**：桌面右侧滑出 / 移动端 Bottom Sheet；含日期、时间（本地 + 官方双显示）、时区、地点、状态、描述、官方来源（区分 Official / Brand / Media / Fan Project 可信度）；底部操作按钮按类型动态出现（设置提醒 / 查看官方来源 / 观看直播 / 查看地点 / 导航 / 加入我的日历 [导出 .ics]）
- **Timeline**：月度分组的 Archive 感纵向时间线，快速浏览整个时期
- **Archive**：已发生活动的档案索引（按年份 / 月份归档，保留来源记录）
- **Tutorial**：板块式入门教程（使用指南，部分板块预告中）
- **About**：来源可信度政策说明与免责声明
- **多语言**：简体中文 / English / 한국어 一键切换
- **数据双源**：启动优先拉取后端 API（`/api/{events,artists,comebacks,tutorials,meta}`），后端不可用时自动回退到 `src/data/` 本地快照，纯静态部署也可完整使用
- **数据独立**：活动数据集中在数据层（API + `src/data/`），更换艺人只需替换数据文件；预留 `artist` 字段支持多艺人

## 快速开始

前端：

```bash
npm install
npm run dev
```

后端（可选，提供数据 API 与 Web Push 推送）：

```bash
cd api
./mvnw spring-boot:run   # 默认 8080；数据库默认 H2 文件库，首次启动 Flyway 自动建表/迁移
```

数据库说明与 PostgreSQL 生产切换见「数据库与迁移（Flyway）」一节。

打开浏览器访问 http://localhost:5173（Vite dev 已代理 `/api` → 8080）

## 构建

```bash
npm run build    # 构建前自动生成 calendar.ics
npm run preview
```

## 项目结构

```
IdolCal/
├── index.html
├── package.json
├── vite.config.js
├── public/
│   ├── sw.js                 # Service Worker（PWA / Web Push 推送）
│   └── calendar.ics          # 构建时自动生成的可订阅日历
├── scripts/
│   ├── gen-ics.js            # 生成 .ics 订阅文件
│   ├── gen-vapid.mjs         # 生成 Web Push VAPID 密钥
│   └── export-seed.mjs       # 导出种子数据
├── api/                      # Spring Boot 后端（数据 API + 推送管理）
│   └── src/main/java/com/example/bim/api/
│       ├── entity/           # Artist / Comeback / Event / Tutorial / Push…
│       ├── repository/       # Spring Data JPA
│       ├── service/          # 业务逻辑（推送调度 PushScheduler 等）
│       ├── web/              # REST 控制器
│       ├── db/               # Flyway Java 迁移（存量库幂等改名/补列）
│       └── config/           # CORS / 限流 / 推送定时任务 / 种子数据
└── src/           
    ├── main.js               # 应用入口（Pinia + Router + i18n）
    ├── App.vue               # 根组件（顶部导航 + 底部导航 + 全局详情面板）
    ├── style.css             # 全局视觉体系（中性色 token / 通用组件样式）
    ├── router/index.js       # 路由：Schedule / Comeback / Reminders / Timeline / Archive / About
    ├── stores/
    │   ├── ui.js             # 全局 UI 状态（详情面板 + 类型筛选）
    │   ├── data.js           # 数据 Store（API 唯一数据源，本地快照兜底）
    │   ├── reminders.js      # 提醒状态（本地持久化 + Web Push 开关 / 同步）
    │   └── timezone.js       # 显示时区（'auto' 跟随浏览器或手动 IANA）
    ├── i18n/
    │   ├── locales/          # zh-CN.js / en.js / ko.js
    │   └── index.js
    ├── data/
    │   ├── artists.js        # 艺人档案配置（EVAN，预留多艺人）
    │   ├── events.js         # 活动数据（独立数据结构 + 类型 / 状态 / 来源定义）
    │   ├── comebacks.js      # 回归专题（阶段时间线，关联 Event）
    │   └── tutorials.js      # 教程板块内容
    ├── utils/
    │   ├── date.js           # 日期工具（D-Day 倒计时 / 杂志式格式化 / 月历网格）
    │   ├── time.js           # 时区转换核心（Temporal）+ 提醒偏移计算
    │   ├── push.js           # Web Push 订阅 / 提醒同步 / 设备标识
    │   └── ics.js            # 导出 .ics
    ├── views/
    │   ├── ScheduleView.vue  # 主页：Today / Week / Calendar 切换 → Hero → 筛选 → 日历 / Upcoming
    │   ├── ComebackView.vue  # 回归专题：D-DAY 倒计时 + 阶段时间线
    │   ├── RemindersView.vue # 提醒中心：提醒列表 + Web Push 开关
    │   ├── TimelineView.vue  # 月度分组 Archive 时间线
    │   ├── ArchiveView.vue   # 历史活动归档
    │   ├── TutorialView.vue  # 使用教程
    │   └── AboutView.vue     # 关于 / 来源政策
    └── components/
        ├── SiteHeader.vue        # 顶部导航（桌面）
        ├── BottomNav.vue         # 底部导航（移动端）
        ├── HeroSection.vue       # Hero：艺人身份 + ERA + NEXT 倒计时 + 设置提醒入口
        ├── FilterBar.vue         # 多选 Pill 类型筛选
        ├── CalendarPanel.vue     # FullCalendar 封装（Editorial 风格 + Month/List）
        ├── UpcomingList.vue      # 未来活动纵向时间线
        ├── DayScheduleCard.vue   # Today / This Week 日程卡片
        ├── EventCard.vue         # 小型 Editorial 事件卡片（含提醒铃铛标记）
        ├── EventTime.vue         # 统一时间显示（本地 + 官方双时区）
        ├── EventDetailDrawer.vue # 活动详情（右侧 Drawer / Bottom Sheet + 提醒入口）
        ├── ReminderPanel.vue     # 提醒设置弹窗
        ├── SubscribePanel.vue    # .ics 订阅引导
        ├── TimezoneSwitcher.vue  # 时区选择器
        └── LocaleSwitcher.vue    # 语言切换
```

## 数据说明

活动数据结构（`src/data/events.js`，与后端 Event 实体一致）：

```
{ id, artist, date, endDate, time, timezone, originalTimezone,
  startDateTime, endDateTime, title, type, status, location,
  description, image, sourceName, sourceUrl, isOfficial,
  onlineUrl, mapUrl }
```

- `timezone` 保存官方原始时区（如 `KST`），`originalTimezone` 记录来源时区；**转换后的时间绝不写回数据**，展示层统一经 `utils/time.js` 基于 Temporal 换算
- 前端数据源为后端 API，本地 `src/data/*.js` 是离线兜底快照；页面组件只负责渲染
- 未来更换 / 新增艺人（EVAN / HEESEUNG / ENHYPEN …）只需替换数据文件

## 数据库与迁移（Flyway）

- 默认开发库为 H2 文件库（`api/data/idolcal.mv.db`，重启不丢）；生产切换 PostgreSQL 只需设置环境变量：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `IDOLCAL_DB_URL` | `jdbc:h2:file:./data/idolcal;AUTO_SERVER=TRUE` | JDBC 连接串；生产设为 `jdbc:postgresql://host:5432/idolcal` |
| `IDOLCAL_DB_DRIVER` | `org.h2.Driver` | 生产设为 `org.postgresql.Driver` |
| `IDOLCAL_DB_USERNAME` | `sa` | 数据库用户名 |
| `IDOLCAL_DB_PASSWORD` | — | 数据库密码（生产必填） |

- **Flyway 是 schema 唯一事实来源**（`ddl-auto: none`，Hibernate 不再自动建表/改表）：改表必须新增版本化迁移脚本，禁止手改表结构
- 迁移脚本位置：`api/src/main/resources/db/migration/`（SQL，如 `V1__init.sql`）+ `com.example.bim.api.db`（Java，存量库幂等改名/补列，如 `current` → `is_current`、`events.start_at_utc` 补列）
- 存量库平滑升级：`baseline-on-migrate` + `baseline-version: 0`，V1 全部 `IF NOT EXISTS` —— 旧库自动跳过已有对象、补建缺失表（如旧版没有的 `push_*` 表）；新版本必须向后兼容，只允许幂等补列/改名
- 同一套脚本同时跑 H2（开发/测试）与 PostgreSQL（生产，IDENTITY 主键 / TEXT 列 / 唯一索引均双端兼容）；`PgMigrationTest` 用 Testcontainers 在真实 PostgreSQL 容器验证迁移与种子导入（需 Docker，无 Docker 自动跳过）

## 2026-08 推送可靠性优化记录

针对万人级单爱豆推送日历的优化实施记录（详见 `IdolCal_README_优化方案.md`）：

### 🔴 P0 已修复

- **Retry → PROCESSING 状态流转 bug**：`claim` 原来只允许 PENDING→PROCESSING，导致重试扫描到的 RETRY 任务永远无法被认领；改为支持 `PENDING | RETRY → PROCESSING` 的 CAS 流转
- **`unscheduleAll(deviceId)` 误删全部 Quartz Job**：已随 Fan-out 改造彻底移除（方法本身按设备删 Job 的思路就被调度模型取代）；现在孤儿调度清理只在调度下无任何未完成任务时精确删除对应 Job
- **PROCESSING 超时恢复防重复发送**：新增 `attemptId` 租约机制（每次认领递增，结果落库校验，旧 attempt 结果丢弃）；超时恢复后不立即重跑，延迟到下一轮扫描补跑；超时阈值可配置
- **JWT Secret 生产强制**：strict 模式——未配置或不足 32 字节时拒绝启动，禁止默认密钥上生产
- **管理员登录安全**：登录接口专用限流（10 次/分钟）+ 连续失败锁定（默认 5 次 / 15 分钟，可配置）
- 复查确认：测试管理员密码无残留；所有写接口已统一鉴权（无需改动）

### 🟠 第一阶段已实施

- **Schedule + Fan-out 推送模型（万人级核心改造）**：
  - 数据模型从「用户 → PushTask → Quartz」改为「Event → PushSchedule → 1 个 Quartz Job → Fan-out → PushSubscription」；一个活动提醒时间点只有 1 个 Job，8000 用户 = 1 Job 而非 8000 Job
  - 调度级状态机（CAS）：`PENDING | RETRY → PROCESSING`（认领时 `attemptId` 递增，收尾校验防旧尝试倒灌）→ 任务全完成 `SUCCESS` / 有待重试 `RETRY`（调度级退避 1/5/30 分钟）
  - Fan-out 执行：一次查询调度下全部可投递任务（`findDueBySchedule`）→ 内存分批（`fanout-batch-size` 默认 500/批）→ 每批 `sendBatch`（批量查订阅 `findByDeviceIdIn` 避免 N+1 + 有界线程池 8 并发 + 单条 30s 超时）→ 任务状态与投递日志统一 `saveAll`（替代每条一次 DB 写）
  - 任务级状态机保留：SUCCESS / RETRY（最多 3 次）/ FAILED；订阅失效（404/410）即时删除
  - 调度幂等 upsert（`(event_id, offset_minutes)` 唯一约束）+ 孤儿调度清理（无未完成任务时删 Job）；设备取消提醒不影响其他用户
  - **重复任务兜底**：`UNIQUE(schedule_id, device_id)` 唯一约束（实体声明 + 启动时幂等建索引，`ddl-auto=update` 不会改存量表），同一设备在同一调度下只能有一个任务
  - **发送超时治理**：Future 超时后 `cancel(true)` 中断线程；底层 HTTP 客户端自定义（连接 / 读取超时 = 30s + 连接池上限 = 并发数，web-push 默认客户端无超时可能无限卡死）；取消后补失败日志行避免下游 NPE
  - **任务结果 attempt 校验**：每批发送前 / 后校验调度 attemptId 与状态（PROCESSING），旧 Worker 迟到结果不写入任务状态，防止覆盖新尝试
  - 兜底扫描改为调度级：Quartz 丢失 / 重启补跑（`recoverPending`）、重试补跑（`retryDue`）、PROCESSING 超时重置（`recoverStale`，仅重置不立即重跑）
- **Web Push 有限并发批量发送**：有界线程池（默认 8 并发），单条发送 30s 超时，应用关闭时优雅停机
- **推送表索引优化**：`push_subscriptions(device_id)`、`push_tasks(status, next_retry_at)`、`push_schedules(status, trigger_at / next_retry_at / processed_at)`、`push_delivery_log(result, sent_at)`
- **DeliveryLog 增长控制**：每日凌晨自动清理 90 天前的投递日志（保留天数可配置）
- **后台监控接口**：`GET /api/admin/push/tasks/stats`（调度 + 任务双状态分布）、`GET /api/admin/push/upcoming`（24h 内即将触发的调度，每条附 Fan-out 收件人数）
- **首页 NEXT EVENT 强化**：卡片下方新增「设置提醒」按钮，直达提醒面板（有明确开始时间的活动显示）
- **Reminders 页分组**：按提醒触发日期分组 Today / Tomorrow / Upcoming，三语同步

### 🟡 后续演进方向（暂未实施）

Redis 限流 / 统计缓存（多实例时）、Quartz Cluster（多实例调度时）、监控告警与 P95/P99 耗时统计。

### 新增环境变量 / 配置

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `IDOLCAL_JWT_SECRET` | — | **生产必填**，≥32 字节高强度随机密钥，缺失或过短拒绝启动 |
| `idolcal.push.send-concurrency` | `8` | Web Push 并发发送线程数 |
| `idolcal.push.fanout-batch-size` | `500` | Fan-out 分批大小（8000 用户 = 16 批） |
| `idolcal.push.processing-timeout-ms` | `600000` | PROCESSING 超时恢复阈值（10 分钟） |
| `idolcal.push.delivery-log-retention-days` | `90` | 投递日志保留天数 |
| `idolcal.auth.login-max-failures` | `5` | 连续登录失败锁定阈值 |
| `idolcal.auth.login-lock-minutes` | `15` | 锁定时长（分钟） |

*（内容由AI生成，仅供参考）*
