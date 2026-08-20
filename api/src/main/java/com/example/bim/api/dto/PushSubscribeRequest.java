package com.example.bim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 浏览器 Push Subscription 上报（POST /api/push/subscribe） */
public record PushSubscribeRequest(
        @NotBlank @Size(max = 64) String deviceId,
        @NotBlank @Size(max = 500) String endpoint,
        @NotBlank @Size(max = 128) String p256dh,
        @NotBlank @Size(max = 64) String auth
) {
}
