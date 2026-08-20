package com.example.bim.api.web;

import com.example.bim.api.dto.PushRemindersRequest;
import com.example.bim.api.dto.PushSendTestRequest;
import com.example.bim.api.dto.PushSubscribeRequest;
import com.example.bim.api.dto.PushUnsubscribeRequest;
import com.example.bim.api.service.DeviceAuthService;
import com.example.bim.api.service.PushTaskService;
import com.example.bim.api.service.WebPushService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Web Push 接口（无登录体系，deviceId 为浏览器匿名标识）：
 * 设备所有权：subscribe 成功后返回 HMAC 凭证，前端保存；
 * 其余写请求（退订 / 同步提醒 / 测试推送）必须携带 X-Device-Token 头，防止越权操作他人设备。
 * GET    /api/push/vapid-public-key   VAPID 公钥（订阅必需）
 * POST   /api/push/subscribe          保存订阅（同时注册设备记录）；endpoint 已属他人返回 409
 * DELETE /api/push/subscribe          退订（需设备凭证）
 * PUT    /api/push/reminders          全量同步该设备的提醒任务（需设备凭证）
 * POST   /api/push/send-test          发送测试通知（需设备凭证）
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WebPushService push;
    private final PushTaskService pushTaskService;
    private final DeviceAuthService deviceAuth;

    public PushController(WebPushService push, PushTaskService pushTaskService, DeviceAuthService deviceAuth) {
        this.push = push;
        this.pushTaskService = pushTaskService;
        this.deviceAuth = deviceAuth;
    }

    @GetMapping("/vapid-public-key")
    public Map<String, String> vapidPublicKey() {
        return Map.of("key", push.vapidPublicKey());
    }

    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@Valid @RequestBody PushSubscribeRequest req, HttpServletRequest http) {
        // 首次注册无需凭证（凭证本接口签发）；endpoint 已属其他设备时后端拒绝覆盖
        String credential = push.subscribe(req.deviceId(), req.endpoint(), req.p256dh(), req.auth(), http.getHeader("User-Agent"));
        return Map.of("ok", true, "credential", credential);
    }

    @DeleteMapping("/subscribe")
    public Map<String, Boolean> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest req,
                                            @RequestHeader(value = DeviceAuthService.TOKEN_HEADER, required = false) String token) {
        deviceAuth.requireDevice(token, req.deviceId());
        push.unsubscribe(req.deviceId(), req.endpoint());
        return Map.of("ok", true);
    }

    @PutMapping("/reminders")
    public Map<String, Boolean> syncReminders(@Valid @RequestBody PushRemindersRequest req,
                                              @RequestHeader(value = DeviceAuthService.TOKEN_HEADER, required = false) String token) {
        deviceAuth.requireDevice(token, req.deviceId());
        pushTaskService.syncReminders(req.deviceId(),
                req.reminders().stream()
                        .map(i -> new PushTaskService.ReminderItem(i.eventId(), i.offsetMinutes()))
                        .toList());
        return Map.of("ok", true);
    }

    @PostMapping("/send-test")
    public Map<String, Object> sendTest(@Valid @RequestBody PushSendTestRequest req,
                                        @RequestHeader(value = DeviceAuthService.TOKEN_HEADER, required = false) String token) {
        deviceAuth.requireDevice(token, req.deviceId());
        boolean sent = push.sendTest(req.deviceId());
        if (!sent) throw new BadRequestException("推送发送失败，请确认设备订阅有效");
        return Map.of("ok", true);
    }
}
