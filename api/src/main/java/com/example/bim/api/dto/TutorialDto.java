package com.example.bim.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * 教程板块 DTO。sections / notes / noCount 为嵌套 JSON 数组（JsonNode 透传，
 * 与前端 src/data/tutorials.js 结构一致）；null 时返回 JSON 空数组。
 */
public record TutorialDto(
        @NotBlank @Size(max = 20) String id,
        @NotBlank @Pattern(regexp = "^(ready|coming)$") String status,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String tagline,
        @Size(max = 100_000) JsonNode sections,
        @Size(max = 100_000) JsonNode notes,
        @Size(max = 100_000) JsonNode noCount
) {
}
