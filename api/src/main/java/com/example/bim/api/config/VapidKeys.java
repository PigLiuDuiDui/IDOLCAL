package com.example.bim.api.config;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * VAPID 密钥（Web Push 签名，RFC 8292）
 * 读取顺序：
 *   1. 环境变量 IDOLCAL_VAPID_PUBLIC_KEY / IDOLCAL_VAPID_PRIVATE_KEY（生产推荐）
 *   2. 本地文件 api/.vapid-keys（scripts/gen-vapid.mjs 生成，仅限开发，勿提交）
 * 密钥格式：公钥 = base64url(SEC1 65 字节)，私钥 = base64url(32 字节标量)
 */
@Component
public class VapidKeys {

    public static final String SUBJECT = System.getenv().getOrDefault("IDOLCAL_VAPID_SUBJECT", "mailto:admin@idolcal.app");

    private final String publicKey;
    private final String privateKey;

    public VapidKeys() throws IOException {
        String pub = System.getenv("IDOLCAL_VAPID_PUBLIC_KEY");
        String priv = System.getenv("IDOLCAL_VAPID_PRIVATE_KEY");
        if (isBlank(pub) || isBlank(priv)) {
            // 开发兜底：读 api/.vapid-keys（两行：公钥 / 私钥）
            Path file = Path.of(".vapid-keys");
            if (Files.exists(file)) {
                var lines = Files.readAllLines(file).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (lines.size() >= 2) {
                    pub = lines.get(0);
                    priv = lines.get(1);
                }
            }
        }
        if (isBlank(pub) || isBlank(priv)) {
            throw new IllegalStateException(
                    "VAPID 密钥未配置：请设置环境变量 IDOLCAL_VAPID_PUBLIC_KEY / IDOLCAL_VAPID_PRIVATE_KEY，"
                            + "或先运行 node scripts/gen-vapid.mjs 生成 api/.vapid-keys");
        }
        this.publicKey = pub;
        this.privateKey = priv;
    }

    public String publicKey() {
        return publicKey;
    }

    public String privateKey() {
        return privateKey;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
