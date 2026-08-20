package com.example.bim.api.Exception;

/** 401 未认证 / 未授权（管理后台权限） */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
