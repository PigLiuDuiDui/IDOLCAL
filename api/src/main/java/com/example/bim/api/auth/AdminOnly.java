package com.example.bim.api.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理权限标记：标注在 Controller 方法（或类，作用于全部方法）上，
 * 由 AuthInterceptor 校验 Authorization: Bearer JWT 且角色为 ADMIN。
 * 注解驱动替代路径前缀匹配——新增接口时显式声明权限，避免相似前缀路由被误拦 / 漏拦。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
}
