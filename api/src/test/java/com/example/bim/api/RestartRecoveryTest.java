package com.example.bim.api;

import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushSubscription;
import com.example.bim.api.entity.PushTask;
import com.example.bim.api.entity.PushTaskStatus;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.PushScheduleRepository;
import com.example.bim.api.repository.PushSubscriptionRepository;
import com.example.bim.api.repository.PushTaskRepository;
import com.example.bim.api.service.PushTaskService;
import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 重启恢复故障注入测试（真实链路，仅替换 Push Service 为本地假服务）：
 * 模拟进程崩溃 / 重启后 Quartz 内存 Job 丢失，由 PushScheduler 兜底扫描
 * （recoverPending / recoverStale / retryDue）恢复——与生产每分钟 recoveryScan 同入口。
 * <p>
 * 1. quartzLostRecovery —— 调度 PENDING 未触发（Quartz 丢失）：recoverPending 补跑
 * 2. processingStuckRecovery —— 崩溃时卡在 PROCESSING：recoverStale 重置 →
 * recoverPending 补跑，崩溃 Worker 的迟到结果必须被 attempt 防护丢弃
 * 3. retryDueRecovery —— 调度 RETRY 到期：retryDue 补跑
 * 4. partialCompletedRecovery —— 部分任务已完成时崩溃：补跑只发未完成任务（绝不重复推送）
 * <p>
 * 运行：mvnw.cmd test -Dtest=RestartRecoveryTest（H2 内存库，不影响开发数据）
 * 报告：target/restart-recovery-report.txt
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.datasource.url=jdbc:h2:mem:restart;DB_CLOSE_DELAY=-1"})
class RestartRecoveryTest {

    private static final int FAKE_PORT = 19115; // 类级单端口：连接池连接始终指向活着的 server，杜绝 dead 连接复用
    private static final long STALL_MS = 35_000L;   // 卡死时长（> socket 超时 30s）
    private static final long NORMAL_DELAY_MS = 50L; // 正常模式单请求延迟

    @Autowired PushScheduleRepository schedules;
    @Autowired PushTaskRepository tasks;
    @Autowired PushSubscriptionRepository subs;
    @Autowired EventRepository events;
    @Autowired JdbcTemplate jdbc;
    @Autowired PushTaskService pushTaskService;

    /** 类级假 Push Service：整个测试类期间不重启，连接池无 dead 连接 */
    private static HttpServer fakePush;
    private static final AtomicLong totalRequests = new AtomicLong();
    private static final AtomicLong stallHits = new AtomicLong();
    private static final AtomicInteger stallBudget = new AtomicInteger();

    private static final StringBuilder report = new StringBuilder();

