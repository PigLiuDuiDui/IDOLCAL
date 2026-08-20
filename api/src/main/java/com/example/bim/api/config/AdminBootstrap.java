package com.example.bim.api.config;

import com.example.bim.api.auth.PasswordHasher;
import com.example.bim.api.entity.User;
import com.example.bim.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 管理后台账号初始化：users 表无管理员时自动创建。
 * 密码来源：IDOLCAL_ADMIN_PASSWORD（生产必须设置）；未设置时生成随机密码并打印日志（仅开发）。
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final String username;
    private final String configuredPassword;

    public AdminBootstrap(UserRepository users,
                          @Value("${idolcal.auth.admin-username:admin}") String username,
                          @Value("${idolcal.auth.admin-password:}") String configuredPassword) {
        this.users = users;
        this.username = username;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername(username).isPresent()) return;
        String password = configuredPassword == null || configuredPassword.isBlank()
                ? randomPassword()
                : configuredPassword;
        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(PasswordHasher.hash(password));
        admin.setRole("ADMIN");
        admin.setCreatedAt(System.currentTimeMillis());
        users.save(admin);
        if (configuredPassword == null || configuredPassword.isBlank()) {
            log.warn("[auth] 已创建默认管理员 {}，随机密码（仅本次启动可见）：{}；生产环境请设置 IDOLCAL_ADMIN_PASSWORD", username, password);
        } else {
            log.info("[auth] 管理员账号已创建：{}（密码来自 IDOLCAL_ADMIN_PASSWORD）", username);
        }
    }

    private String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(16);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
