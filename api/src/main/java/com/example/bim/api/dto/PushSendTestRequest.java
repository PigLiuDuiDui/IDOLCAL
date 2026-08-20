package com.example.bim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 发送测试通知（POST /api/push/send-test，仅需设备标识） */
public record PushSendTestRequest(
        @NotBlank @Size(max = 64) String deviceId
) {
}
