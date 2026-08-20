package com.example.bim.api;

import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.PushDeliveryLog;
import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushSubscription;
import com.example.bim.api.entity.PushTask;
import com.example.bim.api.entity.PushTaskStatus;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.PushScheduleRepository;
import com.example.bim.api.repository.PushSubscriptionRepository;
import com.example.bim.api.repository.PushTaskRepository;
import com.example.bim.api.schedule.PushTaskScheduler;
import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fan-out 万人级压测（真实链路，仅替换 Push Service 为本地假服务）：
 * - 假 Push Service：JDK HttpServer，单请求 50ms 延迟，1% endpoint 返回 410（验证失效清理）
 * - 每档：独立 Event + PushSchedule + N 条 PushTask + N 条 PushSubscription，同一 triggerAt 齐射
 * - 完整链路：Quartz 精确触发 → PushSendJob → processSchedule → 分批 sendBatch（8 并发）→ saveAll → finishSchedule
 * - 指标：投递成功率、总耗时（首条→末条）、P50/P95/P99、任务终态、假服务峰值并发、JVM 内存
 * 运行：mvnw.cmd test -Dtest=PushLoadTest（H2 内存库，不影响开发数据）
 * 报告：target/load-test-report.txt
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.datasource.url=jdbc:h2:mem:loadtest;DB_CLOSE_DELAY=-1"})
class PushLoadTest {

    private static final int FAKE_PORT = 19099;
    /** 假 Push Service 单请求延迟（模拟真实 Push Service 网络耗时） */
    private static final long FAKE_DELAY_MS = 50L;
    /** 档位规模（用户要求的压测序列） */
    private static final int[] SIZES = {100, 500, 1000, 3000, 5000, 8000};
    /** 档间间隔：插入 + 发送（8000 × 50ms / 8 并发 ≈ 50s）+ 余量 */
    private static final long STEP_MS = 75_000L;

    @Autowired PushScheduleRepository schedules;
    @Autowired PushTaskRepository tasks;
    @Autowired PushSubscriptionRepository subs;
    @Autowired EventRepository events;
    @Autowired JdbcTemplate jdbc;
    @Autowired PushTaskScheduler quartz;

    private HttpServer fakePush;
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicInteger inflight = new AtomicInteger();
    private final AtomicInteger maxConcurrent = new AtomicInteger();
    private final StringBuilder report = new StringBuilder();

    @BeforeEach
    void startFakePush() throws IOException {
        fakePush = HttpServer.create(new InetSocketAddress("127.0.0.1", FAKE_PORT), 0);
        fakePush.createContext("/push/", exchange -> {
            totalRequests.incrementAndGet();
            int cur = inflight.incrementAndGet();
            maxConcurrent.accumulateAndGet(cur, Math::max);
            try {
                Thread.sleep(FAKE_DELAY_MS);
            } catch (InterruptedException ignored) {
            }
            URI uri = exchange.getRequestURI();
            int code = uri.getPath().contains("/exp/") ? 410 : 201;
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
            inflight.decrementAndGet();
        });
        fakePush.setExecutor(Executors.newFixedThreadPool(16));
        fakePush.start();
    }

    @AfterEach
    void stopFakePush() {
        fakePush.stop(0);
    }

