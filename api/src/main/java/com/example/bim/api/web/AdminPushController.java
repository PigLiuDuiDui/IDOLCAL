package com.example.bim.api.web;

import com.example.bim.api.Exception.NotFoundException;
import com.example.bim.api.auth.AdminOnly;
import com.example.bim.api.service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台推送查询（只读）：
 * GET /api/admin/push/schedules       调度分页（含任务聚合：target / success / failed / progress）
 * GET /api/admin/push/schedules/{id}  调度详情（事件信息 + 状态分布 + 投递时间线样本）
 * GET /api/admin/push/tasks           设备任务分页（scheduleId / status 筛选）
 * GET /api/admin/push/deliveries      投递日志分页（result / 设备 / endpoint 筛选）
 */
@RestController
@RequestMapping("/api/admin/push")
public class AdminPushController {

    private final AdminService admin;

    public AdminPushController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/schedules")
    @AdminOnly
    public Object schedules(@RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return admin.schedules(status, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "triggerAt")));
    }

    @GetMapping("/schedules/{id}")
    @AdminOnly
    public Map<String, Object> scheduleDetail(@PathVariable Long id) {
        Map<String, Object> detail = admin.scheduleDetail(id);
        if (detail == null) throw new NotFoundException("Push schedule not found: " + id);
        return detail;
    }

    @GetMapping("/tasks")
    @AdminOnly
    public Object tasks(@RequestParam(required = false) Long scheduleId,
                        @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
        return admin.tasks(scheduleId, status,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/deliveries")
    @AdminOnly
    public Object deliveries(@RequestParam(required = false) String result,
                             @RequestParam(required = false) String q,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return admin.deliveries(result, q,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "sentAt")));
    }
}
