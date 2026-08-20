package com.example.bim.api.web;

import com.example.bim.api.ratelimit.RateLimitException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;

import java.util.LinkedHashMap;
import java.util.Map;

/** 全局异常 → 统一 JSON 响应 { error, code } */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /** 未认证 / 未授权（管理后台权限）：401 */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unauthorized(UnauthorizedException e) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    /** 限流拒绝：429 + Retry-After（剩余窗口秒数） */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> rateLimit(RateLimitException e, HttpServletResponse response) {
        response.setHeader("Retry-After", String.valueOf(e.retryAfterSeconds()));
        return build(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", e.getMessage());
    }

    /** Bean Validation 失败：400 + 首个字段错误 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("validation failed");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", msg);
    }

    /** JSON 解析失败（Jackson 3）：400 而非 500 */
    @ExceptionHandler(JacksonException.class)
    public ResponseEntity<Map<String, Object>> invalidJson(JacksonException e) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed request body");
    }

    /** 请求体不可读（非法 JSON / 类型不匹配）：400 而非 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed request body");
    }

    /** 未匹配的静态资源 / 未知路径 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noResource(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> internal(Exception e) {
        log.error("Unexpected error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
