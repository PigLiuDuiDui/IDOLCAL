package com.example.bim.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 活动 DTO。多语言字段为 { en, 'zh-CN', ko } 结构（与前端数据格式一致）。
 * 既是请求体也是响应体；id 可空（新增时由后端生成）。
 */
public record EventDto(
        @Size(max = 20) String id,
        @NotBlank @Size(max = 20) String artist,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String date,
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String endDate,
        @Pattern(regexp = "^\\d{2}:\\d{2}$") String time,
        @Size(max = 32) String timezone,
        @NotEmpty @Size(max = 5) Map<String, String> title,
        @NotBlank @Size(max = 20) String type,
        @NotBlank @Size(max = 20) String status,
        @Size(max = 5) Map<String, String> location,
        @Size(max = 5000) Map<String, String> description,
        @Size(max = 1000) String image,
        @Size(max = 200) String sourceName,
        @Size(max = 1000) String sourceUrl,
        @JsonProperty("isOfficial") boolean isOfficial,
        @Size(max = 1000) String onlineUrl,
        @Size(max = 1000) String mapUrl
) {
}
