package com.example.bim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 退订（DELETE /api/push/subscribe） */
public record PushUnsubscribeRequest(
        @NotBlank @Size(max = 64) String deviceId,
        @NotBlank @Size(max = 500) String endpoint
) {
}
