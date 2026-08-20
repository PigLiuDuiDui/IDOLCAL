package com.example.bim.api.web;

import com.example.bim.api.dto.PushRemindersRequest;
import com.example.bim.api.dto.PushSendTestRequest;
import com.example.bim.api.dto.PushSubscribeRequest;
import com.example.bim.api.dto.PushUnsubscribeRequest;
import com.example.bim.api.service.PushTaskService;
import com.example.bim.api.service.WebPushService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Web Push 接口（无登录体系，deviceId 为浏览器匿名标识）：
 * GET    /api/push/vapid-public-key   VAPID 公钥（订阅必需）
 * POST   /api/push/subscribe          保存订阅（同时注册设备记录）
 * DELETE /api/push/subscribe          退订
 * PUT    /api/push/reminders          全量同步该设备的提醒任务（后端计算触发时刻）
 * POST   /api/push/send-test          发送测试通知（开发调试）
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WebPushService push;
    private final PushTaskService pushTaskService;

    public PushController(WebPushService push, PushTaskService pushTaskService) {
        this.push = push;
        this.pushTaskService = pushTaskService;
    }

    @GetMapping("/vapid-public-key")
    public Map<String, String> vapidPublicKey() {
        return Map.of("key", push.vapidPublicKey());
    }

    @PostMapping("/subscribe")
    public Map<String, Boolean> subscribe(@Valid @RequestBody PushSubscribeRequest req, HttpServletRequest http) {
        push.subscribe(req.deviceId(), req.endpoint(), req.p256dh(), req.auth(), http.getHeader("User-Agent"));
        return Map.of("ok", true);
    }

    @DeleteMapping("/subscribe")
    public Map<String, Boolean> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest req) {
        push.unsubscribe(req.deviceId(), req.endpoint());
        return Map.of("ok", true);
    }

    @PutMapping("/reminders")
    public Map<String, Boolean> syncReminders(@Valid @RequestBody PushRemindersRequest req) {
        pushTaskService.syncReminders(req.deviceId(),
                req.reminders().stream()
                        .map(i -> new PushTaskService.ReminderItem(i.eventId(), i.offsetMinutes()))
                        .toList());
        return Map.of("ok", true);
    }

    @PostMapping("/send-test")
    public Map<String, Object> sendTest(@Valid @RequestBody PushSendTestRequest req) {
        boolean sent = push.sendTest(req.deviceId());
        if (!sent) throw new BadRequestException("推送发送失败，请确认设备订阅有效");
        return Map.of("ok", true);
    }
}
