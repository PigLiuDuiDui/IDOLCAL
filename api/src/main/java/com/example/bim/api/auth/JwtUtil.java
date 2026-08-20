package com.example.bim.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 JWT（HS256，HMAC-SHA256 + Base64URL，零第三方依赖）
 * 密钥未配置时启动生成随机密钥（重启后旧 token 失效）；
 * 生产必须设置 IDOLCAL_JWT_SECRET，并开启 strict 模式（未配置 / 强度不足直接拒绝启动）。
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HMAC_ALG = "HmacSHA256";
    /** 生产强制要求的最小密钥长度（32 字节 = 256 bit，HS256 安全下限） */
    private static final int MIN_SECRET_BYTES = 32;

    private final byte[] secret;
    private final long ttlMillis;

    public JwtUtil(@Value("${idolcal.auth.jwt-secret:}") String secret,
                   @Value("${idolcal.auth.jwt-ttl-hours:24}") long ttlHours,
                   @Value("${idolcal.auth.strict-jwt-secret:false}") boolean strict) {
        if (secret == null || secret.isBlank()) {
            if (strict) {
                // 生产强制：未配置高强度密钥直接拒绝启动，不允许隐式随机密钥
                throw new IllegalStateException("IDOLCAL_JWT_SECRET 未配置：生产环境必须设置高强度随机密钥（≥32 字节）");
            }
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.secret = random;
            log.warn("[auth] JWT 密钥未配置，已生成随机密钥（重启后登录态失效）；生产请设置 IDOLCAL_JWT_SECRET");
        } else {
            if (strict && secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("IDOLCAL_JWT_SECRET 强度不足：生产环境要求 ≥32 字节的随机密钥");
            }
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.ttlMillis = ttlHours * 3_600_000L;
    }

    /** 签发 token（username + role，默认 24h 有效） */
    public String createToken(String username, String role) {
        try {
            long now = System.currentTimeMillis();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", username);
            payload.put("role", role);
            payload.put("iat", now / 1000);
            payload.put("exp", (now + ttlMillis) / 1000);
            String signing = b64(JSON.writeValueAsBytes(header)) + "." + b64(JSON.writeValueAsBytes(payload));
            return signing + "." + b64(hmac(signing));
        } catch (Exception e) {
            throw new IllegalStateException("JWT sign failed", e);
        }
    }

    /** 校验并解析；签名错误 / 过期 / 格式非法 / 关键字段缺失返回 null */
    public Map<String, String> verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String expected = b64(hmac(parts[0] + "." + parts[1]));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            JsonNode payload = JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
            // exp / sub / role 缺字段直接判非法（asLong / asText 对 null 会 NPE，靠 catch 兜底太脆弱）
            JsonNode exp = payload.get("exp");
            if (exp == null || !exp.isNumber() || System.currentTimeMillis() / 1000 >= exp.asLong()) return null;
            JsonNode sub = payload.get("sub");
            JsonNode role = payload.get("role");
            if (sub == null || role == null) return null;
            return Map.of("username", sub.asText(), "role", role.asText());
        } catch (Exception e) {
            return null;
        }
    }

    /** 通用 HMAC-SHA256 签名（推送设备凭证等场景复用同一密钥与实现，避免各自造轮子） */
    public String signData(String data) {
        try {
            return b64(hmac(data));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC sign failed", e);
        }
    }

    /** 校验 HMAC 签名（恒定时间比较，防时序攻击）；非法输入返回 false */
    public boolean verifyData(String data, String signature) {
        if (data == null || signature == null || signature.isBlank()) return false;
        try {
            String expected = b64(hmac(data));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hmac(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALG);
        mac.init(new SecretKeySpec(secret, HMAC_ALG));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
