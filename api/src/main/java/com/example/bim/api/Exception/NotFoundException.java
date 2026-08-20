package com.example.bim.api.Exception;

/** 资源不存在 → 404 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
