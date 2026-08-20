package com.example.bim.api.dto;

import jakarta.validation.constraints.NotBlank;

/** 管理后台登录（POST /api/admin/login） */
public record AdminLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
