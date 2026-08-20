package com.example.bim.api;

import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushSubscription;
import com.example.bim.api.entity.PushTask;
import com.example.bim.api.Enum.PushTaskStatus;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.PushScheduleRepository;
import com.example.bim.api.repository.PushSubscriptionRepository;
import com.example.bim.api.repository.PushTaskRepository;
import com.example.bim.api.schedule.PushTaskScheduler;
import com.example.bim.api.service.PushTaskService;
import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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
import java.util.concurrent.CyclicBarrier;
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
 * 故障注入测试（真实链路，仅替换 Push Service 为本地可控假服务）：
 * <p>
 * 1. httpStallRecovery —— Push Service 全部请求卡死 30s+（模拟上游挂起）：
 * Future.get 超时 → cancel(true) → socket 超时兜底 → 线程最终释放 → 重试批次继续；
 * 验证无线程永久卡住 / 无任务卡 PROCESSING / 线程池耗尽后恢复 / JVM 内存不持续上涨。
 * <p>
 * 2. attemptPreemption —— 发送中途强制调度重置（模拟 recoverStale 超时恢复）：
 * Worker B 重新认领 attempt=2 并完成，Worker A 迟到结果必须被 attemptId + status 防护丢弃，
 * 不得覆盖新尝试的调度 / 任务状态（防状态倒灌）。
 * <p>
 * 3. concurrentClaim —— 多执行器同时 CAS 认领同一调度：
 * 仅一个成功（attempt=1），其余 affected rows = 0 放弃，绝不出现双套 Worker 同时发送。
 * <p>
 * 运行：mvnw.cmd test -Dtest=FaultInjectionTest（H2 内存库，不影响开发数据）
 * 报告：target/fault-test-report.txt
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.datasource.url=jdbc:h2:mem:fault;DB_CLOSE_DELAY=-1"})
class FaultInjectionTest {

    private static final int[] FAKE_PORTS = {19101, 19102, 19103}; // 每测试独立端口：连接池按 route 隔离，避免复用上一测试的 dead 连接
    /** 卡死时长：大于客户端 socket 超时（30s），保证「30s 不返回」语义且线程由超时兜底释放 */
    private static final long STALL_MS = 35_000L;
    /** 正常模式单请求延迟（模拟 Push Service 网络耗时） */
    private static final long NORMAL_DELAY_MS = 50L;

    @Autowired PushScheduleRepository schedules;
    @Autowired PushTaskRepository tasks;
    @Autowired PushSubscriptionRepository subs;
    @Autowired EventRepository events;
    @Autowired JdbcTemplate jdbc;
    @Autowired PushTaskScheduler quartz;
    @Autowired PushTaskService pushTaskService;

    private HttpServer fakePush;
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicInteger inflight = new AtomicInteger();
    private final AtomicInteger maxConcurrent = new AtomicInteger();
    /** 命中卡死的请求数（验证卡死窗口精确命中预期的请求） */
    private final AtomicLong stallHits = new AtomicLong();
    /** 剩余卡死配额：>0 时前 N 个请求卡死，其余正常（测试 B 用） */
    private final AtomicInteger stallBudget = new AtomicInteger();
    /** 全部请求卡死（测试 A 用） */
    private volatile boolean stallAll = false;

    private final StringBuilder report = new StringBuilder();

    /** 当前测试的假 Push Service 端口（@BeforeEach 按测试方法分配，端口隔离） */
    private int fakePort;

