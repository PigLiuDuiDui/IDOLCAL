package com.example.bim.api.web;

/** 资源状态冲突（如推送订阅 endpoint 已属于其他设备时拒绝覆盖）：409 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
