---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 2c5f038cd7f38b6afa3b398cc7bedaea_83142b779c7311f19155525400826444
    ReservedCode1: sZALYL4FYho5+vbwTR83LnVVVnvVSHLwBrE0AHsnk2NA0L9IetCSzij1crD4DWUBdo6j6P89W5twbvgR2YL38d2TKht697nbCVCfzaxkMWrxmSmmlHd5RJudjm98C+pWXIWWSqhahASOZdv8iThSEnKbUAF5WBMdhG8qex2cKoc2JlzFpDMbA7tL5oA=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 2c5f038cd7f38b6afa3b398cc7bedaea_83142b779c7311f19155525400826444
    ReservedCode2: sZALYL4FYho5+vbwTR83LnVVVnvVSHLwBrE0AHsnk2NA0L9IetCSzij1crD4DWUBdo6j6P89W5twbvgR2YL38d2TKht697nbCVCfzaxkMWrxmSmmlHd5RJudjm98C+pWXIWWSqhahASOZdv8iThSEnKbUAF5WBMdhG8qex2cKoc2JlzFpDMbA7tL5oA=
---

# IdolCal API 文档

> 版本：1.0.0（对应后端 `api/` 模块，Spring Boot 4.1.0 / Java 21）
> 基础路径：`http://<host>:8080`（默认端口 8080，生产建议 Nginx 反代）

## 约定

- **认证方式**：管理接口使用 `Authorization: Bearer <JWT>` 头（JWT 由 `POST /api/admin/login` 签发，24 小时有效，可用 `IDOLCAL_JWT_TTL_HOURS` 调整）。
- **鉴权要求**：写接口（POST/PUT/DELETE）均标注 `@AdminOnly` 需 ADMIN 角色；`/api/admin/**` 命名空间由 `AuthInterceptor` 兜底保护（`/api/admin/login` 除外）。`IDOLCAL_AUTH_ENABLED=false` 可整体关闭鉴权（仅限本地调试，生产禁止）。
- **限流规则**（`IDOLCAL_RATE_LIMIT_ENABLED=true` 时生效，按 IP+路径）：
  - `POST /api/admin/login`：10 次/分钟
  - `GET /api/events|meta|artists|comebacks|tutorials`：60 次/分钟
  - `GET /api/*/{id}` 详情：120 次/分钟
  - 写操作（POST/PUT/PATCH/DELETE）：30 次/分钟
  - 其余 GET 兜底：60 次/分钟
  - 超限返回 `429`。
- **错误格式**：统一 `{ "error": "CODE", "message": "描述" }`（`ApiExceptionHandler` 处理），常见：`404 NOT_FOUND`、`400 BAD_REQUEST`、`401 UNAUTHORIZED`、`403 FORBIDDEN`、`409 CONFLICT`、`429 RATE_LIMITED`、`500 INTERNAL_ERROR`。
- **缓存**：`GET /api/events`、`GET /api/meta` 有 15s 短 TTL 缓存（`IDOLCAL_CACHE_ENABLED=true` 时），管理端写操作会立即失效。

---

## 1. 活动 Events

### GET /api/events — 活动列表
- **鉴权**：无
- **参数**（全部可选 Query）：`type`（活动类型）、`status`（状态）、`artist`（艺人）、`from` / `to`（ISO 时间范围）、`page` / `size`（内存分页；不传默认全量返回）
- **响应**：`200`，`EventDto[]`（含 id / title / type / status / artist / start / end / timezone / description / source / sourceUrl 等）

### GET /api/events/{id} — 活动详情
- **鉴权**：无
- **响应**：`200` `EventDto`；不存在返回 `404`

### POST /api/events — 新增活动
- **鉴权**：`@AdminOnly`（Bearer JWT）
- **请求体**：`EventDto`（id 缺省时自动生成 `e###`）
- **响应**：`201` `EventDto`

### PUT /api/events/{id} — 全量更新活动
- **鉴权**：`@AdminOnly`
- **请求体**：`EventDto`（全量字段）
- **响应**：`200` `EventDto`；不存在返回 `404`

### DELETE /api/events/{id} — 删除活动
- **鉴权**：`@AdminOnly`
- **响应**：`204 No Content`；不存在返回 `404`

---

## 2. 艺人 Artists

### GET /api/artists — 艺人列表
- **鉴权**：无
- **响应**：`200` `ArtistDto[]`（`current=true` 为当前展示艺人）

### GET /api/artists/{id} — 艺人详情
- **鉴权**：无
- **响应**：`200` `ArtistDto`；不存在 `404`

### POST /api/artists — 新增艺人
- **鉴权**：`@AdminOnly`
- **响应**：`201` `ArtistDto`

### PUT /api/artists/{id} — 更新艺人
- **鉴权**：`@AdminOnly`
- **响应**：`200` `ArtistDto`

### DELETE /api/artists/{id} — 删除艺人
- **鉴权**：`@AdminOnly`
- **响应**：`204`

---

## 3. 回归专题 Comebacks

### GET /api/comebacks — 回归列表
- **鉴权**：无
- **响应**：`200` `ComebackDto[]`（按发行日升序；stages 节点引用 events 表活动 id）

### GET /api/comebacks/{id} — 回归详情
- **鉴权**：无
- **响应**：`200` `ComebackDto`；不存在 `404`

### POST /api/comebacks — 新增回归
- **鉴权**：`@AdminOnly`
- **响应**：`201` `ComebackDto`

### PUT /api/comebacks/{id} — 更新回归
- **鉴权**：`@AdminOnly`
- **响应**：`200` `ComebackDto`

