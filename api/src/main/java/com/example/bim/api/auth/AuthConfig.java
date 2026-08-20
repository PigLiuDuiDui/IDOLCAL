package com.example.bim.api.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理权限拦截器装配（与限流拦截器叠加，认证先于业务执行）。
 */
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public AuthConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
