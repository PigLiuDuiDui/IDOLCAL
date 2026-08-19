package com.example.bim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 艺人档案 DTO。intro 为 { en, 'zh-CN', ko } 结构。
 * current = true 表示当前展示艺人（Hero / 日历订阅数据源）。
 */
public record ArtistDto(
        @NotBlank @Size(max = 20) String id,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String subName,
        @Pattern(regexp = "^\\d{4}$") String year,
        @NotBlank @Size(max = 50) String era,
        @Size(max = 200) String eraPeriod,
        @Size(max = 20) String accent,
        @Size(max = 20) String accentSoft,
        @Size(max = 1000) String heroImage,
        @Size(max = 200) String sourceTag,
        @Size(max = 5) Map<String, String> intro,
        boolean current
) {
}