### DELETE /api/comebacks/{id} — 删除回归
- **鉴权**：`@AdminOnly`
- **响应**：`204`

---

## 4. 教程 Tutorials

### GET /api/tutorials — 教程列表
- **鉴权**：无
- **响应**：`200` `TutorialDto[]`（含 ready 与 coming 板块）

### GET /api/tutorials/{id} — 教程详情
- **鉴权**：无
- **响应**：`200` `TutorialDto`；不存在 `404`

### POST /api/tutorials — 新增教程
- **鉴权**：`@AdminOnly`
- **响应**：`201` `TutorialDto`

### PUT /api/tutorials/{id} — 更新教程
- **鉴权**：`@AdminOnly`
- **响应**：`200` `TutorialDto`

### DELETE /api/tutorials/{id} — 删除教程
- **鉴权**：`@AdminOnly`
- **响应**：`204`

---

## 5. 元数据 Meta

### GET /api/meta — 元数据全量
- **鉴权**：无
- **响应**：`200` `{ eventTypes, statuses, sourceLevels, comebackStages }`（有 15s 缓存）

### PUT /api/meta/{key} — 更新单个元数据 key
- **鉴权**：`@AdminOnly`
- **路径**：`key` ∈ { eventTypes, statuses, sourceLevels, comebackStages }
- **请求体**：任意 JSON 值
- **响应**：`200` 更新后的 JSON 值；触发 `eventTypes` 类型标签缓存重载（`WebPushService.invalidateTypeLabels()`）

---

## 6. 推送 Web Push（匿名设备）

> 设备所有权：`subscribe` 成功后返回 HMAC 凭证（`credential`），前端保存；
> 其余写请求必须携带 `X-Device-Token: <credential>` 头，防止越权操作他人设备。

### GET /api/push/vapid-public-key — VAPID 公钥
- **鉴权**：无
- **响应**：`200` `{ "key": "<base64url 公钥>" }`

### POST /api/push/subscribe — 保存订阅（并注册设备）
- **鉴权**：无（凭证由本接口签发）
- **请求体**：`PushSubscribeRequest { deviceId, endpoint, p256dh, auth }`
- **响应**：`200` `{ "ok": true, "credential": "<HMAC 凭证>" }`
- **错误**：endpoint 已属他人返回 `409 CONFLICT`

### DELETE /api/push/subscribe — 退订
- **鉴权**：需 `X-Device-Token`
- **请求体**：`PushUnsubscribeRequest { deviceId, endpoint }`
- **响应**：`200` `{ "ok": true }`

### PUT /api/push/reminders — 全量同步设备提醒任务
- **鉴权**：需 `X-Device-Token`
- **请求体**：`PushRemindersRequest { deviceId, reminders: [{ eventId, offsetMinutes }] }`
- **响应**：`200` `{ "ok": true }`

### POST /api/push/send-test — 发送测试通知
- **鉴权**：需 `X-Device-Token`
- **请求体**：`PushSendTestRequest { deviceId }`
- **响应**：`200` `{ "ok": true }`；发送失败返回 `400 BAD_REQUEST`

---

## 7. 管理后台 Admin

### POST /api/admin/login — 管理员登录
- **鉴权**：无（限流 10 次/分钟/IP；连续失败锁定，`IDOLCAL_AUTH_LOGIN_MAX_FAILURES` 次后锁 `IDOLCAL_AUTH_LOGIN_LOCK_MINUTES` 分钟）
- **请求体**：`AdminLoginRequest { username, password }`
- **响应**：`200` `{ "token": "<JWT>", "role": "ADMIN" }`

### GET /api/admin/push/stats — 推送投递统计
- **鉴权**：`@AdminOnly`
- **响应**：`200` `{ "today": { total, success, failed, expired } }`

### GET /api/admin/push/tasks/stats — 调度+任务状态分布
- **鉴权**：`@AdminOnly`
- **响应**：`200` `{ "schedules": { total, byStatus }, "tasks": { total, byStatus } }`
- **状态**：PENDING / PROCESSING / SUCCESS / FAILED / RETRY

### GET /api/admin/push/upcoming — 未来 24h 即将触发的调度
- **鉴权**：`@AdminOnly`
- **响应**：`200` `[{ scheduleId, eventId, offsetMinutes, triggerAt, recipients }]`

---

## 附录 A：静态资源

- `GET /`、`/index.html`、`/assets/*`：前端 SPA 静态资源（由构建产物部署，Nginx 或后端 static 目录提供）
- `GET /icon-192.png`：PWA 图标
- `GET /calendar.ics`：日历导出文件（构建时 `scripts/gen-ics.js` 生成）

## 附录 B：环境变量速查

详见根目录 `.env.example`（完整模板）。关键项：

| 变量 | 必填 | 说明 |
|---|---|---|
| `IDOLCAL_JWT_SECRET` | 生产必填 | JWT 密钥，≥32 字节 |
| `IDOLCAL_ADMIN_PASSWORD` | 生产必填 | 管理员初始密码 |
| `IDOLCAL_DB_URL` / `IDOLCAL_DB_USERNAME` / `IDOLCAL_DB_PASSWORD` | 生产必填 | PostgreSQL 连接 |
| `IDOLCAL_VAPID_PUBLIC_KEY` / `IDOLCAL_VAPID_PRIVATE_KEY` | 推送必填 | Web Push 签名密钥（缺失时回退 `api/.vapid-keys`） |
| `IDOLCAL_REDIS_HOST` / `IDOLCAL_REDIS_PORT` / `IDOLCAL_REDIS_PASSWORD` | 选填 | Redis（缺失自动降级内存实现） |
*（内容由AI生成，仅供参考）*
