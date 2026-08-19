package com.example.bim.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS：开发环境前端 Vite（5173/5174）直连 API 时跨域。
 * 白名单由环境变量 IDOLCAL_CORS_ORIGINS 控制（逗号分隔）；
 * 生产环境建议同域反向代理（Nginx / 静态托管 + API 子域），此时置空即关闭 CORS。
 * 不再使用 * 通配：公开接口无 Cookie，无需 allowCredentials。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origins;

    public CorsConfig(@Value("${idolcal.cors.origins:}") String origins) {
        this.origins = origins == null || origins.isBlank()
                ? new String[0]
                : java.util.Arrays.stream(origins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origins.length == 0) return; // 同域部署：无需 CORS
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
