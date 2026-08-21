package com.example.bim.api.service;

import com.example.bim.api.Enum.PushTaskStatus;
import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.PushDeliveryLog;
import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushSubscription;
import com.example.bim.api.entity.PushTask;
import com.example.bim.api.entity.User;
import com.example.bim.api.entity.UserDevice;
import com.example.bim.api.repository.ArtistRepository;
import com.example.bim.api.repository.ComebackRepository;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.PushDeliveryLogRepository;
import com.example.bim.api.repository.PushScheduleRepository;
import com.example.bim.api.repository.PushSubscriptionRepository;
import com.example.bim.api.repository.PushTaskRepository;
import com.example.bim.api.repository.TutorialRepository;
import com.example.bim.api.repository.UserDeviceRepository;
import com.example.bim.api.repository.UserRepository;
import com.example.bim.api.schedule.PushTaskScheduler;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 管理后台查询聚合（只读为主）：Dashboard 总览 / 用户 / 设备 / 订阅 / 调度任务 / 投递日志 / 系统监控。
 * 数据全部来自现有实体与 Repository，不改变业务写入逻辑；
 * 仅「清理失效设备」为管理操作，级联删除语义与 PushTaskService.syncReminders 保持一致。
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    /** 设备失效判定：超过该时长无任何订阅且未活动 → EXPIRED（可清理） */
    private static final long DEVICE_EXPIRED_MS = 90L * 24 * 3_600_000L;
    /** 调度时间线采样条数（批次粒度展示） */
    private static final int TIMELINE_SAMPLE = 500;
    /** 任务分页拉取上限（调度时间线取 taskId 用） */
    private static final int TASK_ID_LIMIT = 2000;

    private final UserRepository users;
    private final UserDeviceRepository devices;
    private final PushSubscriptionRepository subscriptions;
    private final PushTaskRepository tasks;
    private final PushScheduleRepository schedules;
    private final PushDeliveryLogRepository deliveryLogs;
    private final EventRepository events;
    private final ArtistRepository artists;
    private final ComebackRepository comebacks;
    private final TutorialRepository tutorials;
    private final WebPushService webPush;
    private final PushTaskScheduler pushScheduler;
    private final Scheduler quartz;
    private final DataSource dataSource;

    private final boolean authEnabled;
    private final long jwtTtlHours;
    private final boolean rateLimitEnabled;
    private final boolean cacheEnabled;
    private final int sendConcurrency;
    private final int fanoutBatchSize;
    private final long processingTimeoutMs;
    private final long deliveryLogRetentionDays;
    private final int loginMaxFailures;
    private final long loginLockMinutes;
    private final String adminUsername;

    public AdminService(UserRepository users, UserDeviceRepository devices,
                        PushSubscriptionRepository subscriptions, PushTaskRepository tasks,
                        PushScheduleRepository schedules, PushDeliveryLogRepository deliveryLogs,
                        EventRepository events, ArtistRepository artists,
                        ComebackRepository comebacks, TutorialRepository tutorials,
                        WebPushService webPush, PushTaskScheduler pushScheduler,
                        Scheduler quartz, DataSource dataSource,
                        @Value("${idolcal.auth.enabled:true}") boolean authEnabled,
                        @Value("${idolcal.auth.jwt-ttl-hours:24}") long jwtTtlHours,
                        @Value("${idolcal.rate-limit.enabled:true}") boolean rateLimitEnabled,
                        @Value("${idolcal.cache.enabled:true}") boolean cacheEnabled,
                        @Value("${idolcal.push.send-concurrency:8}") int sendConcurrency,
                        @Value("${idolcal.push.fanout-batch-size:500}") int fanoutBatchSize,
                        @Value("${idolcal.push.processing-timeout-ms:600000}") long processingTimeoutMs,
                        @Value("${idolcal.push.delivery-log-retention-days:90}") long deliveryLogRetentionDays,
                        @Value("${idolcal.auth.login-max-failures:5}") int loginMaxFailures,
                        @Value("${idolcal.auth.login-lock-minutes:15}") long loginLockMinutes,
                        @Value("${idolcal.auth.admin-username:admin}") String adminUsername) {
        this.users = users;
        this.devices = devices;
        this.subscriptions = subscriptions;
        this.tasks = tasks;
        this.schedules = schedules;
        this.deliveryLogs = deliveryLogs;
        this.events = events;
        this.artists = artists;
        this.comebacks = comebacks;
        this.tutorials = tutorials;
        this.webPush = webPush;
        this.pushScheduler = pushScheduler;
        this.quartz = quartz;
        this.dataSource = dataSource;
        this.authEnabled = authEnabled;
        this.jwtTtlHours = jwtTtlHours;
        this.rateLimitEnabled = rateLimitEnabled;
        this.cacheEnabled = cacheEnabled;
        this.sendConcurrency = sendConcurrency;
        this.fanoutBatchSize = fanoutBatchSize;
        this.processingTimeoutMs = processingTimeoutMs;
        this.deliveryLogRetentionDays = deliveryLogRetentionDays;
        this.loginMaxFailures = loginMaxFailures;
        this.loginLockMinutes = loginLockMinutes;
        this.adminUsername = adminUsername;
    }

    // ---- Dashboard 总览 ----

    /** 总览聚合：KPI + 7 天趋势 + 最近活动（一次请求完成 Dashboard 首屏） */
    public Map<String, Object> overview() {
        long now = System.currentTimeMillis();
        long dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        Map<String, Object> out = new LinkedHashMap<>();

        // 用户 / 设备 / 内容规模
        Map<String, Object> userStats = new LinkedHashMap<>();
        userStats.put("accounts", users.count());
        userStats.put("total", devices.count());
        userStats.put("active24h", devices.countByLastActiveAtGreaterThanEqual(now - 24 * 3_600_000L));
        userStats.put("new7d", devices.countByCreatedAtGreaterThanEqual(now - 7L * 24 * 3_600_000L));
        userStats.put("pushEnabled", subscriptions.countDistinctDeviceId());
        out.put("users", userStats);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("events", events.count());
        content.put("artists", artists.count());
        content.put("comebacks", comebacks.count());
        content.put("tutorials", tutorials.count());
        out.put("content", content);

        // 推送：今日投递 + 任务状态分布 + 即将触发
        Map<String, Object> push = new LinkedHashMap<>();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : deliveryLogs.countByResultSince(dayStart)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        long success = counts.getOrDefault("SUCCESS", 0L);
        long failed = counts.getOrDefault("FAILED", 0L);
        long expired = counts.getOrDefault("EXPIRED", 0L);
        Map<String, Object> today = new LinkedHashMap<>();
        today.put("total", success + failed + expired);
        today.put("success", success);
        today.put("failed", failed);
        today.put("expired", expired);
        long totalToday = success + failed + expired;
        today.put("successRate", totalToday == 0 ? 0 : Math.round(success * 1000.0 / totalToday) / 10.0);
        push.put("today", today);
        push.put("schedules", distribution(schedules.countByStatus()));
        push.put("tasks", distribution(tasks.countByStatus()));
        out.put("push", push);

        // 7 天趋势（按 UTC 自然日聚合，缺失日补零）
        long weekAgo = now - 7L * 24 * 3_600_000L;
        Map<Long, Map<String, Long>> byDay = new LinkedHashMap<>();
        for (Object[] row : deliveryLogs.countByDaySince(weekAgo)) {
            long day = ((Number) row[0]).longValue();
            byDay.computeIfAbsent(day, k -> new LinkedHashMap<>())
                    .merge((String) row[1], (Long) row[2], Long::sum);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            long day = (now - i * 86_400_000L) / 86_400_000L;
            Map<String, Long> row = byDay.getOrDefault(day, Map.of());
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("day", day);
            t.put("sent", row.values().stream().mapToLong(Long::longValue).sum());
            t.put("success", row.getOrDefault("SUCCESS", 0L));
            t.put("failed", row.getOrDefault("FAILED", 0L));
            t.put("expired", row.getOrDefault("EXPIRED", 0L));
            trend.add(t);
        }
        out.put("trend", trend);

        // 最近活动：投递日志 + 新建调度 + 新设备合并时间线
        out.put("activity", recentActivity(now));
        return out;
    }

    /** 最近活动时间线（投递 / 调度 / 设备三类事件合并，取最新 12 条） */
    private List<Map<String, Object>> recentActivity(long now) {
        List<Map<String, Object>> items = new ArrayList<>();

        for (PushDeliveryLog l : deliveryLogs.findAll(PageRequest.of(0, 8)).getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts", l.getSentAt());
            m.put("type", "delivery");
            m.put("status", l.getResult());
            m.put("title", switch (l.getResult()) {
                case "SUCCESS" -> "Push delivered";
                case "EXPIRED" -> "Push device expired";
                default -> "Push delivery failed";
            });
            m.put("detail", shortId(l.getDeviceId()) + (l.getErrorMessage() != null ? " · " + l.getErrorMessage() : ""));
            items.add(m);
        }
        for (PushSchedule s : schedules.findAll(PageRequest.of(0, 5, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts", s.getCreatedAt());
            m.put("type", "schedule");
            m.put("status", s.getStatus().name());
            m.put("title", "Push schedule created");
            m.put("detail", eventLabel(s.getEventId()) + " · " + offsetLabel(s.getOffsetMinutes()));
            items.add(m);
        }
        for (UserDevice d : devices.findAll(PageRequest.of(0, 3, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts", d.getCreatedAt());
            m.put("type", "device");
            m.put("status", "REGISTERED");
            m.put("title", "Device registered");
            m.put("detail", shortId(d.getDeviceId()) + " · " + (d.getPlatform() == null ? "unknown" : d.getPlatform()));
            items.add(m);
        }
        items.sort(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("ts")).reversed());
        return items.size() > 12 ? items.subList(0, 12) : items;
    }

    // ---- 用户 / 设备 / 订阅 ----

    /** 用户账号分页（users 表：管理账号；匿名用户以设备为锚点见 devices） */
    public Page<User> users(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return users.findAll(pageable);
        return users.findByUsernameContainingIgnoreCase(q.trim(), pageable);
    }

    /**
     * 设备分页（匿名用户锚点）。status 计算：
     * ACTIVE=存在订阅；EXPIRED=无订阅且超过 90 天未活动（可清理）；INACTIVE=无订阅但近期活跃。
     */
    public Page<Map<String, Object>> devices(String q, String status, Pageable pageable) {
        long now = System.currentTimeMillis();
        Page<UserDevice> page = devices.search(normalize(q), pageable);
        List<Map<String, Object>> rows = new ArrayList<>(page.getContent().size());
        for (UserDevice d : page.getContent()) {
            boolean hasSub = !subscriptions.findByDeviceId(d.getDeviceId()).isEmpty();
            String st = deviceStatus(d, hasSub, now);
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase(st)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("deviceId", d.getDeviceId());
            m.put("userId", d.getUserId());
            m.put("platform", d.getPlatform());
            m.put("createdAt", d.getCreatedAt());
            m.put("lastActiveAt", d.getLastActiveAt());
            m.put("status", st);
            m.put("subscriptions", hasSub ? subscriptions.findByDeviceId(d.getDeviceId()).size() : 0);
            rows.add(m);
        }
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    /** 订阅分页（push_subscriptions：浏览器 Push 投递地址） */
    public Page<Map<String, Object>> subscriptions(Pageable pageable) {
        long now = System.currentTimeMillis();
        Page<PushSubscription> page = subscriptions.findAll(pageable);
        Map<String, UserDevice> deviceCache = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>(page.getContent().size());
        for (PushSubscription s : page.getContent()) {
            UserDevice d = deviceCache.get(s.getDeviceId());
            if (d == null) {
                d = devices.findByDeviceId(s.getDeviceId()).orElse(null);
                if (d != null) deviceCache.put(s.getDeviceId(), d);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("deviceId", s.getDeviceId());
            m.put("endpoint", s.getEndpoint());
            m.put("createdAt", s.getCreatedAt());
            m.put("platform", d == null ? null : d.getPlatform());
            m.put("lastActiveAt", d == null ? null : d.getLastActiveAt());
            m.put("status", d == null ? "ORPHAN" : deviceStatus(d, true, now));
            rows.add(m);
        }
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    // ---- 清理失效设备（唯一管理写操作） ----

    /** 清理单个设备：订阅 + 任务 + 设备行（任务删除后顺带清理孤儿调度） */
    @Transactional
    public Map<String, Object> deleteDevice(String deviceId) {
        removeDevice(deviceId);
        return Map.of("removed", 1, "deviceId", deviceId);
    }

    /** 批量清理 EXPIRED 设备（无订阅且超过 90 天未活动），返回删除数量 */
    @Transactional
    public Map<String, Object> cleanExpired() {
        long now = System.currentTimeMillis();
        long cutoff = now - DEVICE_EXPIRED_MS;
        AtomicLong removed = new AtomicLong();
        Set<Long> orphanCandidates = new HashSet<>();
        for (UserDevice d : devices.findAll()) {
            boolean hasSub = !subscriptions.findByDeviceId(d.getDeviceId()).isEmpty();
            if (hasSub || d.getLastActiveAt() >= cutoff) continue;
            collectOrphanSchedules(d.getDeviceId(), orphanCandidates);
            devices.deleteByDeviceId(d.getDeviceId());
            subscriptions.deleteByDeviceId(d.getDeviceId());
            tasks.deleteByDeviceId(d.getDeviceId());
            removed.incrementAndGet();
            log.info("[admin] 清理失效设备 {}（平台 {}，最后活跃 {}）",
                    d.getDeviceId(), d.getPlatform(), d.getLastActiveAt());
        }
        cleanupOrphanSchedules(orphanCandidates);
        return Map.of("removed", removed.get());
    }

    /** 删除设备 + 级联订阅/任务，并清理孤儿调度 */
    private void removeDevice(String deviceId) {
        Set<Long> orphanCandidates = new HashSet<>();
        collectOrphanSchedules(deviceId, orphanCandidates);
        devices.deleteByDeviceId(deviceId);
        subscriptions.deleteByDeviceId(deviceId);
        tasks.deleteByDeviceId(deviceId);
        cleanupOrphanSchedules(orphanCandidates);
        log.info("[admin] 删除设备 {}", deviceId);
    }

    /** 记录设备任务涉及的全部调度（删除后可能成为孤儿） */
    private void collectOrphanSchedules(String deviceId, Set<Long> out) {
        for (PushTask t : tasks.findByDeviceId(deviceId)) {
            if (t.getScheduleId() != null) out.add(t.getScheduleId());
        }
    }

    /** 调度下已无任何未完成任务 → 删除调度 + Quartz Job（与 PushTaskService 清理语义一致） */
    private void cleanupOrphanSchedules(Collection<Long> scheduleIds) {
        List<PushTaskStatus> active = List.of(PushTaskStatus.PENDING, PushTaskStatus.PROCESSING, PushTaskStatus.RETRY);
        for (Long sid : scheduleIds) {
            if (sid == null || tasks.countByScheduleIdAndStatusIn(sid, active) > 0) continue;
            PushSchedule s = schedules.findById(sid).orElse(null);
            if (s == null) continue;
            if (s.getStatus() == PushTaskStatus.SUCCESS || s.getStatus() == PushTaskStatus.FAILED) continue;
            pushScheduler.unschedule(s);
            schedules.delete(s);
            log.info("[admin] 调度 {} 已无未完成任务，随设备清理删除", sid);
        }
    }

    // ---- 推送调度 / 任务 / 投递日志 ----

    /** 调度分页 + 任务聚合（target / success / failed / expired / progress） */
    public Page<Map<String, Object>> schedules(String status, Pageable pageable) {
        Page<PushSchedule> page;
        PushTaskStatus st = parseStatus(status);
        if (st == null) {
            page = schedules.findAll(pageable);
        } else {
            page = schedules.findByStatus(st, pageable);
        }
        // 一次查询全量调度任务分布，内存聚合避免 N+1
        Map<Long, Map<String, Long>> dist = new HashMap<>();
        for (Object[] row : tasks.countGroupedByScheduleAndStatus()) {
            Long sid = (Long) row[0];
            if (sid == null) continue;
            dist.computeIfAbsent(sid, k -> new LinkedHashMap<>())
                    .merge(row[1].toString(), (Long) row[2], Long::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>(page.getContent().size());
        for (PushSchedule s : page.getContent()) {
            rows.add(scheduleRow(s, dist.getOrDefault(s.getId(), Map.of())));
        }
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    /** 调度详情：调度 + 事件信息 + 任务状态分布 + 投递时间线样本 */
    public Map<String, Object> scheduleDetail(Long id) {
        PushSchedule s = schedules.findById(id).orElse(null);
        if (s == null) return null;
        Map<String, Object> out = scheduleRow(s, distributionMap(tasks.countByScheduleIdGrouped(id)));

        Event event = events.findById(s.getEventId()).orElse(null);
        Map<String, Object> eventInfo = new LinkedHashMap<>();
        if (event != null) {
            eventInfo.put("id", event.getId());
            eventInfo.put("title", event.getTitleZh());
            eventInfo.put("titleEn", event.getTitleEn());
            eventInfo.put("artist", event.getArtist());
            eventInfo.put("date", event.getDate());
            eventInfo.put("time", event.getTime());
            eventInfo.put("timezone", event.getTimezone());
            eventInfo.put("type", event.getType());
            eventInfo.put("status", event.getStatus());
            eventInfo.put("isOfficial", event.isOfficial());
        }
        out.put("event", eventInfo);

        // 投递时间线：调度下任务日志样本，按秒聚合为批次（Batch #n）
        List<Long> taskIds = tasks.findByScheduleId(id, PageRequest.of(0, TASK_ID_LIMIT))
                .getContent().stream().map(PushTask::getId).toList();
        List<Map<String, Object>> timeline = new ArrayList<>();
        if (!taskIds.isEmpty()) {
            long batchNo = 0;
            long bucketSec = -1;
            Map<String, Object> bucket = null;
            for (PushDeliveryLog l : deliveryLogs.findTop500ByTaskIdInOrderBySentAtAsc(taskIds)) {
                long sec = l.getSentAt() / 1000;
                if (sec != bucketSec) {
                    if (bucket != null) timeline.add(bucket);
                    bucket = new LinkedHashMap<>();
                    bucket.put("batch", ++batchNo);
                    bucket.put("ts", sec * 1000);
                    bucket.put("success", 0L);
                    bucket.put("failed", 0L);
                    bucket.put("expired", 0L);
                    bucketSec = sec;
                }
                String resultKey = l.getResult().toLowerCase();
                bucket.put(resultKey, ((Number) bucket.getOrDefault(resultKey, 0L)).longValue() + 1);
            }
            if (bucket != null) timeline.add(bucket);
        }
        out.put("timeline", timeline);
        return out;
    }

    /** 设备任务分页（调度详情页明细） */
    public Page<PushTask> tasks(Long scheduleId, String status, Pageable pageable) {
        PushTaskStatus st = parseStatus(status);
        if (scheduleId != null && st != null) return tasks.findByScheduleIdAndStatus(scheduleId, st, pageable);
        if (scheduleId != null) return tasks.findByScheduleId(scheduleId, pageable);
        if (st != null) return tasks.findByStatus(st, pageable);
        return tasks.findAll(pageable);
    }

    /** 投递日志分页（result / 设备 / endpoint 筛选） */
    public Page<PushDeliveryLog> deliveries(String result, String q, Pageable pageable) {
        String r = (result == null || result.isBlank()) ? null : result.trim().toUpperCase();
        return deliveryLogs.search(r, normalize(q), pageable);
    }

    // ---- 系统监控 ----

    /** 系统监控：JVM / Push 线程池 / 数据库连接池 / Quartz / 环境配置 */
    public Map<String, Object> system() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jvm", jvmStats());
        out.put("pushWorker", webPush.senderPoolStats());
        out.put("database", dbStats());
        out.put("quartz", quartzStats());
        out.put("config", configStats());
        return out;
    }

    private Map<String, Object> jvmStats() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maxMemory", rt.maxMemory());
        m.put("totalMemory", rt.totalMemory());
        m.put("freeMemory", rt.freeMemory());
        m.put("usedMemory", rt.totalMemory() - rt.freeMemory());
        m.put("threads", Thread.activeCount());
        m.put("cpuCores", rt.availableProcessors());
        m.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        m.put("startTimeMs", ManagementFactory.getRuntimeMXBean().getStartTime());
        return m;
    }

    private Map<String, Object> dbStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("connected", true);
        if (dataSource instanceof HikariDataSource hikari) {
            m.put("active", hikari.getHikariPoolMXBean().getActiveConnections());
            m.put("idle", hikari.getHikariPoolMXBean().getIdleConnections());
            m.put("total", hikari.getHikariPoolMXBean().getTotalConnections());
            m.put("awaiting", hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
            m.put("url", safeDbUrl(hikari.getJdbcUrl()));
        } else {
            m.put("url", dataSource.getClass().getSimpleName());
        }
        return m;
    }

    /** JDBC URL 脱敏：去掉密码参数，仅保留定位信息 */
    private String safeDbUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("(?i)(password=)[^;&]*", "$1***");
    }

    private Map<String, Object> quartzStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            m.put("running", quartz.isStarted() && !quartz.isInStandbyMode());
            m.put("standby", quartz.isInStandbyMode());
            m.put("jobs", quartz.getJobKeys(GroupMatcher.anyGroup()).size());
            m.put("executedJobs", quartz.getMetaData().getNumberOfJobsExecuted());
            Long next = null;
            for (org.quartz.TriggerKey key : quartz.getTriggerKeys(GroupMatcher.anyGroup())) {
                Trigger t = quartz.getTrigger(key);
                if (t != null && t.getNextFireTime() != null) {
                    long t0 = t.getNextFireTime().getTime();
                    if (next == null || t0 < next) next = t0;
                }
            }
            m.put("nextFireAt", next);
        } catch (SchedulerException e) {
            log.warn("[admin] Quartz 状态读取失败：{}", e.getMessage());
            m.put("running", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> configStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("authEnabled", authEnabled);
        m.put("adminUsername", adminUsername);
        m.put("jwtTtlHours", jwtTtlHours);
        m.put("loginMaxFailures", loginMaxFailures);
        m.put("loginLockMinutes", loginLockMinutes);
        m.put("rateLimitEnabled", rateLimitEnabled);
        m.put("cacheEnabled", cacheEnabled);
        m.put("sendConcurrency", sendConcurrency);
        m.put("fanoutBatchSize", fanoutBatchSize);
        m.put("processingTimeoutMs", processingTimeoutMs);
        m.put("deliveryLogRetentionDays", deliveryLogRetentionDays);
        return m;
    }

    // ---- 工具 ----

    private PushTaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return PushTaskStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalize(String q) {
        if (q == null || q.isBlank()) return null;
        return q.trim();
    }

    private String shortId(String s) {
        if (s == null) return "-";
        return s.length() > 16 ? s.substring(0, 16) + "…" : s;
    }

    private String eventLabel(String eventId) {
        Event e = events.findById(eventId).orElse(null);
        return e == null ? eventId : e.getTitleZh();
    }

    private String offsetLabel(int offsetMinutes) {
        if (offsetMinutes == 0) return "on time";
        if (offsetMinutes % 1440 == 0) return offsetMinutes / 1440 + "d before";
        if (offsetMinutes % 60 == 0) return offsetMinutes / 60 + "h before";
        return offsetMinutes + "m before";
    }

    private Map<String, Object> distribution(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PushTaskStatus s : PushTaskStatus.values()) counts.put(s.name(), 0L);
        for (Object[] row : rows) counts.put(row[0].toString(), (Long) row[1]);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("byStatus", counts);
        return out;
    }

    private Map<String, Long> distributionMap(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) counts.put(row[0].toString(), (Long) row[1]);
        return counts;
    }

    /** 调度行：基础字段 + 任务聚合（target / success / failed / expired / progress 0~100） */
    private Map<String, Object> scheduleRow(PushSchedule s, Map<String, Long> dist) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("eventId", s.getEventId());
        m.put("eventTitle", eventLabel(s.getEventId()));
        m.put("offsetMinutes", s.getOffsetMinutes());
        m.put("triggerAt", s.getTriggerAt());
        m.put("status", s.getStatus().name());
        m.put("retryCount", s.getRetryCount());
        m.put("nextRetryAt", s.getNextRetryAt());
        m.put("processedAt", s.getProcessedAt());
        m.put("finishedAt", s.getFinishedAt());
        m.put("createdAt", s.getCreatedAt());
        long target = dist.values().stream().mapToLong(Long::longValue).sum();
        long success = dist.getOrDefault("SUCCESS", 0L);
        long failed = dist.getOrDefault("FAILED", 0L);
        long expired = dist.getOrDefault("EXPIRED", 0L);
        long done = success + failed + expired;
        m.put("target", target);
        m.put("success", success);
        m.put("failed", failed);
        m.put("expired", expired);
        m.put("pending", dist.getOrDefault("PENDING", 0L) + dist.getOrDefault("RETRY", 0L) + dist.getOrDefault("PROCESSING", 0L));
        m.put("progress", target == 0 ? 0 : Math.round(done * 100.0 / target));
        return m;
    }

    /** 设备状态：有订阅=ACTIVE；无订阅且超 90 天未活动=EXPIRED；否则 INACTIVE */
    private String deviceStatus(UserDevice d, boolean hasSub, long now) {
        if (hasSub) return "ACTIVE";
        return d.getLastActiveAt() < now - DEVICE_EXPIRED_MS ? "EXPIRED" : "INACTIVE";
    }
}