    @BeforeAll
    static void startFakePush() throws IOException {
        fakePush = HttpServer.create(new InetSocketAddress("127.0.0.1", FAKE_PORT), 0);
        fakePush.createContext("/push/", exchange -> {
            totalRequests.incrementAndGet();
            if (stall()) {
                stallHits.incrementAndGet();
                try {
                    Thread.sleep(STALL_MS);
                } catch (InterruptedException ignored) {
                }
            } else {
                try {
                    Thread.sleep(NORMAL_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
            URI uri = exchange.getRequestURI();
            int code = uri.getPath().contains("/exp/") ? 410 : 201;
            try {
                exchange.sendResponseHeaders(code, -1);
            } catch (Exception ignored) {
            }
            exchange.close();
        });
        fakePush.setExecutor(Executors.newFixedThreadPool(32));
        fakePush.start();
    }

    @AfterAll
    static void stopFakePush() {
        fakePush.stop(0);
    }

    /** 每测试重置计数与卡死开关（类级 server 常驻，仅状态清零） */
    @BeforeEach
    void resetCounters() {
        totalRequests.set(0);
        stallHits.set(0);
        stallBudget.set(0);
    }

    private static boolean stall() {
        int b = stallBudget.get();
        while (b > 0) {
            if (stallBudget.compareAndSet(b, b - 1)) return true;
            b = stallBudget.get();
        }
        return false;
    }

    // ==================== 测试 1：Quartz 丢失（PENDING 未触发 → recoverPending 补跑） ====================

    @Test
    void quartzLostRecovery() throws Exception {
        long now = System.currentTimeMillis();
        Round r = prepareRound("restart-a", 8, now - 3_600_000, now - 1_000); // 已过期，不注册 Quartz（= 重启后 Job 丢失）
        report.append("===== 测试 1：Quartz 丢失（调度 #%d PENDING 已过期，不注册 Quartz）=====\n".formatted(r.scheduleId()));

        pushTaskService.recoverPending(); // 兜底扫描：PENDING 且 triggerAt<=now → 补跑

        PushTaskStatus st = waitStatus(r.scheduleId(), s -> s == PushTaskStatus.SUCCESS
                || s == PushTaskStatus.FAILED, 30_000);
        assertEquals(PushTaskStatus.SUCCESS, st, "recoverPending 应补跑成功");
        PushSchedule s = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(1L, s.getAttemptId(), "补跑 attempt 应为 1");
        assertTrue(allTasksDone(r.scheduleId(), PushTaskStatus.SUCCESS), "任务应全 SUCCESS");
        Map<String, Long> logs = deliveryStats(r.scheduleId());
        assertEquals(8L, logs.values().stream().mapToLong(Long::longValue).sum(), "投递日志恰 8 条：" + logs);
        assertTrue(!logs.containsKey("FAILED"), "不应有失败：" + logs);
        report.append("PENDING 调度被 recoverPending 补跑：SUCCESS（attempt=1），任务 8 全 SUCCESS，日志 8 条无失败\n\n");
    }

    // ==================== 测试 2：崩溃在发送中（PROCESSING 卡死 → recoverStale 重置 → 补跑） ====================

    @Test
    void processingStuckRecovery() throws Exception {
        long now = System.currentTimeMillis();
        Round r = prepareRound("restart-b", 8, now - 1_000, now - 1_000);
        report.append("===== 测试 2：崩溃在发送中（调度 #%d PROCESSING 卡死 → recoverStale 重置 → 补跑）=====\n".formatted(r.scheduleId()));
        stallBudget.set(8); // 崩溃 Worker 的 8 个请求卡死，补跑的请求正常

        // 模拟崩溃前正在发送：Worker 认领并卡死在 sendBatch
        AtomicReference<Throwable> crashError = new AtomicReference<>();
        Thread crashWorker = new Thread(() -> {
            try {
                pushTaskService.processSchedule(r.scheduleId());
            } catch (Throwable t) {
                crashError.set(t);
            }
        }, "crash-worker");
        crashWorker.start();
        assertTrue(waitAttempt(r.scheduleId(), 1, 10_000), "崩溃 Worker 应认领 attempt=1");
        // 模拟进程崩溃后的时间流逝：processed_at 已超阈值
        jdbc.update("UPDATE push_schedules SET processed_at = ? WHERE id = ?", now - 600_000, r.scheduleId());

        // 重启后兜底扫描：recoverStale 把卡死的 PROCESSING 重置为 PENDING（不立即重发）
        pushTaskService.recoverStale();
        assertEquals(PushTaskStatus.PENDING, schedules.findById(r.scheduleId()).orElseThrow().getStatus(),
                "recoverStale 应把崩溃调度重置为 PENDING");

        // 生产时序：recoverStale 阈值（>30s）远大于单条发送超时，崩溃 Worker 的线程早已被
        // socket 超时兜底释放；这里同样等其释放后再补跑，否则补跑请求排队 30s 撞上
        // Future.get 超时窗口，批首任务可能被取消（测试失去确定性）
        assertEquals(0, waitSenderIdle(60_000), "崩溃 Worker 的发送线程应在 60s 内全部释放（socket 超时兜底）");

        // 下轮扫描 recoverPending 补跑（attempt=2；线程已空闲，请求不排队）
        pushTaskService.recoverPending();
        PushTaskStatus st = waitStatus(r.scheduleId(), s -> s == PushTaskStatus.SUCCESS
                || s == PushTaskStatus.FAILED, 60_000);
        assertEquals(PushTaskStatus.SUCCESS, st, "补跑应成功");
        PushSchedule s = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(2L, s.getAttemptId(), "补跑 attempt 应为 2");

        // 崩溃 Worker 的线程最终超时退出，迟到结果必须被丢弃
        crashWorker.join(60_000);
        assertFalse(crashWorker.isAlive(), "崩溃 Worker 线程必须退出（socket 超时兜底）");
        assertNull(crashError.get(), "崩溃 Worker 不应抛异常：" + crashError.get());
        PushSchedule after = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(2L, after.getAttemptId(), "崩溃 Worker 迟到收尾不得倒灌 attempt");
        assertEquals(PushTaskStatus.SUCCESS, after.getStatus(), "崩溃 Worker 迟到收尾不得覆盖状态");

        assertTrue(allTasksDone(r.scheduleId(), PushTaskStatus.SUCCESS), "任务应全 SUCCESS");
        Map<String, Long> logs = deliveryStats(r.scheduleId());
        assertEquals(8L, logs.getOrDefault("FAILED", 0L), "崩溃 Worker 的 8 次投递应为 FAILED（事实记录）：" + logs);
        assertEquals(8L, logs.getOrDefault("SUCCESS", 0L) + logs.getOrDefault("EXPIRED", 0L),
                "补跑 8 条应全部完成：" + logs);
        assertEquals(16L, logs.values().stream().mapToLong(Long::longValue).sum(), "共 16 条日志");
        assertEquals(8L, stallHits.get(), "恰好 8 个请求命中卡死（崩溃 Worker）");
        assertEquals(0, waitSenderIdle(10_000), "所有发送线程必须释放");
        report.append("recoverStale 重置 + recoverPending 补跑成功：attempt=2，崩溃 Worker 迟到结果被丢弃，任务归补跑\n\n");
    }

    // ==================== 测试 3：RETRY 到期（retryDue 补跑） ====================

    @Test
    void retryDueRecovery() throws Exception {
        long now = System.currentTimeMillis();
        Round r = prepareRound("restart-c", 8, now - 1_000, now - 1_000);
        report.append("===== 测试 3：RETRY 到期（调度 #%d → retryDue 补跑）=====\n".formatted(r.scheduleId()));
        // 模拟上一轮发送失败后进入退避：任务与调度均为 RETRY 且已到重试时间
        jdbc.update("UPDATE push_tasks SET status = 'RETRY', retry_count = 1, next_retry_at = ?, error_message = 'injected' "
                + "WHERE schedule_id = ?", now - 1_000, r.scheduleId());
        jdbc.update("UPDATE push_schedules SET status = 'RETRY', retry_count = 1, next_retry_at = ? WHERE id = ?",
                now - 1_000, r.scheduleId());

        pushTaskService.retryDue(); // 兜底扫描：RETRY 且 next_retry_at<=now → 补跑

        PushTaskStatus st = waitStatus(r.scheduleId(), s -> s == PushTaskStatus.SUCCESS
                || s == PushTaskStatus.FAILED, 30_000);
        assertEquals(PushTaskStatus.SUCCESS, st, "retryDue 应补跑成功");
        PushSchedule s = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(1L, s.getAttemptId(), "RETRY 调度补跑 attempt 应为 1（CAS 认领 RETRY）");
        assertTrue(allTasksDone(r.scheduleId(), PushTaskStatus.SUCCESS), "任务应全 SUCCESS");
        Map<String, Long> logs = deliveryStats(r.scheduleId());
        assertEquals(8L, logs.values().stream().mapToLong(Long::longValue).sum(), "投递日志恰 8 条：" + logs);
        assertTrue(!logs.containsKey("FAILED"), "不应有失败：" + logs);
        report.append("RETRY 调度被 retryDue 补跑：SUCCESS（attempt=1），任务 8 全 SUCCESS\n\n");
    }

    // ==================== 测试 4：部分完成时崩溃（补跑只发未完成，绝不重复推送） ====================

    @Test
    void partialCompletedRecovery() throws Exception {
        long now = System.currentTimeMillis();
        Round r = prepareRound("restart-d", 16, now - 1_000, now - 1_000);
        report.append("===== 测试 4：部分完成崩溃（调度 #%d 一半任务已 SUCCESS 时崩溃）=====\n".formatted(r.scheduleId()));
        // 模拟崩溃前已完成前 8 个任务（已发送并落库 SUCCESS），后 8 个仍 PENDING
        jdbc.update("UPDATE push_tasks SET status = 'SUCCESS', sent_at = ? WHERE schedule_id = ? AND id IN "
                + "(SELECT id FROM push_tasks WHERE schedule_id = ? ORDER BY id LIMIT 8)",
                now - 60_000, r.scheduleId(), r.scheduleId());
        assertEquals(8L, tasks.countByScheduleIdAndStatusIn(r.scheduleId(),
                List.of(PushTaskStatus.SUCCESS)), "前 8 个任务应已标记 SUCCESS");
        assertEquals(8L, tasks.countByScheduleIdAndStatusIn(r.scheduleId(),
                List.of(PushTaskStatus.PENDING)), "后 8 个任务应仍 PENDING");

        pushTaskService.recoverPending(); // 重启后兜底补跑

        PushTaskStatus st = waitStatus(r.scheduleId(), s -> s == PushTaskStatus.SUCCESS
                || s == PushTaskStatus.FAILED, 30_000);
        assertEquals(PushTaskStatus.SUCCESS, st, "补跑应成功");
        assertEquals(16L, tasks.countByScheduleIdAndStatusIn(r.scheduleId(),
                List.of(PushTaskStatus.SUCCESS)), "16 任务应全 SUCCESS");
        // 关键：投递日志必须恰好 8 条（只补发了未完成的 8 个），已完成的任务绝不重复推送
        Map<String, Long> logs = deliveryStats(r.scheduleId());
        assertEquals(8L, logs.values().stream().mapToLong(Long::longValue).sum(),
                "投递日志必须恰 8 条（已完成任务不重复推送）：" + logs);
        assertEquals(0, waitSenderIdle(10_000), "线程必须释放");
        report.append("部分完成崩溃补跑：只发未完成 8 个，日志恰 8 条（无重复推送），任务 16 全 SUCCESS\n\n");
    }

    // ==================== 辅助 ====================

    /** 准备一档：Event + Schedule（不注册 Quartz，等价于重启后 Job 丢失）+ N 订阅 + N 任务（1% 过期订阅） */
    private Round prepareRound(String eventId, int size, long scheduleTriggerAt, long taskTriggerAt) {
        long now = System.currentTimeMillis();
        Event ev = new Event();
        ev.setId(eventId);
        ev.setArtist("restart-test");
        ev.setDate("2026-08-20");
        ev.setTime("18:00");
        ev.setTimezone("KST");
        ev.setStartAtUtc(scheduleTriggerAt + 30 * 60_000L);
        ev.setTitleEn("Restart " + eventId);
        ev.setTitleZh("重启 " + eventId);
        ev.setTitleKo("재시작 " + eventId);
        ev.setType("music-show");
        ev.setStatus("CONFIRMED");
        ev.setOfficial(true);
        events.save(ev);

        PushSchedule s = new PushSchedule();
        s.setEventId(ev.getId());
        s.setOffsetMinutes(30);
        s.setTriggerAt(scheduleTriggerAt);
        s.setStatus(PushTaskStatus.PENDING);
        s.setCreatedAt(now);
        schedules.save(s);

        List<PushSubscription> subList = new ArrayList<>(size);
        List<PushTask> taskList = new ArrayList<>(size);
        for (int k = 0; k < size; k++) {
            boolean expired = k % 100 == 0;
            String deviceId = String.format("%s-%d-%06d", expired ? "exp" : "dev", s.getId(), k);
            PushSubscription sub = new PushSubscription();
            sub.setDeviceId(deviceId);
            sub.setEndpoint("http://127.0.0.1:" + FAKE_PORT + "/push/" + s.getId() + "/" + (expired ? "exp/" : "dev/") + k);
            sub.setP256dh(ecRawKey());
            sub.setAuth(authKey());
            sub.setCreatedAt(now);
            subList.add(sub);
            PushTask t = new PushTask();
            t.setDeviceId(deviceId);
            t.setEventId(ev.getId());
            t.setOffsetMinutes(30);
            t.setTriggerAt(taskTriggerAt);
            t.setScheduleId(s.getId());
            t.setStatus(PushTaskStatus.PENDING);
            t.setCreatedAt(now);
            taskList.add(t);
        }
        subs.saveAll(subList);
        tasks.saveAll(taskList);
        return new Round(s.getId());
    }

    private Map<String, Long> deliveryStats(long scheduleId) {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT l.result, COUNT(*) c FROM push_delivery_logs l JOIN push_tasks t ON l.task_id = t.id "
                        + "WHERE t.schedule_id = ? GROUP BY l.result", scheduleId)) {
            dist.put(row.get("result").toString(), ((Number) row.get("c")).longValue());
        }
        return dist;
    }

    private boolean allTasksDone(long scheduleId, PushTaskStatus status) {
        return tasks.countByScheduleIdAndStatusIn(scheduleId, List.of(status)) > 0
                && tasks.countByScheduleIdAndStatusIn(scheduleId, List.of(
                PushTaskStatus.PENDING, PushTaskStatus.PROCESSING, PushTaskStatus.RETRY)) == 0;
    }

    private PushTaskStatus waitStatus(long scheduleId, Predicate<PushTaskStatus> done, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        PushTaskStatus st = null;
        while (System.currentTimeMillis() < deadline) {
            st = schedules.findById(scheduleId).orElseThrow().getStatus();
            if (done.test(st)) return st;
            Thread.sleep(50);
        }
        return st;
    }

    private boolean waitAttempt(long scheduleId, long attemptId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            PushSchedule s = schedules.findById(scheduleId).orElseThrow();
            if (s.getStatus() == PushTaskStatus.PROCESSING && s.getAttemptId() != null
                    && s.getAttemptId() == attemptId) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    private int waitSenderIdle(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int busy;
        while ((busy = busySenders()) > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        return busy;
    }

    private int busySenders() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        int busy = 0;
        for (ThreadInfo ti : bean.dumpAllThreads(false, false)) {
            if (!ti.getThreadName().startsWith("webpush-sender")) continue;
            Thread.State st = ti.getThreadState();
            if (st != Thread.State.WAITING && st != Thread.State.TIMED_WAITING && st != Thread.State.TERMINATED) {
                busy++;
            }
        }
        return busy;
    }

    private record Round(long scheduleId) {
    }

    private static String ecRawKey() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            byte[] raw = ((ECPublicKey) kp.getPublic()).getQ().getEncoded(false);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("EC 密钥生成失败", e);
        }
    }

    private static String authKey() {
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(auth);
    }
}
