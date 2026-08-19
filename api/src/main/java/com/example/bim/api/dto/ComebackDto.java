package com.example.bim.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 回归专题 DTO。tagline 为 { en, 'zh-CN', ko } 结构；
 * stages 按时间线顺序排列，eventIds 引用 events 表的活动 id。
 */
public record ComebackDto(
        @NotBlank @Size(max = 20) String id,
        @NotBlank @Size(max = 20) String artistId,
        @NotBlank @Size(max = 200) String title,
        @NotEmpty @Size(max = 5) Map<String, String> tagline,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String releaseDate,
        @Pattern(regexp = "^\\d{2}:\\d{2}$") String releaseTime,
        @Size(max = 32) String releaseTimezone,
        List<@Valid Stage> stages
) {

    public record Stage(
            @NotBlank @Size(max = 20) String stage,
            List<@Size(max = 20) String> eventIds
    ) {
    }
}
