package com.example.bim.api.service;

import com.example.bim.api.auth.JwtUtil;
import com.example.bim.api.web.UnauthorizedException;
import org.springframework.stereotype.Service;

/**
 * 推送设备所有权凭证（HMAC）：/api/push/* 无登录体系，
 * 若仅凭设备自报 ID 识别身份，知道他人 deviceId 即可篡改其提醒 / 劫持订阅。
 * 方案：subscribe 时后端为 deviceId 签发 HMAC 签名（复用 JWT 密钥），
 * 前端存 localStorage，后续写请求经 X-Device-Token 头携带，服务端恒定时间比较。
 * 密钥轮换 / 服务重启（随机密钥模式）后凭证失效 → 前端重新 subscribe 即可换发。
 */
@Service
public class DeviceAuthService {

    /** 请求头名：X-Device-Token: <deviceId>.<base64url(HMAC-SHA256(deviceId))> */
    public static final String TOKEN_HEADER = "X-Device-Token";

    private final JwtUtil jwt;

    public DeviceAuthService(JwtUtil jwt) {
        this.jwt = jwt;
    }

    /** 为设备签发所有权凭证（subscribe 响应返回，前端保存） */
    public String issueCredential(String deviceId) {
        return jwt.signData(deviceId);
    }

    /**
     * 校验请求头中的设备凭证，并要求与请求体 deviceId 一致。
     * 任一不满足抛 401；成功返回已校验的 deviceId。
     */
    public String requireDevice(String tokenHeader, String bodyDeviceId) {
        if (tokenHeader == null || tokenHeader.isBlank()) {
            throw new UnauthorizedException("Device credential required");
        }
        int dot = tokenHeader.indexOf('.');
        String deviceId = dot > 0 ? tokenHeader.substring(0, dot) : null;
        String signature = dot > 0 ? tokenHeader.substring(dot + 1) : null;
        if (deviceId == null || bodyDeviceId == null
                || !deviceId.equals(bodyDeviceId) || !jwt.verifyData(deviceId, signature)) {
            throw new UnauthorizedException("Invalid device credential");
        }
        return deviceId;
    }
}