    @Test
    void fanoutLoad() throws Exception {
        long base = System.currentTimeMillis() + 15_000;
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            long triggerAt = base + i * STEP_MS;
            long roundStart = System.currentTimeMillis();
            PushSchedule schedule = prepareRound(size, triggerAt);
            report.append("===== 档位 %d：%d 用户 | 调度 #%d | triggerAt=%s =====\n"
                    .formatted(i + 1, size, schedule.getId(), fmt.format(new Date(triggerAt))));
            // 等 Quartz 触发 + Fan-out 完成（调度 SUCCESS / FAILED；RETRY 会退避补发，继续等）
            long deadline = triggerAt + 300_000;
            PushTaskStatus st = null;
            while (System.currentTimeMillis() < deadline) {
                st = schedules.findById(schedule.getId()).orElseThrow().getStatus();
                if (st == PushTaskStatus.SUCCESS || st == PushTaskStatus.FAILED) break;
                Thread.sleep(1_000);
            }
            assertTrue(st == PushTaskStatus.SUCCESS || st == PushTaskStatus.FAILED,
                    "调度未在超时内完成：status=" + st);
            appendRoundReport(schedule, triggerAt, fmt);
            long used = System.currentTimeMillis() - roundStart;
            report.append(String.format("本档实测耗时：%.1fs（含数据准备）%n%n", used / 1000.0));
        }
        appendJvmReport();
        String text = report.toString();
        System.out.println(text);
        Path out = Paths.get("target", "load-test-report.txt");
        Files.writeString(out, text, StandardCharsets.UTF_8);
        System.out.println("[load] 报告已写入 " + out.toAbsolutePath());
    }

    /** 准备一档：Event + Schedule（注册 Quartz）+ N 订阅 + N 任务（1% 过期订阅） */
    private PushSchedule prepareRound(int size, long triggerAt) {
        long now = System.currentTimeMillis();
        Event ev = new Event();
        ev.setId("load-" + size);
        ev.setArtist("load-test");
        ev.setDate("2026-08-20");
        ev.setTime("18:00");
        ev.setTimezone("KST");
        ev.setStartAtUtc(triggerAt + 30 * 60_000L);
        ev.setTitleEn("Load Test " + size);
        ev.setTitleZh("压测 " + size);
        ev.setTitleKo("로드 " + size);
        ev.setType("music-show");
        ev.setStatus("CONFIRMED");
        ev.setOfficial(true);
        events.save(ev);

        PushSchedule s = new PushSchedule();
        s.setEventId(ev.getId());
        s.setOffsetMinutes(30);
        s.setTriggerAt(triggerAt);
        s.setStatus(PushTaskStatus.PENDING);
        s.setCreatedAt(now);
        schedules.save(s);
        quartz.schedule(s); // 注册 Quartz Job，到点精确触发（与生产一致）

        List<PushSubscription> subList = new ArrayList<>(size);
        List<PushTask> taskList = new ArrayList<>(size);
        for (int k = 0; k < size; k++) {
            boolean expired = k % 100 == 0; // 1% 订阅模拟已失效
            // endpoint 带调度 id 保证全局唯一（push_subscriptions.endpoint 有 unique 约束）
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
            t.setTriggerAt(triggerAt);
            t.setScheduleId(s.getId());
            t.setStatus(PushTaskStatus.PENDING);
            t.setCreatedAt(now);
            taskList.add(t);
        }
        subs.saveAll(subList);
        tasks.saveAll(taskList);
        return s;
    }

    /** 按调度关联统计一档结果（join 任务表取 schedule_id，避免相邻档时间窗重叠污染） */
    private void appendRoundReport(PushSchedule schedule, long triggerAt, SimpleDateFormat fmt) {
        Map<String, Long> dist = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT l.result, COUNT(*) c FROM push_delivery_logs l JOIN push_tasks t ON l.task_id = t.id "
                        + "WHERE t.schedule_id = ? GROUP BY l.result", schedule.getId())) {
            dist.put(row.get("result").toString(), ((Number) row.get("c")).longValue());
        }
        long total = dist.values().stream().mapToLong(Long::longValue).sum();
        long success = dist.getOrDefault("SUCCESS", 0L);
        long expired = dist.getOrDefault("EXPIRED", 0L);
        long failed = dist.getOrDefault("FAILED", 0L);

        List<Long> times = jdbc.query(
                "SELECT l.sent_at FROM push_delivery_logs l JOIN push_tasks t ON l.task_id = t.id "
                        + "WHERE t.schedule_id = ? ORDER BY l.sent_at",
                (rs, i) -> rs.getLong(1), schedule.getId());

        Map<String, Long> tstat = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT status, COUNT(*) c FROM push_tasks WHERE schedule_id = ? GROUP BY status", schedule.getId())) {
            tstat.put(row.get("status").toString(), ((Number) row.get("c")).longValue());
        }

        report.append("投递日志：%d 条（SUCCESS=%d EXPIRED=%d FAILED=%d）成功率=%.2f%%%n"
                .formatted(total, success, expired, failed, total == 0 ? 0 : success * 100.0 / total));
        report.append("任务终态：%s%n".formatted(tstat));
        if (!times.isEmpty()) {
            long first = times.get(0);
            long last = times.get(times.size() - 1);
            report.append("总耗时：%.1fs（首条 %s → 末条 %s）%n"
                    .formatted((last - first) / 1000.0, fmt.format(new Date(first)), fmt.format(new Date(last))));
            List<Long> rel = new ArrayList<>(times.size());
            for (long t : times) rel.add(t - first);
            report.append("延迟分布（相对首条）：P50=%.1fs P95=%.1fs P99=%.1fs 均值=%.1fs%n"
                    .formatted(pct(rel, 0.50) / 1000.0, pct(rel, 0.95) / 1000.0,
                            pct(rel, 0.99) / 1000.0, rel.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0));
        } else {
            report.append("无投递日志（链路异常？）%n");
        }
        report.append("假 Push Service：总请求 %d，峰值并发 %d（线程池 8）%n"
                .formatted(totalRequests.get(), maxConcurrent.get()));
    }

    private void appendJvmReport() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        report.append("===== JVM 内存（测试进程）=====%n");
        report.append("堆已用：%.0fMB / 最大 %.0fMB%n".formatted(used / 1048576.0, rt.maxMemory() / 1048576.0));
    }

    private static double pct(List<Long> sorted, double p) {
        int idx = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
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
