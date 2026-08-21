package com.example.bim.api.service;

import com.example.bim.api.config.VapidKeys;
import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.Meta;
import com.example.bim.api.entity.PushDeliveryLog;
import com.example.bim.api.entity.PushSubscription;
import com.example.bim.api.entity.PushTask;
import com.example.bim.api.entity.UserDevice;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.MetaRepository;
import com.example.bim.api.repository.PushDeliveryLogRepository;
import com.example.bim.api.repository.PushSubscriptionRepository;
import com.example.bim.api.repository.UserDeviceRepository;
import com.example.bim.api.Exception.ConflictException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Web Push 核心：订阅管理 / 设备注册 / 发送（含投递日志）。
 * - 发送用 nl.martijndwars.webpush（VAPID 签名 + RFC 8292 payload 加密）
 * - 每次发送记录 push_delivery_logs（后台统计 / 排查），日志统一批量 saveAll
 * - 通知点击直达活动页 /#/event/{id}
 * - Fan-out 批次内所有订阅用有界线程池并发发送（idolcal.push.send-concurrency），
 *   避免万人级场景下完全串行发送造成吞吐瓶颈；拒绝策略为调用方执行，任务不丢失。
 * - 发送用自定义 Apache HttpClient（连接 / 读取超时 = SEND_TIMEOUT_MS + 连接池上限 = 并发数），
 *   web-push 默认客户端无超时，慢 endpoint 可能无限卡住线程；
 *   Future 超时后 cancel(true) 中断线程，配合 socket 超时保证线程最终必然结束。
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 通知图标（public 静态资源，PWA 同域部署） */
    private static final String ICON = "/icon-192.png";
    private static final String BADGE = "/icon-192.png";
    /** 单条投递超时（超过视为失败，防止个别慢 endpoint 拖垮整批） */
    private static final long SEND_TIMEOUT_MS = 30_000L;

    // web-push 用 KeyFactory.getInstance("EC", "BC")，需先注册 BouncyCastle provider
    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    /** 单次投递结果 */
    public enum SendResult { SUCCESS, EXPIRED, FAILED }

    /** 任务级发送结果汇总（PushTaskService 状态机用） */
    public record SendOutcome(boolean anySuccess, boolean allExpired, String message) {
    }

    /** 单条投递结果 + 待落库日志行（调用方批量 saveAll，避免逐条写库） */
    public record SendItem(SendResult result, PushDeliveryLog logRow) {
    }

    /** 批次内单个任务的结果（Fan-out 批次处理后批量更新任务状态） */
    public record TaskSendResult(long taskId, SendOutcome outcome) {
    }

    /** 发送单元：一个任务 + 一个订阅（整批提交线程池，任务级并行） */
    private record DispatchUnit(Long taskId, PushSubscription sub) {
    }

    private final PushSubscriptionRepository subscriptions;
    private final PushDeliveryLogRepository deliveryLogs;
    private final UserDeviceRepository devices;
    private final EventRepository events;
    private final MetaRepository meta;
    private final VapidKeys vapidKeys;
    private final DeviceAuthService deviceAuth;
    /** 事件类型标签缓存（启动加载，meta 写操作失效；避免 Fan-out 每批发送重复查库） */
    private volatile Map<String, String> typeLabelCache = Map.of();
    /** 仅用于生成已签名加密的 HttpPost（preparePost），实际发送走自定义超时客户端 */
    private final nl.martijndwars.webpush.PushService pushService;
    /** 自定义 HTTP 客户端：连接 / 读取超时 + 连接池上限（web-push 默认客户端无超时） */
    private final CloseableHttpClient httpClient;
    /** 有界发送线程池：控制并发数，避免为每个订阅开线程 */
    private final ExecutorService senderPool;

    public WebPushService(PushSubscriptionRepository subscriptions,
                          PushDeliveryLogRepository deliveryLogs,
                          UserDeviceRepository devices,
                          EventRepository events, MetaRepository meta,
                          VapidKeys vapidKeys, DeviceAuthService deviceAuth,
                          @Value("${idolcal.push.send-concurrency:8}") int sendConcurrency) throws GeneralSecurityException {
        this.subscriptions = subscriptions;
        this.deliveryLogs = deliveryLogs;
        this.devices = devices;
        this.events = events;
        this.meta = meta;
        this.vapidKeys = vapidKeys;
        this.deviceAuth = deviceAuth;
        this.pushService = new nl.martijndwars.webpush.PushService(
                vapidKeys.publicKey(), vapidKeys.privateKey(), VapidKeys.SUBJECT);
        int n = Math.max(1, sendConcurrency);
        // 自定义 HTTP 客户端：连接 / 读取超时与单条发送超时一致，连接池上限 = 并发数
        // （web-push 默认客户端无任何超时，慢 endpoint 会无限卡住发送线程）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int) SEND_TIMEOUT_MS)
                .setSocketTimeout((int) SEND_TIMEOUT_MS)
                .setConnectionRequestTimeout((int) SEND_TIMEOUT_MS)
                .build();
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(n)
                .setMaxConnTotal(n)
                .build();
        this.senderPool = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "webpush-sender");
            t.setDaemon(true);
            return t;
        });
        log.info("[push] 发送线程池已初始化：并发 {}（单条超时 {}s）", n, SEND_TIMEOUT_MS / 1000);
    }

    @PreDestroy
    public void shutdown() {
        senderPool.shutdown();
        try {
            httpClient.close();
        } catch (Exception e) {
            log.warn("[push] HTTP 客户端关闭异常：{}", e.getMessage());
        }
    }

    /** VAPID 公钥（前端订阅时 applicationServerKey 用） */
    public String vapidPublicKey() {
        return vapidKeys.publicKey();
    }

    @PostConstruct
    void loadTypeLabels() {
        this.typeLabelCache = typeLabelsFromDb();
        log.info("[push] 活动类型标签已加载（{} 条）", typeLabelCache.size());
    }

    /** 管理端更新 meta 后调用：重载类型标签缓存 */
    public void invalidateTypeLabels() {
        this.typeLabelCache = typeLabelsFromDb();
    }

    /** 发送线程池状态（后台系统监控：Active / Pool Size / Queue / Completed） */
    public Map<String, Object> senderPoolStats() {
        if (senderPool instanceof ThreadPoolExecutor tpe) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("active", tpe.getActiveCount());
            m.put("poolSize", tpe.getPoolSize());
            m.put("corePoolSize", tpe.getCorePoolSize());
            m.put("queueSize", tpe.getQueue().size());
            m.put("completed", tpe.getCompletedTaskCount());
            return m;
        }
        return Map.of();
    }

    // ---- 订阅管理 ----

    /**
     * 保存 / 更新订阅（同 endpoint 视为同一设备重新订阅，覆盖密钥）；同步注册设备记录。
     * endpoint 已属于其他设备时拒绝覆盖（防劫持）；返回该设备的 HMAC 所有权凭证。
     */
    public String subscribe(String deviceId, String endpoint, String p256dh, String auth, String userAgent) {
        PushSubscription existing = subscriptions.findByEndpoint(endpoint).orElse(null);
        if (existing != null && !existing.getDeviceId().equals(deviceId)) {
            throw new ConflictException("Subscription endpoint already owned by another device");
        }
        PushSubscription sub = existing != null ? existing : new PushSubscription();
        sub.setDeviceId(deviceId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        if (sub.getCreatedAt() == 0) sub.setCreatedAt(System.currentTimeMillis());
        subscriptions.save(sub);
        registerDevice(deviceId, userAgent);
        log.info("[push] 订阅已保存 deviceId={}", deviceId);
        return deviceAuth.issueCredential(deviceId);
    }

    public void unsubscribe(String deviceId, String endpoint) {
        subscriptions.findByEndpoint(endpoint)
                .filter(s -> s.getDeviceId().equals(deviceId))
                .ifPresent(subscriptions::delete);
        log.info("[push] 订阅已删除 deviceId={}", deviceId);
    }

    // ---- 设备注册（用户体系基础：匿名设备表，未来绑定用户实现多设备同步） ----

    /** 注册 / 刷新设备（幂等 upsert），由 UA 推断平台 */
    public void registerDevice(String deviceId, String userAgent) {
        UserDevice device = devices.findByDeviceId(deviceId).orElseGet(() -> {
            UserDevice d = new UserDevice();
            d.setDeviceId(deviceId);
            d.setCreatedAt(System.currentTimeMillis());
            return d;
        });
        device.setLastActiveAt(System.currentTimeMillis());
        device.setPlatform(platformOf(userAgent));
        devices.save(device);
    }

    /** UA → 平台分类（PC / Android / iOS PWA / 其他） */
    private String platformOf(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || ua.contains("ipad") || (ua.contains("macintosh") && ua.contains("mobile"))) return "ios";
        if (ua.contains("android")) return "android";
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) return "pc";
        return "other";
    }

    // ---- Fan-out 批量发送（PushTaskService 每批次调用一次） ----

    /**
     * 批量发送一个批次的任务（任务级并行）：
     * - 一次批量查询订阅（findByDeviceIdIn），避免每任务一次 N+1 查询
     * - 整批 (任务 × 订阅) 统一提交到有界线程池并发发送——若按任务逐个提交并等待，
     *   每任务只有 1 个订阅时会退化为纯串行（线程池 8 只用 1 个线程）
     * - 投递日志收集后统一 saveAll（避免每次发送一条 DB 写）
     * 订阅全部失效视为该任务完成（不重试）。
     */
    public List<TaskSendResult> sendBatch(List<PushTask> batch, byte[] payload) {
        List<String> deviceIds = batch.stream().map(PushTask::getDeviceId).distinct().toList();
        Map<String, List<PushSubscription>> subsByDevice = subscriptions.findByDeviceIdIn(deviceIds).stream()
                .collect(Collectors.groupingBy(PushSubscription::getDeviceId));
        // 1. 组装发送单元：整批所有 (任务 × 订阅) 一次提交，线程池内任务级并行
        List<DispatchUnit> units = new ArrayList<>();
        for (PushTask task : batch) {
            for (PushSubscription sub : subsByDevice.getOrDefault(task.getDeviceId(), List.of())) {
                units.add(new DispatchUnit(task.getId(), sub));
            }
        }
        List<TaskSendResult> results = new ArrayList<>(batch.size());
        if (units.isEmpty()) {
            for (PushTask task : batch) {
                results.add(new TaskSendResult(task.getId(), new SendOutcome(false, true, "no active subscription")));
            }
            return results;
        }
        // 2. 统一并发发送（单条 30s 超时 + cancel），日志收集后批量落库
        SendItem[] items = dispatch(units, payload);
        List<PushDeliveryLog> logs = new ArrayList<>(items.length);
        for (SendItem item : items) logs.add(item.logRow());
        if (!logs.isEmpty()) deliveryLogs.saveAll(logs);
        // 3. 按任务汇总结果：至少一台送达 SUCCESS / 订阅全失效 EXPIRED / 其余失败
        Map<Long, List<SendItem>> byTask = new HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            byTask.computeIfAbsent(units.get(i).taskId(), k -> new ArrayList<>()).add(items[i]);
        }
        for (PushTask task : batch) {
            List<SendItem> taskItems = byTask.getOrDefault(task.getId(), List.of());
            boolean any = false;
            int expired = 0;
            for (SendItem item : taskItems) {
                if (item.result() == SendResult.SUCCESS) any = true;
                else if (item.result() == SendResult.EXPIRED) expired++;
            }
            if (any) {
                results.add(new TaskSendResult(task.getId(), new SendOutcome(true, false, null)));
            } else if (!taskItems.isEmpty() && expired == taskItems.size()) {
                results.add(new TaskSendResult(task.getId(), new SendOutcome(false, true, "all subscriptions expired")));
            } else {
                results.add(new TaskSendResult(task.getId(), new SendOutcome(false, false, "push service delivery failed")));
            }
        }
        return results;
    }

    /** 发送测试通知（开发调试用）；至少一次发送成功返回 true */
    public boolean sendTest(String deviceId) {
        List<PushSubscription> subs = subscriptions.findByDeviceId(deviceId);
        if (subs.isEmpty()) return false;
        byte[] payload;
        try {
            payload = JSON.writeValueAsBytes(Map.of(
                    "title", "IdolCal 推送已开启 🎉",
                    "body", "提醒将在活动开始前按你的设置准时送达",
                    "icon", ICON,
                    "badge", BADGE,
                    "url", "/#/reminders",
                    "tag", "idolcal-test"));
        } catch (Exception e) {
            log.error("[push] payload 序列化失败", e);
            return false;
        }
        List<DispatchUnit> units = new ArrayList<>(subs.size());
        for (PushSubscription sub : subs) units.add(new DispatchUnit(null, sub));
        SendItem[] items = dispatch(units, payload);
        List<PushDeliveryLog> logs = new ArrayList<>(items.length);
        boolean ok = false;
        for (SendItem item : items) {
            logs.add(item.logRow());
            if (item.result() == SendResult.SUCCESS) ok = true;
        }
        if (!logs.isEmpty()) deliveryLogs.saveAll(logs);
        return ok;
    }

    // ---- 内部 ----

    /**
     * 有界并发投递：所有发送单元一次提交到线程池并发发送（任务级并行），
     * 等待全部完成（单条超时 SEND_TIMEOUT_MS，超时 cancel 并按失败处理，不阻塞整批）。
     * 返回结果 + 日志行，由调用方统一 saveAll。
     */
    private SendItem[] dispatch(List<DispatchUnit> units, byte[] payload) {
        SendItem[] items = new SendItem[units.size()];
        List<Future<?>> futures = new ArrayList<>(units.size());
        for (int i = 0; i < units.size(); i++) {
            final int idx = i;
            final DispatchUnit u = units.get(idx);
            futures.add(senderPool.submit(() -> items[idx] = send(u.sub(), payload, u.taskId())));
        }
        for (Future<?> f : futures) {
            try {
                f.get(SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // 超时：取消任务并中断线程。中断本身不一定能终止阻塞的 HTTP 调用，
                // 但 socket 超时兜底（SEND_TIMEOUT_MS），线程最终必然结束，不会泄漏。
                // cancel 成功 = 任务尚未开始（队列中）被移除，服务器未收到请求，补 FAILED 正确；
                // cancel 失败 = 任务已开始（请求可能已发出），必须等真实结果收尾，
                // 否则「实际已送达却记 FAILED」会让任务重试 → 用户收到重复推送
                if (f.cancel(true)) {
                    log.warn("[push] 单条发送超时已取消（{}s）", SEND_TIMEOUT_MS / 1000);
                } else {
                    log.warn("[push] 单条发送超时，等待在途请求收尾（{}s）", SEND_TIMEOUT_MS / 1000);
                    try {
                        f.get(); // 已开始的任务由 socket 超时兜底，必然结束
                    } catch (Exception ignored) {
                        // 任务异常已由任务自身写入结果 / 下方补日志兜底
                    }
                }
            } catch (Exception e) {
                // 中断 / 执行异常：按失败处理，不拖垮整批
                log.warn("[push] 单条发送任务异常：{}", e.getMessage());
            }
        }
        // 尚未开始执行即被取消的任务没有结果 → 补失败日志行（避免下游对 null 结果汇总）
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) continue;
            PushSubscription sub = units.get(i).sub();
            PushDeliveryLog row = new PushDeliveryLog();
            row.setTaskId(units.get(i).taskId());
            row.setDeviceId(sub.getDeviceId());
            row.setEndpoint(trimEndpoint(sub.getEndpoint()));
            row.setSentAt(System.currentTimeMillis());
            row.setHttpStatus(0);
            row.setResult("FAILED");
            row.setErrorMessage("timeout/cancelled before completion");
            items[i] = new SendItem(SendResult.FAILED, row);
        }
        return items;
    }

    /** 发送单条通知并生成投递日志行（不落库，由调用方批量 saveAll）；订阅失效（404/410）即时清理 */
    private SendItem send(PushSubscription sub, byte[] payload, Long taskId) {
        long sentAt = System.currentTimeMillis();
        PushDeliveryLog logRow = new PushDeliveryLog();
        logRow.setTaskId(taskId);
        logRow.setDeviceId(sub.getDeviceId());
        logRow.setEndpoint(trimEndpoint(sub.getEndpoint()));
        logRow.setSentAt(sentAt);
        try {
            var notification = new nl.martijndwars.webpush.Notification(
                    sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload);
            // preparePost 生成已签名加密的请求，实际发送走自定义超时客户端（连接 / 读取超时可控）
            var post = pushService.preparePost(notification, nl.martijndwars.webpush.Encoding.AES128GCM);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int code = response.getStatusLine().getStatusCode();
                logRow.setHttpStatus(code);
                if (code == 404 || code == 410) {
                    // 订阅已失效（设备卸载 / 过期）：清理
                    subscriptions.delete(sub);
                    logRow.setResult("EXPIRED");
                    log.info("[push] 订阅失效已清理（{}）", code);
                    return new SendItem(SendResult.EXPIRED, logRow);
                }
                if (code < 200 || code >= 300) {
                    logRow.setResult("FAILED");
                    logRow.setErrorMessage("http " + code);
                    log.warn("[push] 推送失败 status={}", code);
                    return new SendItem(SendResult.FAILED, logRow);
                }
                logRow.setResult("SUCCESS");
                log.info("[push] 推送成功 status={}", code);
                return new SendItem(SendResult.SUCCESS, logRow);
            }
        } catch (Exception e) {
            logRow.setHttpStatus(0);
            logRow.setResult("FAILED");
            logRow.setErrorMessage(e.getMessage());
            log.warn("[push] 推送异常：{}", e.getMessage());
            return new SendItem(SendResult.FAILED, logRow);
        }
    }

    /** endpoint 截断保存（避免超长列溢出） */
    private String trimEndpoint(String endpoint) {
        return endpoint == null ? "" : (endpoint.length() > 200 ? endpoint.substring(0, 200) : endpoint);
    }

    /** 通知内容：标题 + 开始时间 + 直达活动页（icon / badge / eventId 齐全）；Fan-out 批次共用一份 */
    public byte[] payloadFor(Event e) {
        String typeLabel = typeLabelCache.getOrDefault(e.getType(), e.getType());
        String title = typeLabel + " · " + e.getTitleZh();
        String body;
        if (e.getTime() == null || e.getTime().isBlank() || "00:00".equals(e.getTime())) {
            body = "全天活动 · " + e.getDate().replace("-", ".");
        } else {
            body = "开始时间 " + e.getDate().replace("-", ".") + " " + e.getTime() + " (" + (e.getTimezone() == null ? "KST" : e.getTimezone()) + ")";
        }
        try {
            return JSON.writeValueAsBytes(Map.of(
                    "title", title,
                    "body", body,
                    "icon", ICON,
                    "badge", BADGE,
                    "url", "/#/event/" + e.getId(),
                    "tag", "idolcal-event-" + e.getId(),
                    "eventId", e.getId()));
        } catch (Exception ex) {
            log.error("[push] payload 序列化失败", ex);
            return "{}".getBytes();
        }
    }

    /** meta.eventTypes JSON → typeId → 中文标签（启动时加载 / 写操作重载，不在发送路径查库） */
    private Map<String, String> typeLabelsFromDb() {
        Map<String, String> map = new HashMap<>();
        try {
            Meta m = meta.findById("eventTypes").orElse(null);
            if (m == null) return map;
            JsonNode arr = JSON.readTree(m.getMetaValue());
            for (JsonNode node : arr) {
                JsonNode label = node.get("label");
                String zh = label != null && label.get("zh-CN") != null ? label.get("zh-CN").asString() : null;
                if (zh == null && label != null && label.get("en") != null) zh = label.get("en").asString();
                if (zh != null) map.put(node.get("id").asString(), zh);
            }
        } catch (Exception ex) {
            log.warn("[push] 类型标签读取失败：{}", ex.getMessage());
        }
        return map;
    }
}
