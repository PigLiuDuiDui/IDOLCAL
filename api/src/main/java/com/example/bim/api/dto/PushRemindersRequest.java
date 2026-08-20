package com.example.bim.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提醒全量同步（PUT /api/push/reminders；空列表 = 清空该设备全部提醒）。
 * 触发时刻由后端计算：event.start_at_utc - offset_minutes（前端不再传 remindAt）。
 */
public record PushRemindersRequest(
        @NotBlank @Size(max = 64) String deviceId,
        @NotNull List<@Valid Item> reminders
) {
    public record Item(
            @NotBlank @Size(max = 32) String eventId,
            @NotNull @PositiveOrZero Integer offsetMinutes
    ) {
    }
}