    @BeforeEach
    void startFakePush(TestInfo info) throws IOException {
        fakePort = FAKE_PORTS[portIndex(info.getTestMethod().orElseThrow().getName())];
        // 重置计数与卡死开关（每个测试独立，避免跨测试累计污染断言）
        totalRequests.set(0);
        stallHits.set(0);
        inflight.set(0);
        maxConcurrent.set(0);
        stallBudget.set(0);
        stallAll = false;
        fakePush = HttpServer.create(new InetSocketAddress("127.0.0.1", fakePort), 0);
        fakePush.createContext("/push/", exchange -> {
            totalRequests.incrementAndGet();
            int cur = inflight.incrementAndGet();
            maxConcurrent.accumulateAndGet(cur, Math::max);
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
                // 客户端已超时断开，写入失败可忽略
            }
            exchange.close();
            inflight.decrementAndGet();
        });
        fakePush.setExecutor(Executors.newFixedThreadPool(32));
        fakePush.start();
    }

    @AfterEach
    void stopFakePush() {
        fakePush.stop(0);
    }

    /** 测试方法 → 端口索引（测试 A/B/C 各占一个端口，连接池 route 隔离） */
    private static int portIndex(String method) {
        return switch (method) {
            case "httpStallRecovery" -> 0;
            case "attemptPreemption" -> 1;
            case "concurrentClaim" -> 2;
            default -> throw new IllegalStateException("未知测试方法: " + method);
        };
    }

    /** 是否卡死本请求：stallAll 全卡，否则消耗 stallBudget 配额 */
    private boolean stall() {
        if (stallAll) return true;
        int b = stallBudget.get();
        while (b > 0) {
            if (stallBudget.compareAndSet(b, b - 1)) return true;
            b = stallBudget.get();
        }
        return false;
    }

    // ==================== 测试 A：30s HTTP 卡死恢复 ====================

    /**
     * Push Service 全部请求卡死 30s+：16 用户 = 2 轮 × 8 线程全卡。
     * 预期：全 FAILED → 任务 RETRY → 调度 RETRY；线程最终释放（socket 超时兜底）；
     * server 恢复 + 重试到期后整轮补发成功，内存不持续上涨。
     */
    @Test
    void httpStallRecovery() throws Exception {
        report.append("===== 测试 A：30s HTTP 卡死（16 用户，全部请求卡死）=====\n");
        stallAll = true;
        long now = System.currentTimeMillis();
        long triggerAt = now + 10_000;
        Round r = prepareRound("fault-a", 16, triggerAt, triggerAt, true); // 注册 Quartz，真实触发
        report.append("调度 #%d triggerAt=%s\n".formatted(r.scheduleId(), fmt(triggerAt)));

        // 阶段 1：等待 Quartz 触发 + Fan-out 完成（16 任务全失败 → 调度 RETRY）
        PushTaskStatus st = waitStatus(r.scheduleId(), s -> s != PushTaskStatus.PENDING
                && s != PushTaskStatus.PROCESSING, 120_000);
        assertEquals(PushTaskStatus.RETRY, st, "卡死后任务全失败，调度应进入 RETRY");
        PushSchedule s1 = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(1L, s1.getAttemptId(), "首次执行 attempt 应为 1");
        assertTrue(stallHits.get() >= 16, "16 个请求应全部命中卡死，实际 " + stallHits.get());

        Map<String, Long> logs1 = deliveryStats(r.scheduleId());
        Map<String, Long> tstat1 = taskStats(r.scheduleId());
        assertEquals(16L, logs1.getOrDefault("FAILED", 0L), "首次投递应全 FAILED：" + logs1);
        assertEquals(16L, tstat1.getOrDefault("RETRY", 0L), "任务应全 RETRY：" + tstat1);
        assertNull(ManagementFactory.getThreadMXBean().findDeadlockedThreads(), "不得存在死锁");
        int busy1 = waitSenderIdle(10_000);
        assertEquals(0, busy1, "卡死结束后发送线程必须全部释放，残留 " + busy1 + " 个忙碌线程");
        long heapStuck = heapUsed();
        report.append("阶段1：16 任务全 FAILED → RETRY，调度 RETRY；线程全部释放；堆 %.0fMB\n".formatted(heapStuck / 1048576.0));

        // 阶段 2：server 恢复 + 重试时间提前 → retryDue 补发（验证「下一批继续」）
        stallAll = false;
        long t2 = System.currentTimeMillis();
        jdbc.update("UPDATE push_schedules SET next_retry_at = ? WHERE id = ?", t2 - 1_000, r.scheduleId());
        jdbc.update("UPDATE push_tasks SET next_retry_at = ? WHERE schedule_id = ?", t2 - 1_000, r.scheduleId());
        pushTaskService.retryDue();

        PushTaskStatus st2 = waitStatus(r.scheduleId(), s -> s == PushTaskStatus.SUCCESS
                || s == PushTaskStatus.FAILED, 60_000);
        assertEquals(PushTaskStatus.SUCCESS, st2, "重试后调度应 SUCCESS");
        PushSchedule s2 = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(2L, s2.getAttemptId(), "重试轮 attempt 应为 2");
        Map<String, Long> logs2 = deliveryStats(r.scheduleId());
        Map<String, Long> tstat2 = taskStats(r.scheduleId());
        // 重试轮：15 个正常订阅 SUCCESS + 1 个失效订阅（k=0，/exp/）410 → EXPIRED，全部完成
        assertEquals(16L, logs2.getOrDefault("SUCCESS", 0L) + logs2.getOrDefault("EXPIRED", 0L),
                "重试应全部完成（SUCCESS + EXPIRED = 16）：" + logs2);
        assertEquals(1L, logs2.getOrDefault("EXPIRED", 0L), "1% 失效订阅应 EXPIRED：" + logs2);
        assertEquals(16L, tstat2.getOrDefault("SUCCESS", 0L), "任务应全 SUCCESS（EXPIRED 也算完成）：" + tstat2);
        assertEquals(0, waitSenderIdle(10_000), "重试后线程必须全部释放");
        long heapDone = heapUsed();
        long heapGrowth = heapDone - heapStuck;
        report.append("阶段2：重试 16 条全 SUCCESS，调度 SUCCESS；线程全部释放；堆 %.0fMB（较卡死时 %+.0fMB）\n"
                .formatted(heapDone / 1048576.0, heapGrowth / 1048576.0));
        assertTrue(heapGrowth < 300L * 1048576, "堆内存不应持续上涨，增长 " + heapGrowth / 1048576 + "MB");
        appendServerStats();
        finishReport("httpStallRecovery");
    }

    // ==================== 测试 B：attempt 抢占（防状态倒灌） ====================

    /**
     * Worker A 发送中（8 请求卡死占满线程池）→ 调度被强制重置 PENDING（模拟超时恢复）
     * → Worker B 认领 attempt=2 并成功完成 → A 迟到结果必须被 attemptId + status 防护丢弃。
     */
    @Test
    void attemptPreemption() throws Exception {
        report.append("===== 测试 B：attempt 抢占（8 用户，A 卡死中重置 → B 抢占）=====\n");
        stallBudget.set(8); // 前 8 个请求卡死（A 的 8 个单元），B 的请求正常
        long now = System.currentTimeMillis();
        long triggerAt = now + 10_000;
        Round r = prepareRound("fault-b", 8, triggerAt, triggerAt, false); // 不注册 Quartz，手动扮演 Worker
        report.append("调度 #%d triggerAt=%s\n".formatted(r.scheduleId(), fmt(triggerAt)));

        AtomicReference<Throwable> workerError = new AtomicReference<>();
        Thread workerA = new Thread(() -> {
            try {
                pushTaskService.processSchedule(r.scheduleId());
            } catch (Throwable t) {
                workerError.set(t);
            }
        }, "worker-A");
        workerA.start();

        // 等 A 认领成功：PROCESSING && attempt=1
        assertTrue(waitAttempt(r.scheduleId(), 1, 10_000), "Worker A 应在 10s 内认领（attempt=1）");
        report.append("Worker A 已认领 attempt=1（PROCESSING），发送中卡死\n");

        // 模拟超时恢复：CAS PROCESSING → PENDING（recoverStale 的等价操作，重置不立即重发）
        // @Modifying 仓储方法需事务，这里直接 SQL CAS 更新（与 transition/resetStatus 语义一致）
        int reset = jdbc.update("UPDATE push_schedules SET status = ? WHERE id = ? AND status = ?",
                PushTaskStatus.PENDING.name(), r.scheduleId(), PushTaskStatus.PROCESSING.name());
        assertEquals(1, reset, "超时重置应成功（A 的 PROCESSING 被重置为 PENDING）");
        assertEquals(PushTaskStatus.PENDING, schedules.findById(r.scheduleId()).orElseThrow().getStatus());

        // 生产时序：recoverStale 阈值（>30s）远大于单条发送超时，重置时 A 的线程早已被
        // socket 超时兜底释放；这里同样等 A 的线程释放后再让 B 接管，否则 B 的请求排队
        // 30s 撞上 Future.get 超时窗口，批首任务可能被取消（测试失去确定性）
        assertEquals(0, waitSenderIdle(60_000), "A 的发送线程应在 60s 内全部释放（socket 超时兜底）");

        // Worker B 认领（attempt=2）：线程池已空闲，请求不排队，直接发送并成功
        pushTaskService.processSchedule(r.scheduleId());

        PushTaskStatus st = waitStatus(r.scheduleId(), s -> s == PushTaskStatus.SUCCESS
                || s == PushTaskStatus.FAILED, 60_000);
        assertEquals(PushTaskStatus.SUCCESS, st, "B 完成后调度应 SUCCESS");
        PushSchedule fin = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(2L, fin.getAttemptId(), "最终 attempt 必须是 2（新尝试接管）");
        assertEquals(0L, fin.getRetryCount(), "B 全量成功，调度不应进入重试");

        // 等 A 的迟到结果返回并验证被丢弃
        workerA.join(60_000);
        assertFalse(workerA.isAlive(), "Worker A 线程必须退出（socket 超时兜底）");
        assertNull(workerError.get(), "Worker A 不应抛异常：" + workerError.get());
        PushSchedule after = schedules.findById(r.scheduleId()).orElseThrow();
        assertEquals(2L, after.getAttemptId(), "A 的迟到收尾不得倒灌 attempt（仍是 2）");
        assertEquals(PushTaskStatus.SUCCESS, after.getStatus(), "A 的迟到收尾不得覆盖状态（仍是 SUCCESS）");

        Map<String, Long> logs = deliveryStats(r.scheduleId());
        Map<String, Long> tstat = taskStats(r.scheduleId());
        assertEquals(8L, logs.getOrDefault("FAILED", 0L), "A 的 8 次投递应为 FAILED（事实记录）：" + logs);
        // B 重发：7 个正常订阅 SUCCESS + 1 个失效订阅（k=0，/exp/）410 → EXPIRED，全部完成
        assertEquals(8L, logs.getOrDefault("SUCCESS", 0L) + logs.getOrDefault("EXPIRED", 0L),
                "B 的 8 次投递应全部完成（SUCCESS + EXPIRED = 8）：" + logs);
        assertEquals(16L, logs.values().stream().mapToLong(Long::longValue).sum(), "共 16 条日志");
        assertEquals(8L, tstat.getOrDefault("SUCCESS", 0L), "任务状态归 B（全 SUCCESS）：" + tstat);
        assertEquals(8L, stallHits.get(), "恰好 8 个请求命中卡死（A），B 的请求必须正常");
        assertEquals(0, waitSenderIdle(10_000), "所有发送线程必须释放");
        assertNull(ManagementFactory.getThreadMXBean().findDeadlockedThreads(), "不得存在死锁");
        report.append("验证通过：attempt=1 迟到结果被丢弃（attempt=2/SUCCESS 未被覆盖），任务状态归 B\n");
        appendServerStats();
        finishReport("attemptPreemption");
    }

    // ==================== 测试 C：重复 Claim ====================

    /**
     * 多执行器同时 CAS 认领：仅一个成功（attempt=1），其余 affected=0 放弃；
     * 投递日志恰好等于任务数（绝无双套 Worker 同时发送）。3 轮提高置信度。
     */
    @Test
    void concurrentClaim() throws Exception {
        report.append("===== 测试 C：重复 Claim（8 用户 × 4 并发认领 × 3 轮）=====\n");
        for (int i = 0; i < 3; i++) {
            long now = System.currentTimeMillis();
            // 调度 triggerAt +1h（防兜底扫描 recoverPending 抢跑）；任务 triggerAt 已过期（可投递）
            Round r = prepareRound("fault-c" + i, 8, now + 3_600_000, now - 1_000, false);
            int claimers = 4;
            CyclicBarrier barrier = new CyclicBarrier(claimers);
            List<Thread> ts = new ArrayList<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            for (int j = 0; j < claimers; j++) {
                Thread t = new Thread(() -> {
                    try {
                        barrier.await();
                        pushTaskService.processSchedule(r.scheduleId());
                    } catch (Throwable e) {
                        error.set(e);
                    }
                }, "claimer-" + i + "-" + j);
                ts.add(t);
                t.start();
            }
            for (Thread t : ts) t.join(30_000);
            assertNull(error.get(), "认领线程异常：" + error.get());

            PushSchedule s = schedules.findById(r.scheduleId()).orElseThrow();
            assertEquals(PushTaskStatus.SUCCESS, s.getStatus(), "轮 " + i + " 调度应 SUCCESS");
            assertEquals(1L, s.getAttemptId(), "轮 " + i + " 并发认领只允许一个 attempt");
            Map<String, Long> logs = deliveryStats(r.scheduleId());
            Map<String, Long> tstat = taskStats(r.scheduleId());
            assertEquals(8L, tstat.getOrDefault("SUCCESS", 0L), "轮 " + i + " 任务应全 SUCCESS：" + tstat);
            // 7 正常 + 1 失效订阅（/exp/ → 410 EXPIRED），总日志必须恰 8 条（无双发）
            assertEquals(8L, logs.values().stream().mapToLong(Long::longValue).sum(),
                    "轮 " + i + " 投递日志必须恰 8 条（无双发）：" + logs);
            assertEquals(0, waitSenderIdle(10_000), "轮 " + i + " 线程必须释放");
            report.append("轮 %d：4 并发认领 → attempt=%d，投递日志 %d 条（无双发）\n"
                    .formatted(i, s.getAttemptId(), logs.values().stream().mapToLong(Long::longValue).sum()));
        }
        assertNull(ManagementFactory.getThreadMXBean().findDeadlockedThreads(), "不得存在死锁");
        appendServerStats();
        finishReport("concurrentClaim");
    }

    // ==================== 辅助 ====================

    /** 准备一档：Event + Schedule（可选注册 Quartz）+ N 订阅 + N 任务（1% 过期订阅） */
    private Round prepareRound(String eventId, int size, long scheduleTriggerAt, long taskTriggerAt, boolean registerQuartz) {
        long now = System.currentTimeMillis();
        Event ev = new Event();
        ev.setId(eventId);
        ev.setArtist("fault-test");
        ev.setDate("2026-08-20");
        ev.setTime("18:00");
        ev.setTimezone("KST");
        ev.setStartAtUtc(scheduleTriggerAt + 30 * 60_000L);
        ev.setTitleEn("Fault " + eventId);
        ev.setTitleZh("故障 " + eventId);
        ev.setTitleKo("장애 " + eventId);
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
        if (registerQuartz) quartz.schedule(s); // 注册 Quartz Job，到点真实触发

        List<PushSubscription> subList = new ArrayList<>(size);
        List<PushTask> taskList = new ArrayList<>(size);
        for (int k = 0; k < size; k++) {
            boolean expired = k % 100 == 0; // 1% 订阅模拟已失效
            String deviceId = String.format("%s-%d-%06d", expired ? "exp" : "dev", s.getId(), k);
            PushSubscription sub = new PushSubscription();
            sub.setDeviceId(deviceId);
            sub.setEndpoint("http://127.0.0.1:" + fakePort + "/push/" + s.getId() + "/" + (expired ? "exp/" : "dev/") + k);
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

    /** 按调度关联统计投递日志（join 任务表，避免跨档污染） */
    private Map<String, Long> deliveryStats(long scheduleId) {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT l.result, COUNT(*) c FROM push_delivery_logs l JOIN push_tasks t ON l.task_id = t.id "
                        + "WHERE t.schedule_id = ? GROUP BY l.result", scheduleId)) {
            dist.put(row.get("result").toString(), ((Number) row.get("c")).longValue());
        }
        return dist;
    }

    /** 按调度统计任务状态分布 */
    private Map<String, Long> taskStats(long scheduleId) {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT status, COUNT(*) c FROM push_tasks WHERE schedule_id = ? GROUP BY status", scheduleId)) {
            dist.put(row.get("status").toString(), ((Number) row.get("c")).longValue());
        }
        return dist;
    }

    /** 轮询直到状态满足条件，返回最终状态（超时返回最后观测值） */
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

    /** 轮询直到调度处于 PROCESSING 且 attemptId 匹配 */
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

    /** 等待所有 webpush-sender 线程空闲（非 WAITING/TIMED_WAITING 计数为 0），返回残留忙碌数 */
    private int waitSenderIdle(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int busy;
        while ((busy = busySenders()) > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        return busy;
    }

    /** 当前处于忙碌状态（非空闲等待）的发送线程数 */
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

    private long heapUsed() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private void appendServerStats() {
        report.append("假 Push Service：总请求 %d，卡死命中 %d，峰值并发 %d\n"
                .formatted(totalRequests.get(), stallHits.get(), maxConcurrent.get()));
    }

    private void finishReport(String test) throws IOException {
        String text = report.toString();
        System.out.println(text);
        Path out = Paths.get("target", "fault-test-report.txt");
        Files.writeString(out, text, StandardCharsets.UTF_8);
        System.out.println("[fault] " + test + " 报告已写入 " + out.toAbsolutePath());
    }

    private static String fmt(long ts) {
        return new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date(ts));
    }

    private record Round(long scheduleId) {
    }

    /** 生成 raw 65 字节 uncompressed EC 公钥（web-push 库 ECPublicKeySpec 格式），base64url 无 padding */
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

    /** auth：16 字节随机（base64url 无 padding） */
    private static String authKey() {
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(auth);
    }
}
