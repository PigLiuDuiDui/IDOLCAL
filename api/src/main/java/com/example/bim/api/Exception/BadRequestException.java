package com.example.bim.api.Exception;

/** 请求数据不合法 / 冲突 → 400 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
