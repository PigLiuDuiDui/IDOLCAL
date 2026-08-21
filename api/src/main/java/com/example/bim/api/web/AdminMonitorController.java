package com.example.bim.api.web;

import com.example.bim.api.auth.AdminOnly;
import com.example.bim.api.service.AdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台监控查询（只读 + 失效设备清理）：
 * GET    /api/admin/users            用户账号分页
 * GET    /api/admin/devices          设备分页（匿名用户锚点，含状态计算）
 * DELETE /api/admin/devices/{id}     删除单个设备（级联订阅 / 任务 / 孤儿调度）
 * POST   /api/admin/devices/clean-expired  批量清理 EXPIRED 设备
 * GET    /api/admin/subscriptions    推送订阅分页
 * GET    /api/admin/system           系统监控（JVM / 线程池 / 数据库 / Quartz / 配置）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminMonitorController {

    private final AdminService admin;

    public AdminMonitorController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    @AdminOnly
    public Object users(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
        return admin.users(q, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/devices")
    @AdminOnly
    public Object devices(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return admin.devices(q, status, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "lastActiveAt")));
    }

    @DeleteMapping("/devices/{deviceId}")
    @AdminOnly
    public Map<String, Object> deleteDevice(@PathVariable String deviceId) {
        return admin.deleteDevice(deviceId);
    }

    @PostMapping("/devices/clean-expired")
    @AdminOnly
    public Map<String, Object> cleanExpired() {
        return admin.cleanExpired();
    }

    @GetMapping("/subscriptions")
    @AdminOnly
    public Object subscriptions(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return admin.subscriptions(PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/system")
    @AdminOnly
    public Map<String, Object> system() {
        return admin.system();
    }
}
