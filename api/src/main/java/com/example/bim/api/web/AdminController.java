package com.example.bim.api.web;

import com.example.bim.api.auth.AuthService;
import com.example.bim.api.dto.AdminLoginRequest;
import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushTaskStatus;
import com.example.bim.api.repository.PushDeliveryLogRepository;
import com.example.bim.api.repository.PushScheduleRepository;
import com.example.bim.api.repository.PushTaskRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口（需 Bearer Token，由 AuthInterceptor 保护）：
 * POST /api/admin/login               登录（返回 JWT；连续失败锁定 + 限流）
 * GET  /api/admin/push/stats          推送投递统计（今日发送 / 成功 / 失败 / 失效）
 * GET  /api/admin/push/tasks/stats    调度 + 任务双状态分布（PENDING / PROCESSING / SUCCESS / FAILED / RETRY）
 * GET  /api/admin/push/upcoming       未来 24h 即将触发的调度（每条附 Fan-out 收件人数）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final long UPCOMING_WINDOW_MS = 24 * 3_600_000L;

    private final AuthService auth;
    private final PushDeliveryLogRepository deliveryLogs;
    private final PushScheduleRepository schedules;
    private final PushTaskRepository pushTasks;

    public AdminController(AuthService auth, PushDeliveryLogRepository deliveryLogs,
                           PushScheduleRepository schedules, PushTaskRepository pushTasks) {
        this.auth = auth;
        this.deliveryLogs = deliveryLogs;
        this.schedules = schedules;
        this.pushTasks = pushTasks;
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody AdminLoginRequest req, HttpServletRequest http) {
        String token = auth.login(req.username(), req.password(), http.getRemoteAddr());
        return Map.of("token", token, "role", "ADMIN");
    }

    @GetMapping("/push/stats")
    public Map<String, Object> pushStats() {
        long dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
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
        return Map.of("today", today);
    }

    /** 调度 + 任务状态分布：调度反映 Fan-out 执行进度，任务反映每个设备的投递结果 */
    @GetMapping("/push/tasks/stats")
    public Map<String, Object> taskStats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("schedules", distribution(schedules.countByStatus()));
        out.put("tasks", distribution(pushTasks.countByStatus()));
        return out;
    }

    private Map<String, Object> distribution(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (PushTaskStatus s : PushTaskStatus.values()) {
            counts.put(s.name(), 0L);
        }
        for (Object[] row : rows) {
            counts.put(row[0].toString(), (Long) row[1]);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("byStatus", counts);
        return out;
    }

    /** 未来 24h 即将触发的调度：每条附 Fan-out 收件人数（可投递任务数），直接预览万人级推送规模 */
    @GetMapping("/push/upcoming")
    public List<Map<String, Object>> upcoming() {
        long now = System.currentTimeMillis();
        List<PushSchedule> due = schedules.findByStatusInAndTriggerAtBetween(
                List.of(PushTaskStatus.PENDING, PushTaskStatus.RETRY), now, now + UPCOMING_WINDOW_MS);
        List<Map<String, Object>> out = new ArrayList<>(due.size());
        for (PushSchedule s : due) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("scheduleId", s.getId());
            m.put("eventId", s.getEventId());
            m.put("offsetMinutes", s.getOffsetMinutes());
            m.put("triggerAt", s.getTriggerAt());
            m.put("recipients", pushTasks.countByScheduleIdAndStatusIn(s.getId(),
                    List.of(PushTaskStatus.PENDING, PushTaskStatus.RETRY)));
            out.add(m);
        }
        return out;
    }
}
